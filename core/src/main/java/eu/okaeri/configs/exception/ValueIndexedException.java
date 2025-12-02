package eu.okaeri.configs.exception;

import lombok.Builder;
import lombok.Getter;

/**
 * Exception for errors that occur at a specific position within a value.
 * Can be used by any parser/transformer to provide precise error location.
 *
 * <p>Examples:
 * <ul>
 *   <li>Regex pattern syntax errors (index of invalid character)</li>
 *   <li>MiniMessage parsing errors (start/end of invalid tag)</li>
 *   <li>JSON parsing within a string value</li>
 * </ul>
 *
 * <p>Context lines control how many surrounding lines are shown in error output:
 * <pre>
 *  --> config.yml:5:10
 *    |
 *  3 |     other: value
 *  4 |     more: stuff
 *  5 |     port: abc
 *    |           ^^^ expected integer
 *  6 |     next: line
 * </pre>
 */
@Getter
public class ValueIndexedException extends RuntimeException {

    /**
     * Default number of context lines shown before the error line.
     */
    public static final int DEFAULT_CONTEXT_BEFORE = 0;

    /**
     * Default number of context lines shown after the error line.
     */
    public static final int DEFAULT_CONTEXT_AFTER = 0;

    private final int startIndex;
    private final int length;
    private final int contextLinesBefore;
    private final int contextLinesAfter;

    @Builder
    private ValueIndexedException(String message, int startIndex, int length, int contextLinesBefore, int contextLinesAfter, Throwable cause) {
        super(message, cause);
        this.startIndex = startIndex;
        this.length = Math.max(1, (length == 0) ? 1 : length);
        this.contextLinesBefore = (contextLinesBefore >= 0) ? contextLinesBefore : DEFAULT_CONTEXT_BEFORE;
        this.contextLinesAfter = (contextLinesAfter >= 0) ? contextLinesAfter : DEFAULT_CONTEXT_AFTER;
    }

    /**
     * Create an exception pointing to a single character.
     */
    public ValueIndexedException(String message, int index, Throwable cause) {
        this(message, index, 1, DEFAULT_CONTEXT_BEFORE, DEFAULT_CONTEXT_AFTER, cause);
    }

    /**
     * Create an exception pointing to a range of characters.
     */
    public ValueIndexedException(String message, int startIndex, int length, Throwable cause) {
        this(message, startIndex, length, DEFAULT_CONTEXT_BEFORE, DEFAULT_CONTEXT_AFTER, cause);
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
