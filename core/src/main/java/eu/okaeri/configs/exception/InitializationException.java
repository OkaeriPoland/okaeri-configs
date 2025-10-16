package eu.okaeri.configs.exception;

/**
 * Exception thrown during configuration initialization failures.
 * 
 * @deprecated This exception has been replaced with {@link IllegalStateException}.
 *             Catch {@code IllegalStateException} directly instead.
 *             This class will be removed in a future version.
 */
@Deprecated
public class InitializationException extends OkaeriException {

    public InitializationException(String message) {
        super(message);
    }

    public InitializationException(String message, Throwable cause) {
        super(message, cause);
    }

    public InitializationException(Throwable cause) {
        super(cause);
    }
}
