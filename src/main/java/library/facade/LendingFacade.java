package library.facade;

import library.confirmation.ConfirmationRepository;
import library.confirmation.ConfirmationStatus;
import library.confirmation.ConfirmationService;
import library.lending.BookLoan;
import library.lending.LendingService;
import library.lending.LoanRepository;
import library.lending.LoanStatus;
import library.reservation.BookReservation;
import library.reservation.ReservationRepository;
import library.reservation.ReservationService;
import library.reservation.ReservationStatus;
import library.workflow.LendingWorkflow;
import library.workflow.WorkflowRepository;
import library.workflow.WorkflowStatus;

import java.util.UUID;

public class LendingFacade {
    private final ReservationService reservationService;
    private final ReservationRepository reservationRepository;
    private final LendingService lendingService;
    private final LoanRepository loanRepository;
    private final ConfirmationService confirmationService;
    private final ConfirmationRepository confirmationRepository;
    private final WorkflowRepository workflowRepository;

    public LendingFacade(
            ReservationService reservationService,
            ReservationRepository reservationRepository,
            LendingService lendingService,
            LoanRepository loanRepository,
            ConfirmationService confirmationService,
            ConfirmationRepository confirmationRepository,
            WorkflowRepository workflowRepository) {
        this.reservationService = reservationService;
        this.reservationRepository = reservationRepository;
        this.lendingService = lendingService;
        this.loanRepository = loanRepository;
        this.confirmationService = confirmationService;
        this.confirmationRepository = confirmationRepository;
        this.workflowRepository = workflowRepository;
    }

    public LendingWorkflow lendBook(
            String bookId,
            String readerId,
            boolean libraryCardValid,
            boolean simulateConfirmationFailure) {

        String workflowId = UUID.randomUUID().toString();
        LendingWorkflow workflow =
                new LendingWorkflow(workflowId, bookId, readerId);
        workflowRepository.save(workflow);

        try {
            BookReservation reservation =
                    reservationService.reserveBook(bookId, readerId);
            workflow.reservationCreated(reservation.getId());
            workflowRepository.save(workflow);

            BookLoan loan =
                    lendingService.createLoan(bookId, readerId, libraryCardValid);
            workflow.loanCreated(loan.getId());
            workflowRepository.save(workflow);

            var confirmation =
                    confirmationService.createConfirmation(loan.getId(), 14);
            workflow.confirmationCreated(confirmation.getId());
            workflowRepository.save(workflow);

            if (simulateConfirmationFailure) {
                throw new IllegalStateException(
                        "Simulated failure while confirming the loan.");
            }

            confirmationService.confirm(confirmation.getId());
            lendingService.activateLoan(loan.getId());
            reservationService.completeReservation(reservation.getId());

            workflow.complete();
            workflowRepository.save(workflow);
            return workflow;

        } catch (RuntimeException failure) {
            try {
                compensateFromSavedState(workflowId);
            } catch (RuntimeException compensationFailure) {
                failure.addSuppressed(compensationFailure);
            }
            throw failure;
        }
    }

    public void compensateFromSavedState(String workflowId) {
        LendingWorkflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Workflow not found."));

        if (workflow.getStatus() == WorkflowStatus.COMPENSATED) {
            return;
        }
        if (workflow.getStatus() == WorkflowStatus.COMPLETED) {
            throw new IllegalStateException(
                    "A completed workflow cannot be compensated.");
        }

        if (workflow.getConfirmationId() != null) {
            confirmationRepository.findById(workflow.getConfirmationId())
                    .filter(c -> c.getStatus() == ConfirmationStatus.PENDING)
                    .ifPresent(c -> confirmationService.cancel(c.getId()));
        }

        // If C failed, undo B first and then A.
        if (workflow.getLoanId() != null) {
            loanRepository.findById(workflow.getLoanId())
                    .filter(loan -> loan.getStatus() == LoanStatus.CREATED)
                    .ifPresent(loan -> lendingService.cancelLoan(loan.getId()));
        }

        if (workflow.getReservationId() != null) {
            reservationRepository.findById(workflow.getReservationId())
                    .filter(r -> r.getStatus() == ReservationStatus.RESERVED)
                    .ifPresent(r -> reservationService.cancelReservation(r.getId()));
        }

        workflow.compensate();
        workflowRepository.save(workflow);
    }

    public void recoverIncompleteWorkflows() {
        for (LendingWorkflow workflow : workflowRepository.findAll()) {
            WorkflowStatus status = workflow.getStatus();
            if (status == WorkflowStatus.STARTED
                    || status == WorkflowStatus.PENDING_B
                    || status == WorkflowStatus.PENDING_C) {
                compensateFromSavedState(workflow.getId());
            }
        }
    }
}