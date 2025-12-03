package eu.okaeri.configs.serdes.okaeri.range;

import eu.okaeri.commons.range.DoubleRange;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.DeserializationData;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.serdes.SerializationData;
import lombok.NonNull;

/**
 * Unified serializer for {@link DoubleRange} supporting both INLINE and SECTION formats.
 * Format is controlled via {@link RangeSpec} annotation on fields.
 */
public class DoubleRangeSerializer implements ObjectSerializer<DoubleRange> {

    @Override
    public boolean supports(@NonNull Class<?> type) {
        return DoubleRange.class.isAssignableFrom(type);
    }

    @Override
    public void serialize(@NonNull DoubleRange range,
                          @NonNull SerializationData data,
                          @NonNull GenericsDeclaration generics) {

        // Retrieve format preference from context (defaults to SECTION)
        RangeFormat format = data.getContext()
                .getAttachment(RangeSpecData.class)
                .map(RangeSpecData::getFormat)
                .orElse(RangeFormat.SECTION);

        // Serialize based on format
        if (format == RangeFormat.INLINE) {
            // Inline format: "min-max"
            data.setValue(range.getMin() + "-" + range.getMax());
        } else {
            // Section format: { min: X, max: Y }
            data.add("min", range.getMin());
            data.add("max", range.getMax());
        }
    }

    @Override
    public DoubleRange deserialize(@NonNull DeserializationData data,
                                    @NonNull GenericsDeclaration generics) {

        // Try inline format first (string value)
        Object rawValue = data.getValueRaw();
        if (rawValue instanceof String) {
            String strValue = (String) rawValue;
            DoubleRange range = DoubleRange.valueOf(strValue);
            if (range == null) {
                throw new IllegalArgumentException("Expected double range (e.g. 0.5-1.5, -100.0-100.0)");
            }
            return range;
        }

        // Fall back to section format (object with min/max)
        double min = data.get("min", double.class);
        double max = data.get("max", double.class);
        return DoubleRange.of(min, max);
    }
}
