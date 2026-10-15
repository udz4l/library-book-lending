package library.exception;

public class BookUnavailableException extends IllegalStateException {
    public BookUnavailableException(String message) {
        super(message);
    }
}