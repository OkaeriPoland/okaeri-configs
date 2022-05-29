package eu.okaeri.configs.serdes.bucket4j.serializer;

import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.DeserializationData;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.serdes.SerializationData;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConfigurationBuilder;
import lombok.NonNull;

import java.util.List;

public class BucketConfigurationSerializer implements ObjectSerializer<BucketConfiguration> {

    @Override
    public boolean supports(@NonNull Class<? super BucketConfiguration> type) {
        return BucketConfiguration.class.isAssignableFrom(type);
    }

    @Override
    public void serialize(@NonNull BucketConfiguration bucketConfiguration, @NonNull SerializationData data, @NonNull GenericsDeclaration generics) {
        data.addArray("bandwidths", bucketConfiguration.getBandwidths(), Bandwidth.class);
    }

    @Override
    public BucketConfiguration deserialize(@NonNull DeserializationData data, @NonNull GenericsDeclaration generics) {

        ConfigurationBuilder builder = BucketConfiguration.builder();
        List<Bandwidth> bandwidths = data.getAsList("bandwidths", Bandwidth.class);

        for (Bandwidth bandwidth : bandwidths) {
            builder.addLimit(bandwidth);
        }

        return builder.build();
    }
}
