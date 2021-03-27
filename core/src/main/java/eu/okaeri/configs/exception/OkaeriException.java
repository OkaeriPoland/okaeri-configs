package eu.okaeri.configs.exception;

public class OkaeriException extends RuntimeException {

    public OkaeriException(String message) {
        super(message);
    }

    public OkaeriException(String message, Throwable cause) {
        super(message, cause);
    }

    public OkaeriException(Throwable cause) {
        super(cause);
    }
}
