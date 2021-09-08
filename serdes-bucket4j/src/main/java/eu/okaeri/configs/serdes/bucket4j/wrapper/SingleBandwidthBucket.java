package eu.okaeri.configs.serdes.bucket4j.wrapper;

import io.github.bucket4j.*;
import io.github.bucket4j.local.LocalBucket;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class SingleBandwidthBucket implements LocalBucket {

    @Delegate private final LocalBucket bucket;
    private final Bandwidth bandwidth;

    public SingleBandwidthBucket(Bandwidth bandwidth) {
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
