package eu.okaeri.configs.serdes.bucket4j;

import eu.okaeri.configs.serdes.OkaeriSerdesPack;
import eu.okaeri.configs.serdes.SerdesRegistry;
import eu.okaeri.configs.serdes.bucket4j.serializer.BandwidthSerializer;
import eu.okaeri.configs.serdes.bucket4j.serializer.LocalBucketSerializer;
import eu.okaeri.configs.serdes.bucket4j.serializer.SingleBandwidthBucketSerializer;
import lombok.NonNull;

public class SerdesBucket4j implements OkaeriSerdesPack {

    @Override
    public void register(@NonNull SerdesRegistry registry) {
        registry.register(new BandwidthSerializer());
        registry.register(new LocalBucketSerializer());
        registry.register(new SingleBandwidthBucketSerializer());
    }
}
