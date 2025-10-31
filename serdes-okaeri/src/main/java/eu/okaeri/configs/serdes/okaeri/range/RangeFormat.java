package eu.okaeri.configs.serdes.okaeri.range;

/**
 * Serialization format for Range types.
 */
public enum RangeFormat {

    /**
     * Inline format: serializes as compact string (e.g., "10-20").
     * Best for simple configs and space efficiency.
     */
    INLINE,

    /**
     * Section format: serializes as object with min/max fields.
     * Example: { min: 10, max: 20 }
     * Best for readability and explicitness.
     */
    SECTION
}
