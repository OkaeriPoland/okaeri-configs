package eu.okaeri.configs.serdes.bucket4j.wrapper;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.local.LocalBucket;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class SingleBandwidthBucket implements LocalBucket {

    @NonNull @Delegate private final LocalBucket bucket;
    @NonNull private final Bandwidth bandwidth;

    public SingleBandwidthBucket(@NonNull Bandwidth bandwidth) {
        this.bucket = Bucket4j.builder()
                .addLimit(bandwidth)
                .build();
        this.bandwidth = bandwidth;
    }

    public Bandwidth getBandwidth() {
        if (this.getConfiguration().getBandwidths().length != 1) {
            throw new IllegalArgumentException("SimpleBucket principle of single bandwidth was broken");
        }
        return this.bandwidth;
    }
}
