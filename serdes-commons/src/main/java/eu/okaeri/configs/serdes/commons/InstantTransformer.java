package eu.okaeri.configs.serdes.commons;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.TwoSideObjectTransformer;

import java.time.Instant;

public class InstantTransformer extends TwoSideObjectTransformer<String, Instant> {

    @Override
    public GenericsPair getPair() {
        return this.genericsPair(String.class, Instant.class);
    }

    @Override
    public Instant leftToRight(String data) {
        return Instant.parse(data);
    }

    @Override
    public String rightToLeft(Instant data) {
        return data.toString();
    }
}