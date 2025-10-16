package eu.okaeri.configs.serdes.okaeri.range.section;

import eu.okaeri.commons.range.ShortRange;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.DeserializationData;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.serdes.SerializationData;
import lombok.NonNull;

public class ShortRangeSerializer implements ObjectSerializer<ShortRange> {

    @Override
    public boolean supports(@NonNull Class<?> type) {
        return ShortRange.class.isAssignableFrom(type);
    }

    @Override
    public void serialize(@NonNull ShortRange range, @NonNull SerializationData data, @NonNull GenericsDeclaration generics) {
        data.add("min", range.getMin());
        data.add("max", range.getMax());
    }

    @Override
    public ShortRange deserialize(@NonNull DeserializationData data, @NonNull GenericsDeclaration generics) {

        short min = data.get("min", short.class);
        short max = data.get("max", short.class);

        return ShortRange.of(min, max);
    }
}
