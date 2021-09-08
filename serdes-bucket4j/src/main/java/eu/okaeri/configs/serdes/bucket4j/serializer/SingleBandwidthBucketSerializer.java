package eu.okaeri.configs.serdes.bucket4j.serializer;

import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.DeserializationData;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.serdes.SerializationData;
import eu.okaeri.configs.serdes.bucket4j.wrapper.SingleBandwidthBucket;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Refill;

import java.time.Duration;

public class SingleBandwidthBucketSerializer implements ObjectSerializer<SingleBandwidthBucket> {

    @Override
    public boolean supports(Class<? super SingleBandwidthBucket> clazz) {
        return SingleBandwidthBucket.class.isAssignableFrom(clazz);
    }

    @Override
    public void serialize(SingleBandwidthBucket simpleBucket, SerializationData serializationData) {
        Bandwidth bandwidth = simpleBucket.getBandwidth();
        serializationData.add("capacity", bandwidth.getCapacity());
        serializationData.add("refill-period", Duration.ofNanos(bandwidth.getRefillPeriodNanos()), Duration.class);
        serializationData.add("refill-tokens", bandwidth.getRefillTokens());
    }

    @Override
    public SingleBandwidthBucket deserialize(DeserializationData deserializationData, GenericsDeclaration genericsDeclaration) {

        long capacity = deserializationData.get("capacity", long.class);
        Duration refillPeriod = deserializationData.get("refill-period", Duration.class);
        long refillTokens = deserializationData.get("refill-tokens", long.class);

        return new SingleBandwidthBucket(Bandwidth.classic(capacity, Refill.greedy(refillTokens, refillPeriod)));
    }
}
