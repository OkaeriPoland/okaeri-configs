package eu.okaeri.configs.transformer.impl;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.transformer.ObjectTransformer;

public class StringToLongTransformer implements ObjectTransformer<String, Long> {

    @Override
    public GenericsPair getPair() {
        return this.genericsPair(String.class, Long.class);
    }

    @Override
    public Long transform(String data) {
        return Long.parseLong(data);
    }
}
