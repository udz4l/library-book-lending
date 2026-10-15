package library.lending;

import java.util.UUID;
import library.exception.InvalidLibraryCardException;

public class LendingService {
    private final LoanRepository repository;

    public LendingService(LoanRepository repository) {
        this.repository = repository;
    }

    public BookLoan createLoan(String bookId, String readerId, boolean libraryCardValid) {
        if (!libraryCardValid) {
            throw new InvalidLibraryCardException("The library card is not valid.");
        }

        BookLoan loan = new BookLoan(UUID.randomUUID().toString(), bookId, readerId);
        repository.save(loan);
        return loan;
    }

    public void cancelLoan(String loanId) {
        BookLoan loan = repository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found."));

        loan.cancel();
        repository.save(loan);
    }

    public void activateLoan(String loanId) {
        BookLoan loan = repository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found."));

        loan.activate();
        repository.save(loan);
    }
}