package library.facade;

import library.confirmation.ConfirmationRepository;
import library.confirmation.ConfirmationService;
import library.confirmation.ConfirmationStatus;
import library.lending.BookLoan;
import library.lending.LendingService;
import library.lending.LoanRepository;
import library.lending.LoanStatus;
import library.reservation.BookReservation;
import library.reservation.ReservationRepository;
import library.reservation.ReservationService;
import library.reservation.ReservationStatus;
import library.storage.JsonFileStore;
import library.workflow.LendingWorkflow;
import library.workflow.WorkflowRepository;
import library.workflow.WorkflowStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LendingFacadeTest {
    @TempDir
    Path temporaryDirectory;

    private TestApp app;
    private Path dataFile;

    @BeforeEach
    void setUp() {
        dataFile = temporaryDirectory.resolve("library.json");
        app = createApp(dataFile);
    }

    @Test
    void completesTheFullLendingWorkflow() {
        LendingWorkflow workflow =
                app.lendingFacade.lendBook("book-1", "reader-1", true, false);

        assertEquals(WorkflowStatus.COMPLETED, workflow.getStatus());
        assertEquals(
                ReservationStatus.COMPLETED,
                app.reservationRepository.findById(workflow.getReservationId())
                        .orElseThrow().getStatus()
        );
        assertEquals(
                LoanStatus.ACTIVE,
                app.loanRepository.findById(workflow.getLoanId())
                        .orElseThrow().getStatus()
        );
        assertEquals(
                ConfirmationStatus.CONFIRMED,
                app.confirmationRepository.findById(workflow.getConfirmationId())
                        .orElseThrow().getStatus()
        );
    }

    @Test
    void bFailureCompensatesOnlyTheReservation() {
        a_reader_with_an_invalid_card_requests_a_loan();

        LendingWorkflow workflow = app.workflowRepository.findAll().get(0);
        BookReservation reservation =
                app.reservationRepository.findById(workflow.getReservationId())
                        .orElseThrow();

        assertEquals(WorkflowStatus.COMPENSATED, workflow.getStatus());
        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
        assertNull(workflow.getLoanId());
        assertTrue(app.loanRepository.findAll().isEmpty());
    }

    @Test
    void cFailureCompensatesTheLoanAndThenTheReservation() {
        assertThrows(
                IllegalStateException.class,
                () -> app.lendingFacade.lendBook(
                        "book-c", "reader-c", true, true)
        );

        LendingWorkflow workflow = app.workflowRepository.findAll().get(0);

        assertEquals(WorkflowStatus.COMPENSATED, workflow.getStatus());
        assertEquals(
                LoanStatus.CANCELLED,
                app.loanRepository.findById(workflow.getLoanId())
                        .orElseThrow().getStatus()
        );
        assertEquals(
                ReservationStatus.CANCELLED,
                app.reservationRepository.findById(workflow.getReservationId())
                        .orElseThrow().getStatus()
        );
        assertEquals(
                ConfirmationStatus.CANCELLED,
                app.confirmationRepository.findById(workflow.getConfirmationId())
                        .orElseThrow().getStatus()
        );
    }

    @Test
    void reloadsSavedMidWorkflowStateAndRecoversAfterRestart() {
        LendingWorkflow workflow =
                new LendingWorkflow("workflow-restart", "book-r", "reader-r");
        app.workflowRepository.save(workflow);

        BookReservation reservation =
                app.reservationService.reserveBook("book-r", "reader-r");
        workflow.reservationCreated(reservation.getId());
        app.workflowRepository.save(workflow);

        BookLoan loan =
                app.lendingService.createLoan("book-r", "reader-r", true);
        workflow.loanCreated(loan.getId());
        app.workflowRepository.save(workflow);

        assertEquals(WorkflowStatus.PENDING_C, workflow.getStatus());

        // Create new objects using the same file to simulate an application restart.
        TestApp restartedApp = createApp(dataFile);
        restartedApp.lendingFacade.recoverIncompleteWorkflows();

        LendingWorkflow reloadedWorkflow =
                restartedApp.workflowRepository.findById("workflow-restart")
                        .orElseThrow();

        assertEquals(WorkflowStatus.COMPENSATED, reloadedWorkflow.getStatus());
        assertEquals(
                LoanStatus.CANCELLED,
                restartedApp.loanRepository.findById(loan.getId())
                        .orElseThrow().getStatus()
        );
        assertEquals(
                ReservationStatus.CANCELLED,
                restartedApp.reservationRepository.findById(reservation.getId())
                        .orElseThrow().getStatus()
        );
    }

    private void a_reader_with_an_invalid_card_requests_a_loan() {
        assertThrows(
                IllegalArgumentException.class,
                () -> app.lendingFacade.lendBook(
                        "book-b", "reader-b", false, false)
        );
    }

    private static TestApp createApp(Path dataFile) {
        JsonFileStore store = new JsonFileStore(dataFile);

        ReservationRepository reservationRepository =
                new ReservationRepository(store);
        LoanRepository loanRepository = new LoanRepository(store);
        ConfirmationRepository confirmationRepository =
                new ConfirmationRepository(store);
        WorkflowRepository workflowRepository =
                new WorkflowRepository(store);

        ReservationService reservationService =
                new ReservationService(reservationRepository);
        LendingService lendingService =
                new LendingService(loanRepository);
        ConfirmationService confirmationService =
                new ConfirmationService(confirmationRepository);

        LendingFacade lendingFacade = new LendingFacade(
                reservationService,
                reservationRepository,
                lendingService,
                loanRepository,
                confirmationService,
                confirmationRepository,
                workflowRepository
        );

        return new TestApp(
                lendingFacade,
                reservationService,
                lendingService,
                reservationRepository,
                loanRepository,
                confirmationRepository,
                workflowRepository
        );
    }

    private record TestApp(
            LendingFacade lendingFacade,
            ReservationService reservationService,
            LendingService lendingService,
            ReservationRepository reservationRepository,
            LoanRepository loanRepository,
            ConfirmationRepository confirmationRepository,
            WorkflowRepository workflowRepository) {
    }
}