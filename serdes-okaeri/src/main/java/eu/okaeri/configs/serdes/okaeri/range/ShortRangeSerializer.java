package eu.okaeri.configs.serdes.okaeri.range;

import eu.okaeri.commons.range.ShortRange;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.DeserializationData;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.serdes.SerializationData;
import lombok.NonNull;

/**
 * Unified serializer for {@link ShortRange} supporting both INLINE and SECTION formats.
 * Format is controlled via {@link RangeSpec} annotation on fields.
 */
public class ShortRangeSerializer implements ObjectSerializer<ShortRange> {

    @Override
    public boolean supports(@NonNull Class<?> type) {
        return ShortRange.class.isAssignableFrom(type);
    }

    @Override
    public void serialize(@NonNull ShortRange range,
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
    public ShortRange deserialize(@NonNull DeserializationData data,
                                   @NonNull GenericsDeclaration generics) {

        // Try inline format first (string value)
        Object rawValue = data.getValueRaw();
        if (rawValue instanceof String) {
            String strValue = (String) rawValue;
            ShortRange range = ShortRange.valueOf(strValue);
            if (range == null) {
                throw new IllegalArgumentException("Invalid ShortRange format: " + strValue);
            }
            return range;
        }

        // Fall back to section format (object with min/max)
        short min = data.get("min", short.class);
        short max = data.get("max", short.class);
        return ShortRange.of(min, max);
    }
}
