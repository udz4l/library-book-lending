package library.client;

import library.facade.LendingFacade;
import library.facade.LibraryAdminFacade;

import java.util.Scanner;

public class LibraryMenu {
    private final LendingFacade lendingFacade;
    private final LibraryAdminFacade adminFacade;
    private final Scanner scanner = new Scanner(System.in);

    public LibraryMenu(
            LendingFacade lendingFacade,
            LibraryAdminFacade adminFacade) {
        this.lendingFacade = lendingFacade;
        this.adminFacade = adminFacade;
    }

    public void start() {
        boolean running = true;

        while (running) {
            System.out.println("""
                    
                    === Library Book Lending ===
                    1. Complete a normal loan
                    2. Demo B failure (invalid library card)
                    3. Demo C failure (confirmation fails)
                    4. Show saved data
                    5. Recover unfinished workflows
                    0. Exit
                    """);

            System.out.print("Choose an option: ");
            String choice = scanner.nextLine().trim();

            try {
                switch (choice) {
                    case "1" -> createLoan(true, false);
                    case "2" -> createLoan(false, false);
                    case "3" -> createLoan(true, true);
                    case "4" -> System.out.println(adminFacade.showStoredState());
                    case "5" -> {
                        adminFacade.recoverAfterRestart();
                        System.out.println("Recovery finished.");
                    }
                    case "0" -> running = false;
                    default -> System.out.println("Please choose a listed option.");
                }
            } catch (RuntimeException e) {
                System.out.println("The request failed: " + e.getMessage());
            }
        }

        System.out.println("Goodbye.");
    }

    private void createLoan(
            boolean cardValid,
            boolean simulateConfirmationFailure) {
        System.out.println("Enter book ID:");
        String bookId = scanner.nextLine().trim();

        System.out.println("Enter reader ID:");
        String readerId = scanner.nextLine().trim();

        var workflow = lendingFacade.lendBook(
                bookId,
                readerId,
                cardValid,
                simulateConfirmationFailure
        );

        System.out.println("Loan completed. Workflow ID: " + workflow.getId());
    }
}