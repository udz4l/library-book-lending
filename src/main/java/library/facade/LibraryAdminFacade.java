package library.facade;

import library.confirmation.ConfirmationRepository;
import library.lending.LoanRepository;
import library.reservation.ReservationRepository;
import library.workflow.LendingWorkflow;
import library.workflow.WorkflowRepository;

import java.util.List;

public class LibraryAdminFacade {
    private final WorkflowRepository workflowRepository;
    private final ReservationRepository reservationRepository;
    private final LoanRepository loanRepository;
    private final ConfirmationRepository confirmationRepository;
    private final LendingFacade lendingFacade;

    public LibraryAdminFacade(
            WorkflowRepository workflowRepository,
            ReservationRepository reservationRepository,
            LoanRepository loanRepository,
            ConfirmationRepository confirmationRepository,
            LendingFacade lendingFacade) {
        this.workflowRepository = workflowRepository;
        this.reservationRepository = reservationRepository;
        this.loanRepository = loanRepository;
        this.confirmationRepository = confirmationRepository;
        this.lendingFacade = lendingFacade;
    }

    public List<LendingWorkflow> showWorkflows() {
        return workflowRepository.findAll();
    }

    public String showStoredState() {
        return """
                Reservations:
                %s

                Loans:
                %s

                Confirmations:
                %s

                Workflows:
                %s
                """.formatted(
                reservationRepository.findAll(),
                loanRepository.findAll(),
                confirmationRepository.findAll(),
                workflowRepository.findAll()
        );
    }

    public void compensateWorkflow(String workflowId) {
        lendingFacade.compensateFromSavedState(workflowId);
    }

    public void recoverAfterRestart() {
        lendingFacade.recoverIncompleteWorkflows();
    }
}