package eu.okaeri.configs.serdes.okaeri.range;

import eu.okaeri.commons.range.FloatRange;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.DeserializationData;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.serdes.SerializationData;
import lombok.NonNull;

/**
 * Unified serializer for {@link FloatRange} supporting both INLINE and SECTION formats.
 * Format is controlled via {@link RangeSpec} annotation on fields.
 */
public class FloatRangeSerializer implements ObjectSerializer<FloatRange> {

    @Override
    public boolean supports(@NonNull Class<?> type) {
        return FloatRange.class.isAssignableFrom(type);
    }

    @Override
    public void serialize(@NonNull FloatRange range,
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
    public FloatRange deserialize(@NonNull DeserializationData data,
                                   @NonNull GenericsDeclaration generics) {

        // Try inline format first (string value)
        Object rawValue = data.getValueRaw();
        if (rawValue instanceof String) {
            String strValue = (String) rawValue;
            FloatRange range = FloatRange.valueOf(strValue);
            if (range == null) {
                throw new IllegalArgumentException("Expected float range (e.g. 0.5-1.5, -100.0-100.0)");
            }
            return range;
        }

        // Fall back to section format (object with min/max)
        float min = data.get("min", float.class);
        float max = data.get("max", float.class);
        return FloatRange.of(min, max);
    }
}
