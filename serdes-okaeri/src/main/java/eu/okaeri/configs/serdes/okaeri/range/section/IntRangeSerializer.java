package eu.okaeri.configs.serdes.okaeri.range.section;

import eu.okaeri.commons.range.IntRange;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.DeserializationData;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.serdes.SerializationData;
import lombok.NonNull;

public class IntRangeSerializer implements ObjectSerializer<IntRange> {

    @Override
    public boolean supports(@NonNull Class<?> type) {
        return IntRange.class.isAssignableFrom(type);
    }

    @Override
    public void serialize(@NonNull IntRange range, @NonNull SerializationData data, @NonNull GenericsDeclaration generics) {
        data.add("min", range.getMin());
        data.add("max", range.getMax());
    }

    @Override
    public IntRange deserialize(@NonNull DeserializationData data, @NonNull GenericsDeclaration generics) {

        int min = data.get("min", int.class);
        int max = data.get("max", int.class);

        return IntRange.of(min, max);
    }
}
