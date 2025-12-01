package eu.okaeri.configs.postprocessor.format;

import lombok.Builder;
import lombok.Data;

/**
 * Represents a parsed YAML line with position and path information.
 */
@Data
@Builder
public class YamlLine {

    /**
     * Line number (1-based)
     */
    private final int lineNumber;

    /**
     * Column where content starts (0-based, after indentation)
     */
    private final int column;

    /**
     * Indentation level (number of spaces)
     */
    private final int indent;

    /**
     * Type of this line
     */
    private final YamlLineType type;

    /**
     * The key name (for KEY_VALUE, KEY_ONLY, LIST_ITEM_KEY_VALUE, LIST_ITEM_KEY_ONLY)
     */
    private final String key;

    /**
     * The raw value string (for KEY_VALUE, LIST_ITEM, LIST_ITEM_KEY_VALUE)
     */
    private final String value;

    /**
     * Column where the value starts (for error markers)
     */
    private final int valueColumn;

    /**
     * The original raw line content
     */
    private final String rawLine;

    /**
     * The full path to this element (e.g., "database.connections[0].host")
     */
    private String path;

    /**
     * List index if this line is inside a list (-1 if not)
     */
    @Builder.Default
    private int listIndex = -1;
}
