package eu.okaeri.configs.serdes.okaeri.range.section;

import eu.okaeri.commons.range.LongRange;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.DeserializationData;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.serdes.SerializationData;
import lombok.NonNull;

public class LongRangeSerializer implements ObjectSerializer<LongRange> {

    @Override
    public boolean supports(@NonNull Class<?> type) {
        return LongRange.class.isAssignableFrom(type);
    }

    @Override
    public void serialize(@NonNull LongRange range, @NonNull SerializationData data, @NonNull GenericsDeclaration generics) {
        data.add("min", range.getMin());
        data.add("max", range.getMax());
    }

    @Override
    public LongRange deserialize(@NonNull DeserializationData data, @NonNull GenericsDeclaration generics) {

        long min = data.get("min", long.class);
        long max = data.get("max", long.class);

        return LongRange.of(min, max);
    }
}
