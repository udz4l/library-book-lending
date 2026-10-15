package library.reservation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class BookReservation {
    private final String id;
    private final String bookId;
    private final String readerId;
    private ReservationStatus status;

    public BookReservation(String id, String bookId, String readerId) {
        this(id, bookId, readerId, ReservationStatus.RESERVED);
    }

    @JsonCreator
    public BookReservation(
            @JsonProperty("id") String id,
            @JsonProperty("bookId") String bookId,
            @JsonProperty("readerId") String readerId,
            @JsonProperty("status") ReservationStatus status) {
        this.id = id;
        this.bookId = bookId;
        this.readerId = readerId;
        this.status = status;
    }

    public void cancel() {
        requireStatus(ReservationStatus.RESERVED);
        status = ReservationStatus.CANCELLED;
    }

    public void complete() {
        requireStatus(ReservationStatus.RESERVED);
        status = ReservationStatus.COMPLETED;
    }

    private void requireStatus(ReservationStatus expected) {
        if (status != expected) {
            throw new IllegalStateException("Expected status " + expected + ".");
        }
    }

    public String getId() { return id; }
    public String getBookId() { return bookId; }
    public String getReaderId() { return readerId; }
    public ReservationStatus getStatus() { return status; }

    @Override
    public String toString() {
        return "BookReservation{id='%s', bookId='%s', readerId='%s', status=%s}"
                .formatted(id, bookId, readerId, status);
    }
}