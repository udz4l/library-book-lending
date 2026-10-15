package library.facade;

import library.confirmation.*;
import library.lending.*;
import library.reservation.*;
import library.storage.JsonFileStore;
import library.workflow.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class SubsystemCoverageTest {
    @TempDir
    Path temporaryDirectory;

    private ReservationRepository reservationRepository;
    private LoanRepository loanRepository;
    private ConfirmationRepository confirmationRepository;
    private WorkflowRepository workflowRepository;

    private ReservationService reservationService;
    private LendingService lendingService;
    private ConfirmationService confirmationService;
    private LendingFacade lendingFacade;
    private LibraryAdminFacade adminFacade;

    @BeforeEach
    void setUp() {
        JsonFileStore store =
                new JsonFileStore(temporaryDirectory.resolve("library.json"));

        reservationRepository = new ReservationRepository(store);
        loanRepository = new LoanRepository(store);
        confirmationRepository = new ConfirmationRepository(store);
        workflowRepository = new WorkflowRepository(store);

        reservationService = new ReservationService(reservationRepository);
        lendingService = new LendingService(loanRepository);
        confirmationService = new ConfirmationService(confirmationRepository);

        lendingFacade = new LendingFacade(
                reservationService,
                reservationRepository,
                lendingService,
                loanRepository,
                confirmationService,
                confirmationRepository,
                workflowRepository
        );

        adminFacade = new LibraryAdminFacade(
                workflowRepository,
                reservationRepository,
                loanRepository,
                confirmationRepository,
                lendingFacade
        );
    }

    @Test
    void reservationServiceChecksInputsDuplicatesAndMissingRecords() {
        assertThrows(IllegalArgumentException.class,
                () -> reservationService.reserveBook("", "reader-1"));
        assertThrows(IllegalArgumentException.class,
                () -> reservationService.reserveBook("book-1", " "));

        BookReservation first =
                reservationService.reserveBook("book-1", "reader-1");

        assertThrows(IllegalStateException.class,
                () -> reservationService.reserveBook("book-1", "reader-2"));
        assertThrows(IllegalArgumentException.class,
                () -> reservationService.cancelReservation("missing"));
        assertThrows(IllegalArgumentException.class,
                () -> reservationService.completeReservation("missing"));

        reservationService.cancelReservation(first.getId());

        BookReservation second =
                reservationService.reserveBook("book-1", "reader-2");
        reservationService.completeReservation(second.getId());

        assertEquals(
                ReservationStatus.COMPLETED,
                reservationRepository.findById(second.getId()).orElseThrow().getStatus()
        );
        assertThrows(IllegalStateException.class,
                () -> reservationService.cancelReservation(second.getId()));
    }

    @Test
    void lendingServiceChecksCardsAndLoanTransitions() {
        assertThrows(IllegalArgumentException.class,
                () -> lendingService.createLoan("book-1", "reader-1", false));
        assertThrows(IllegalArgumentException.class,
                () -> lendingService.cancelLoan("missing"));
        assertThrows(IllegalArgumentException.class,
                () -> lendingService.activateLoan("missing"));

        BookLoan cancelled =
                lendingService.createLoan("book-1", "reader-1", true);
        lendingService.cancelLoan(cancelled.getId());

        assertEquals(
                LoanStatus.CANCELLED,
                loanRepository.findById(cancelled.getId()).orElseThrow().getStatus()
        );
        assertThrows(IllegalStateException.class,
                () -> lendingService.cancelLoan(cancelled.getId()));
        assertThrows(IllegalStateException.class,
                () -> lendingService.activateLoan(cancelled.getId()));

        BookLoan active =
                lendingService.createLoan("book-2", "reader-2", true);
        lendingService.activateLoan(active.getId());

        assertEquals(
                LoanStatus.ACTIVE,
                loanRepository.findById(active.getId()).orElseThrow().getStatus()
        );
        assertThrows(IllegalStateException.class,
                () -> lendingService.cancelLoan(active.getId()));
    }

    @Test
    void confirmationServiceChecksDatesRecordsAndTransitions() {
        assertThrows(IllegalArgumentException.class,
                () -> confirmationService.createConfirmation("loan-1", 0));
        assertThrows(IllegalArgumentException.class,
                () -> confirmationService.confirm("missing"));
        assertThrows(IllegalArgumentException.class,
                () -> confirmationService.cancel("missing"));

        LoanConfirmation confirmed =
                confirmationService.createConfirmation("loan-1", 14);
        confirmationService.confirm(confirmed.getId());

        assertEquals(
                ConfirmationStatus.CONFIRMED,
                confirmationRepository.findById(confirmed.getId()).orElseThrow().getStatus()
        );
        assertEquals(LocalDate.now().plusDays(14), confirmed.getDueDate());
        assertThrows(IllegalStateException.class,
                () -> confirmationService.confirm(confirmed.getId()));
        assertThrows(IllegalStateException.class,
                () -> confirmationService.cancel(confirmed.getId()));

        LoanConfirmation cancelled =
                confirmationService.createConfirmation("loan-2", 7);
        confirmationService.cancel(cancelled.getId());

        assertEquals(
                ConfirmationStatus.CANCELLED,
                confirmationRepository.findById(cancelled.getId()).orElseThrow().getStatus()
        );
        assertThrows(IllegalStateException.class,
                () -> confirmationService.cancel(cancelled.getId()));
        assertThrows(IllegalStateException.class,
                () -> confirmationService.confirm(cancelled.getId()));
    }

    @Test
    void workflowRejectsOutOfOrderAndRepeatedChanges() {
        LendingWorkflow workflow =
                new LendingWorkflow("workflow-1", "book-1", "reader-1");

        assertThrows(IllegalStateException.class,
                () -> workflow.loanCreated("loan-1"));

        workflow.reservationCreated("reservation-1");

        assertEquals(WorkflowStatus.PENDING_B, workflow.getStatus());
        assertThrows(IllegalStateException.class,
                () -> workflow.reservationCreated("reservation-2"));

        workflow.loanCreated("loan-1");
        workflow.confirmationCreated("confirmation-1");
        workflow.complete();

        assertEquals(WorkflowStatus.COMPLETED, workflow.getStatus());
        assertThrows(IllegalStateException.class, workflow::compensate);

        LendingWorkflow unfinished =
                new LendingWorkflow("workflow-2", "book-2", "reader-2");
        unfinished.compensate();

        assertEquals(WorkflowStatus.COMPENSATED, unfinished.getStatus());
        assertThrows(IllegalStateException.class, unfinished::compensate);
    }

    @Test
    void adminFacadeShowsStateAndCanCompensateSavedWorkflows() {
        LendingWorkflow workflow =
                new LendingWorkflow("workflow-admin", "book-admin", "reader-admin");
        workflowRepository.save(workflow);

        BookReservation reservation =
                reservationService.reserveBook("book-admin", "reader-admin");
        workflow.reservationCreated(reservation.getId());
        workflowRepository.save(workflow);

        assertEquals(1, adminFacade.showWorkflows().size());
        assertTrue(adminFacade.showStoredState().contains("workflow-admin"));

        adminFacade.compensateWorkflow(workflow.getId());

        LendingWorkflow saved =
                workflowRepository.findById(workflow.getId()).orElseThrow();
        BookReservation savedReservation =
                reservationRepository.findById(reservation.getId()).orElseThrow();

        assertEquals(WorkflowStatus.COMPENSATED, saved.getStatus());
        assertEquals(ReservationStatus.CANCELLED, savedReservation.getStatus());

        // Also covers recovery when no unfinished workflow remains.
        assertDoesNotThrow(adminFacade::recoverAfterRestart);
    }

    @Test
    void facadeCompensatesAWorkflowWhenReservationInputIsInvalid() {
        assertThrows(IllegalArgumentException.class,
                () -> lendingFacade.lendBook("", "reader-1", true, false));

        LendingWorkflow saved = workflowRepository.findAll().get(0);
        assertEquals(WorkflowStatus.COMPENSATED, saved.getStatus());
        assertNull(saved.getReservationId());
    }

    @Test
    void jsonFileStoreCanDeleteARecord() {
        JsonFileStore store =
                new JsonFileStore(temporaryDirectory.resolve("direct-store.json"));

        store.save("records", "record-1", java.util.Map.of("name", "sample"));
        assertNotNull(store.find("records", "record-1"));
        assertEquals(1, store.findAll("records").size());

        store.delete("records", "record-1");

        assertNull(store.find("records", "record-1"));
        assertTrue(store.findAll("records").isEmpty());
    }
}