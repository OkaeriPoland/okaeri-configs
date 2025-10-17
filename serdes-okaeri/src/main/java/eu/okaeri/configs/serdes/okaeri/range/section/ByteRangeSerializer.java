package eu.okaeri.configs.serdes.okaeri.range.section;

import eu.okaeri.commons.range.ByteRange;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.DeserializationData;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.serdes.SerializationData;
import lombok.NonNull;

public class ByteRangeSerializer implements ObjectSerializer<ByteRange> {

    @Override
    public boolean supports(@NonNull Class<?> type) {
        return ByteRange.class.isAssignableFrom(type);
    }

    @Override
    public void serialize(@NonNull ByteRange range, @NonNull SerializationData data, @NonNull GenericsDeclaration generics) {
        data.add("min", range.getMin());
        data.add("max", range.getMax());
    }

    @Override
    public ByteRange deserialize(@NonNull DeserializationData data, @NonNull GenericsDeclaration generics) {

        byte min = data.get("min", byte.class);
        byte max = data.get("max", byte.class);

        return ByteRange.of(min, max);
    }
}
