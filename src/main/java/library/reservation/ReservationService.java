package library.reservation;

import java.util.UUID;
import library.exception.BookUnavailableException;

public class ReservationService {
    private final ReservationRepository repository;

    public ReservationService(ReservationRepository repository) {
        this.repository = repository;
    }

    public BookReservation reserveBook(String bookId, String readerId) {
        if (bookId == null || bookId.isBlank()) {
            throw new IllegalArgumentException("Book ID is required.");
        }
        if (readerId == null || readerId.isBlank()) {
            throw new IllegalArgumentException("Reader ID is required.");
        }

        boolean alreadyReserved = repository.findAll().stream()
                .anyMatch(reservation ->
                        reservation.getBookId().equals(bookId)
                                && reservation.getStatus() != ReservationStatus.CANCELLED);

        if (alreadyReserved) {
            throw new BookUnavailableException("This book is already reserved.");
        }

        BookReservation reservation = new BookReservation(
                UUID.randomUUID().toString(), bookId, readerId);
        repository.save(reservation);
        return reservation;
    }

    public void cancelReservation(String reservationId) {
        BookReservation reservation = repository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found."));

        reservation.cancel();
        repository.save(reservation);
    }

    public void completeReservation(String reservationId) {
        BookReservation reservation = repository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found."));

        reservation.complete();
        repository.save(reservation);
    }
}