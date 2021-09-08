package eu.okaeri.configs.serdes.bucket4j.serializer;

import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.DeserializationData;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.serdes.SerializationData;
import eu.okaeri.configs.serdes.bucket4j.wrapper.SingleBandwidthBucket;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.local.LocalBucket;
import io.github.bucket4j.local.LocalBucketBuilder;

import java.util.List;

public class LocalBucketSerializer implements ObjectSerializer<LocalBucket> {

    @Override
    public boolean supports(Class<? super LocalBucket> clazz) {
        return LocalBucket.class.isAssignableFrom(clazz) && !SingleBandwidthBucket.class.isAssignableFrom(clazz);
    }

    @Override
    public void serialize(LocalBucket localBucket, SerializationData serializationData) {
        Bandwidth[] bandwidths = localBucket.getConfiguration().getBandwidths();
        serializationData.addArray("bandwidths", bandwidths, Bandwidth.class);
    }

    @Override
    public LocalBucket deserialize(DeserializationData deserializationData, GenericsDeclaration genericsDeclaration) {

        LocalBucketBuilder builder = Bucket4j.builder();
        List<Bandwidth> bandwidths = deserializationData.getAsList("bandwidths", Bandwidth.class);

        for (Bandwidth bandwidth : bandwidths) {
            builder.addLimit(bandwidth);
        }

        return builder.build();
    }
}
