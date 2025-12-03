package eu.okaeri.configs.format;

import eu.okaeri.configs.serdes.ConfigPath;
import lombok.Builder;
import lombok.Data;

/**
 * Represents a location in source content for error reporting and comment insertion.
 * Format-agnostic representation used by SourceErrorMarker and source walkers.
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

    /**
     * The full config path to this location (e.g., database.connections[0].host)
     */
    private final ConfigPath configPath;

    /**
     * Indentation level (number of spaces), used for comment insertion
     */
    @Builder.Default
    private final int indent = 0;

    // ==================== Multiline Content Support ====================

    /**
     * For multiline block scalars (|, >, |--, etc.), the line number where
     * actual content starts (1-based). -1 if not multiline.
     */
    @Builder.Default
    private final int contentLineNumber = -1;

    /**
     * For multiline block scalars, the column where content starts (0-based).
     */
    @Builder.Default
    private final int contentColumn = -1;

    /**
     * For multiline block scalars, the raw line containing the first line of content.
     */
    private final String contentRawLine;

    /**
     * For multiline block scalars, the last line number of the content (1-based).
     * Used to calculate how many lines to show or skip with ellipsis.
     */
    @Builder.Default
    private final int contentEndLineNumber = -1;

    /**
     * Returns true if this location represents a multiline block scalar.
     */
    public boolean isMultiline() {
        return this.contentLineNumber > 0;
    }
}
