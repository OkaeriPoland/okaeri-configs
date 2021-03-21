package eu.okaeri.configs.transformer.impl;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.transformer.ObjectTransformer;

public class StringToIntegerTransformer implements ObjectTransformer<String, Integer> {

    @Override
    public GenericsPair getPair() {
        return this.genericsPair(String.class, Integer.class);
    }

    @Override
    public Integer transform(String data) {
        return Integer.parseInt(data);
    }
}
