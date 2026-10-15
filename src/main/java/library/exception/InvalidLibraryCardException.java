package library.exception;

public class InvalidLibraryCardException extends IllegalArgumentException {
    public InvalidLibraryCardException(String message) {
        super(message);
    }
}