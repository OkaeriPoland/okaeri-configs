package eu.okaeri.configs.serdes.commons;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.TwoSideObjectTransformer;

import java.time.Duration;

public class DurationTransformer extends TwoSideObjectTransformer<String, Duration> {

    @Override
    public GenericsPair<String, Duration> getPair() {
        return this.genericsPair(String.class, Duration.class);
    }

    @Override
    public Duration leftToRight(String data) {
        return Duration.parse(data);
    }

    @Override
    public String rightToLeft(Duration data) {
        return data.toString();
    }
}