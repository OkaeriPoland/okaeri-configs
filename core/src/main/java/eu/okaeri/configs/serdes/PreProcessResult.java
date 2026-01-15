package eu.okaeri.configs.serdes;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Result of a {@link ValuePreProcessor#process} operation.
 * <p>
 * Indicates whether and how a value was modified:
 * <ul>
 *   <li>{@link #NOOP} - no change, use original value</li>
 *   <li>{@link #runtimeOnly(Object)} - changed, but preserve original for saving</li>
 *   <li>{@link #transformed(Object)} - changed, write new value to file</li>
 * </ul>
 *
 * @see ValuePreProcessor
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class PreProcessResult {

    private static final PreProcessResult NOOP = new PreProcessResult(null, false, false);

    private final Object value;
    private final boolean modified;
    private final boolean writeToFile;

    /**
     * No processing was done - original value will be used unchanged.
     *
     * @return result indicating no modification
     */
    public static PreProcessResult noop() {
        return NOOP;
    }

    /**
     * Value was transformed, preserve original for saving.
     * <p>
     * Use this for runtime-only transformations like environment variable
     * placeholder resolution, where you want the original {@code ${VAR}}
     * syntax to remain in the saved file.
     *
     * @param value the transformed value
     * @return result indicating runtime-only modification
     */
    public static PreProcessResult runtimeOnly(Object value) {
        return new PreProcessResult(value, true, false);
    }

    /**
     * Value was transformed, save the new value to file.
     * <p>
     * Use this when the transformation should be persisted, replacing
     * the original value in the configuration file on save.
     *
     * @param value the transformed value
     * @return result indicating persistent transformation
     */
    public static PreProcessResult transformed(Object value) {
        return new PreProcessResult(value, true, true);
    }
}
