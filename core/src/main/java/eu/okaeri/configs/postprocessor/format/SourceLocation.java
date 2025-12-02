package eu.okaeri.configs.postprocessor.format;

import lombok.Builder;
import lombok.Data;

/**
 * Represents a location in source content for error reporting.
 * Format-agnostic representation used by SourceErrorMarker.
 */
@Data
@Builder
public class SourceLocation {

    /**
     * Line number (1-based)
     */
    private final int lineNumber;

    /**
     * The original raw line content
     */
    private final String rawLine;

    /**
     * Column where the value starts (0-based), -1 if no value
     */
    @Builder.Default
    private final int valueColumn = -1;

    /**
     * The raw value string (may include quotes)
     */
    private final String value;

    /**
     * Column where the key starts (0-based), used as fallback
     */
    @Builder.Default
    private final int keyColumn = 0;

    /**
     * The key name, used as fallback when no value
     */
    private final String key;
}
