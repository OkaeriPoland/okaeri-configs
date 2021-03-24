package eu.okaeri.configs.serdes.impl;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.ObjectTransformer;

public class StringToLongTransformer extends ObjectTransformer<String, Long> {

    @Override
    public GenericsPair getPair() {
        return this.genericsPair(String.class, Long.class);
    }

    @Override
    public Long transform(String data) {
        return Long.parseLong(data);
    }
}
