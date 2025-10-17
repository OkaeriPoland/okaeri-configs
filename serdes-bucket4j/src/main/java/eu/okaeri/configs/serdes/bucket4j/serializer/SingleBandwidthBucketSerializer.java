package eu.okaeri.configs.serdes.bucket4j.serializer;

import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.DeserializationData;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.serdes.SerializationData;
import eu.okaeri.configs.serdes.bucket4j.wrapper.SingleBandwidthBucket;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Refill;
import lombok.NonNull;

import java.time.Duration;

public class SingleBandwidthBucketSerializer implements ObjectSerializer<SingleBandwidthBucket> {

    @Override
    public boolean supports(@NonNull Class<?> clazz) {
        return SingleBandwidthBucket.class.isAssignableFrom(clazz);
    }

    @Override
    public void serialize(@NonNull SingleBandwidthBucket simpleBucket, @NonNull SerializationData serializationData, @NonNull GenericsDeclaration generics) {
        Bandwidth bandwidth = simpleBucket.getBandwidth();
        serializationData.add("capacity", bandwidth.getCapacity());
        serializationData.add("refill-period", Duration.ofNanos(bandwidth.getRefillPeriodNanos()), Duration.class);
        serializationData.add("refill-tokens", bandwidth.getRefillTokens());
    }

    @Override
    public SingleBandwidthBucket deserialize(@NonNull DeserializationData deserializationData, @NonNull GenericsDeclaration generics) {

        long capacity = deserializationData.get("capacity", long.class);
        Duration refillPeriod = deserializationData.get("refill-period", Duration.class);
        long refillTokens = deserializationData.get("refill-tokens", long.class);

        return new SingleBandwidthBucket(Bandwidth.classic(capacity, Refill.greedy(refillTokens, refillPeriod)));
    }
}
