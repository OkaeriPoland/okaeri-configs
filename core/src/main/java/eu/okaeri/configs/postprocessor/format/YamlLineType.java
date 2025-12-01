package eu.okaeri.configs.postprocessor.format;

/**
 * Types of lines in a YAML document.
 */
public enum YamlLineType {
    /**
     * Empty line or whitespace only
     */
    BLANK,
    /**
     * Comment line (starts with #)
     */
    COMMENT,
    /**
     * Key-value pair: "key: value"
     */
    KEY_VALUE,
    /**
     * Key only (value on next line or nested): "key:"
     */
    KEY_ONLY,
    /**
     * List item with value: "- value"
     */
    LIST_ITEM,
    /**
     * List item with key-value: "- key: value"
     */
    LIST_ITEM_KEY_VALUE,
    /**
     * List item key only: "- key:"
     */
    LIST_ITEM_KEY_ONLY,
    /**
     * Multiline continuation
     */
    MULTILINE_CONTENT
}
