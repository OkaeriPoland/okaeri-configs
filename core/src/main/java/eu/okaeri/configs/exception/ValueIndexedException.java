package eu.okaeri.configs.exception;

import lombok.Getter;

/**
 * Exception for errors that occur at a specific position within a value.
 * Can be used by any parser/transformer to provide precise error location.
 *
 * Examples:
 * - Regex pattern syntax errors (index of invalid character)
 * - MiniMessage parsing errors (start/end of invalid tag)
 * - JSON parsing within a string value
 */
@Getter
public class ValueIndexedException extends RuntimeException {

    private final int startIndex;
    private final int length;

    /**
     * Create an exception pointing to a single character.
     */
    public ValueIndexedException(String message, int index, Throwable cause) {
        super(message, cause);
        this.startIndex = index;
        this.length = 1;
    }

    /**
     * Create an exception pointing to a range of characters.
     */
    public ValueIndexedException(String message, int startIndex, int length, Throwable cause) {
        super(message, cause);
        this.startIndex = startIndex;
        this.length = Math.max(1, length);
    }

    /**
     * Create an exception pointing to a single character, no cause.
     */
    public ValueIndexedException(String message, int index) {
        this(message, index, null);
    }

    /**
     * Create an exception pointing to a range, no cause.
     */
    public ValueIndexedException(String message, int startIndex, int length) {
        this(message, startIndex, length, null);
    }
}
