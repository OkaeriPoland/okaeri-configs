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
import lombok.NonNull;

import java.util.List;

public class LocalBucketSerializer implements ObjectSerializer<LocalBucket> {

    @Override
    public boolean supports(@NonNull Class<? super LocalBucket> clazz) {
        return LocalBucket.class.isAssignableFrom(clazz) && !SingleBandwidthBucket.class.isAssignableFrom(clazz);
    }

    @Override
    public void serialize(@NonNull LocalBucket localBucket, @NonNull SerializationData serializationData, @NonNull GenericsDeclaration generics) {
        Bandwidth[] bandwidths = localBucket.getConfiguration().getBandwidths();
        serializationData.addArray("bandwidths", bandwidths, Bandwidth.class);
    }

    @Override
    public LocalBucket deserialize(@NonNull DeserializationData deserializationData, @NonNull GenericsDeclaration generics) {

        LocalBucketBuilder builder = Bucket4j.builder();
        List<Bandwidth> bandwidths = deserializationData.getAsList("bandwidths", Bandwidth.class);

        for (Bandwidth bandwidth : bandwidths) {
            builder.addLimit(bandwidth);
        }

        return builder.build();
    }
}
