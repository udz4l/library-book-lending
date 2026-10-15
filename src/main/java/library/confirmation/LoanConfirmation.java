package library.confirmation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public class LoanConfirmation {
    private final String id;
    private final String loanId;
    private final LocalDate dueDate;
    private ConfirmationStatus status;

    public LoanConfirmation(String id, String loanId, LocalDate dueDate) {
        this(id, loanId, dueDate, ConfirmationStatus.PENDING);
    }

    @JsonCreator
    public LoanConfirmation(
            @JsonProperty("id") String id,
            @JsonProperty("loanId") String loanId,
            @JsonProperty("dueDate") LocalDate dueDate,
            @JsonProperty("status") ConfirmationStatus status) {
        this.id = id;
        this.loanId = loanId;
        this.dueDate = dueDate;
        this.status = status;
    }

    public void confirm() {
        requireStatus(ConfirmationStatus.PENDING);
        status = ConfirmationStatus.CONFIRMED;
    }

    public void cancel() {
        requireStatus(ConfirmationStatus.PENDING);
        status = ConfirmationStatus.CANCELLED;
    }

    private void requireStatus(ConfirmationStatus expected) {
        if (status != expected) {
            throw new IllegalStateException("Expected status " + expected + ".");
        }
    }

    public String getId() { return id; }
    public String getLoanId() { return loanId; }
    public LocalDate getDueDate() { return dueDate; }
    public ConfirmationStatus getStatus() { return status; }

    @Override
    public String toString() {
        return "LoanConfirmation{id='%s', loanId='%s', dueDate=%s, status=%s}"
                .formatted(id, loanId, dueDate, status);
    }
}