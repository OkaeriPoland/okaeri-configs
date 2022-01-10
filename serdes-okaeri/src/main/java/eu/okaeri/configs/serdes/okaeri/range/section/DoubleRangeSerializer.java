package eu.okaeri.configs.serdes.okaeri.range.section;

import eu.okaeri.commons.range.DoubleRange;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.DeserializationData;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.serdes.SerializationData;
import lombok.NonNull;

public class DoubleRangeSerializer implements ObjectSerializer<DoubleRange> {

    @Override
    public boolean supports(@NonNull Class<? super DoubleRange> type) {
        return DoubleRange.class.isAssignableFrom(type);
    }

    @Override
    public void serialize(@NonNull DoubleRange range, @NonNull SerializationData data, @NonNull GenericsDeclaration generics) {
        data.add("min", range.getMin());
        data.add("max", range.getMax());
    }

    @Override
    public DoubleRange deserialize(@NonNull DeserializationData data, @NonNull GenericsDeclaration generics) {

        double min = data.get("min", double.class);
        double max = data.get("max", double.class);

        return DoubleRange.of(min, max);
    }
}
