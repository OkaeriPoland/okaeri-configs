package eu.okaeri.configs.serdes.okaeri.range;

import eu.okaeri.commons.range.LongRange;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.DeserializationData;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.serdes.SerializationData;
import lombok.NonNull;

/**
 * Unified serializer for {@link LongRange} supporting both INLINE and SECTION formats.
 * Format is controlled via {@link RangeSpec} annotation on fields.
 */
public class LongRangeSerializer implements ObjectSerializer<LongRange> {

    @Override
    public boolean supports(@NonNull Class<?> type) {
        return LongRange.class.isAssignableFrom(type);
    }

    @Override
    public void serialize(@NonNull LongRange range,
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
            data.set("min", range.getMin());
            data.set("max", range.getMax());
        }
    }

    @Override
    public LongRange deserialize(@NonNull DeserializationData data,
                                  @NonNull GenericsDeclaration generics) {

        // Try inline format first (string value)
        Object rawValue = data.getValueRaw();
        if (rawValue instanceof String) {
            String strValue = (String) rawValue;
            LongRange range = LongRange.valueOf(strValue);
            if (range == null) {
                throw new IllegalArgumentException("Expected long range (e.g. 1-10, -9223372036854775808-9223372036854775807)");
            }
            return range;
        }

        // Fall back to section format (object with min/max)
        long min = data.get("min", long.class);
        long max = data.get("max", long.class);
        return LongRange.of(min, max);
    }
}
