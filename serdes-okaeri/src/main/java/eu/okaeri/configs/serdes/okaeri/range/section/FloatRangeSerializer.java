package eu.okaeri.configs.serdes.okaeri.range.section;

import eu.okaeri.commons.range.FloatRange;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.DeserializationData;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.serdes.SerializationData;
import lombok.NonNull;

public class FloatRangeSerializer implements ObjectSerializer<FloatRange> {

    @Override
    public boolean supports(@NonNull Class<?> type) {
        return FloatRange.class.isAssignableFrom(type);
    }

    @Override
    public void serialize(@NonNull FloatRange range, @NonNull SerializationData data, @NonNull GenericsDeclaration generics) {
        data.add("min", range.getMin());
        data.add("max", range.getMax());
    }

    @Override
    public FloatRange deserialize(@NonNull DeserializationData data, @NonNull GenericsDeclaration generics) {

        float min = data.get("min", float.class);
        float max = data.get("max", float.class);

        return FloatRange.of(min, max);
    }
}
