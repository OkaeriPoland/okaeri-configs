package eu.okaeri.configs.serdes.commons.serializer;

import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.DeserializationData;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.serdes.SerializationData;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@RequiredArgsConstructor
public class InstantSerializer implements ObjectSerializer<Instant> {

    private final boolean numeric;

    @Override
    public boolean supports(@NonNull Class<?> type) {
        return Instant.class.isAssignableFrom(type);
    }

    @Override
    public void serialize(@NonNull Instant object, @NonNull SerializationData data, @NonNull GenericsDeclaration generics) {
        if (this.numeric) {
            data.setValue(object.toEpochMilli(), Long.class);
        } else {
            data.setValue(object.toString());
        }
    }

    @Override
    public Instant deserialize(@NonNull DeserializationData data, @NonNull GenericsDeclaration generics) {
        String str = data.getValue(String.class);
        try {
            long epochMilli = new BigDecimal(str).longValueExact();
            return Instant.ofEpochMilli(epochMilli);
        }
        catch (NumberFormatException e) {
            return Instant.parse(str);
        }
    }
}
