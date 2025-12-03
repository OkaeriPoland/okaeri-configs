package eu.okaeri.configs.serdes.okaeri.range;

import eu.okaeri.commons.range.IntRange;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.DeserializationData;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.serdes.SerializationData;
import lombok.NonNull;

/**
 * Unified serializer for {@link IntRange} supporting both INLINE and SECTION formats.
 * Format is controlled via {@link RangeSpec} annotation on fields.
 */
public class IntRangeSerializer implements ObjectSerializer<IntRange> {

    @Override
    public boolean supports(@NonNull Class<?> type) {
        return IntRange.class.isAssignableFrom(type);
    }

    @Override
    public void serialize(@NonNull IntRange range,
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
    public IntRange deserialize(@NonNull DeserializationData data,
                                @NonNull GenericsDeclaration generics) {

        // Try inline format first (string value)
        Object rawValue = data.getValueRaw();
        if (rawValue instanceof String) {
            String strValue = (String) rawValue;
            IntRange range = IntRange.valueOf(strValue);
            if (range == null) {
                throw new IllegalArgumentException("Expected int range (e.g. 1-10, -2147483648-2147483647)");
            }
            return range;
        }

        // Fall back to section format (object with min/max)
        int min = data.get("min", int.class);
        int max = data.get("max", int.class);
        return IntRange.of(min, max);
    }
}
