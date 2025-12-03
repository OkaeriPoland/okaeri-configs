package eu.okaeri.configs.serdes.bucket4j.serializer;

import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.DeserializationData;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.serdes.SerializationData;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Refill;
import lombok.NonNull;

import java.time.Duration;

public class BandwidthSerializer implements ObjectSerializer<Bandwidth> {

    @Override
    public boolean supports(@NonNull Class<?> clazz) {
        return Bandwidth.class.isAssignableFrom(clazz);
    }

    @Override
    public void serialize(@NonNull Bandwidth bandwidth, @NonNull SerializationData serializationData, @NonNull GenericsDeclaration generics) {
        serializationData.set("capacity", bandwidth.getCapacity());
        serializationData.set("refill-period", Duration.ofNanos(bandwidth.getRefillPeriodNanos()), Duration.class);
        serializationData.set("refill-tokens", bandwidth.getRefillTokens());
    }

    @Override
    public Bandwidth deserialize(@NonNull DeserializationData deserializationData, @NonNull GenericsDeclaration generics) {

        long capacity = deserializationData.get("capacity", long.class);
        Duration refillPeriod = deserializationData.get("refill-period", Duration.class);
        long refillTokens = deserializationData.get("refill-tokens", long.class);

        return Bandwidth.classic(capacity, Refill.greedy(refillTokens, refillPeriod));
    }
}
