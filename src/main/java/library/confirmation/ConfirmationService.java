package library.confirmation;

import java.time.LocalDate;
import java.util.UUID;

public class ConfirmationService {
    private final ConfirmationRepository repository;

    public ConfirmationService(ConfirmationRepository repository) {
        this.repository = repository;
    }

    public LoanConfirmation createConfirmation(String loanId, int loanDays) {
        if (loanDays <= 0) {
            throw new IllegalArgumentException("Loan days must be greater than zero.");
        }

        LoanConfirmation confirmation = new LoanConfirmation(
                UUID.randomUUID().toString(),
                loanId,
                LocalDate.now().plusDays(loanDays)
        );
        repository.save(confirmation);
        return confirmation;
    }

    public void confirm(String confirmationId) {
        LoanConfirmation confirmation = repository.findById(confirmationId)
                .orElseThrow(() -> new IllegalArgumentException("Confirmation not found."));

        confirmation.confirm();
        repository.save(confirmation);
    }

    public void cancel(String confirmationId) {
        LoanConfirmation confirmation = repository.findById(confirmationId)
                .orElseThrow(() -> new IllegalArgumentException("Confirmation not found."));

        confirmation.cancel();
        repository.save(confirmation);
    }
}