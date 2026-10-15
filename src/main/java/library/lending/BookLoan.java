package library.lending;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class BookLoan {
    private final String id;
    private final String bookId;
    private final String readerId;
    private LoanStatus status;

    public BookLoan(String id, String bookId, String readerId) {
        this(id, bookId, readerId, LoanStatus.CREATED);
    }

    @JsonCreator
    public BookLoan(
            @JsonProperty("id") String id,
            @JsonProperty("bookId") String bookId,
            @JsonProperty("readerId") String readerId,
            @JsonProperty("status") LoanStatus status) {
        this.id = id;
        this.bookId = bookId;
        this.readerId = readerId;
        this.status = status;
    }

    public void cancel() {
        requireStatus(LoanStatus.CREATED);
        status = LoanStatus.CANCELLED;
    }

    public void activate() {
        requireStatus(LoanStatus.CREATED);
        status = LoanStatus.ACTIVE;
    }

    private void requireStatus(LoanStatus expected) {
        if (status != expected) {
            throw new IllegalStateException("Expected status " + expected + ".");
        }
    }

    public String getId() { return id; }
    public String getBookId() { return bookId; }
    public String getReaderId() { return readerId; }
    public LoanStatus getStatus() { return status; }

    @Override
    public String toString() {
        return "BookLoan{id='%s', bookId='%s', readerId='%s', status=%s}"
                .formatted(id, bookId, readerId, status);
    }
}