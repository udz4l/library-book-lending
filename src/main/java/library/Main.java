package library;

import library.client.LibraryMenu;
import library.confirmation.ConfirmationRepository;
import library.confirmation.ConfirmationService;
import library.facade.LendingFacade;
import library.facade.LibraryAdminFacade;
import library.lending.LendingService;
import library.lending.LoanRepository;
import library.reservation.ReservationRepository;
import library.reservation.ReservationService;
import library.storage.JsonFileStore;
import library.workflow.WorkflowRepository;

public class Main {
    public static void main(String[] args) {
        JsonFileStore store = new JsonFileStore();

        ReservationRepository reservationRepository =
                new ReservationRepository(store);
        LoanRepository loanRepository =
                new LoanRepository(store);
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

        LibraryAdminFacade adminFacade = new LibraryAdminFacade(
                workflowRepository,
                reservationRepository,
                loanRepository,
                confirmationRepository,
                lendingFacade
        );

        adminFacade.recoverAfterRestart();

        LibraryMenu menu = new LibraryMenu(lendingFacade, adminFacade);
        menu.start();
    }
}