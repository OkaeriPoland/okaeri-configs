package eu.okaeri.configs.serdes.impl;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.ObjectTransformer;

public class StringToIntegerTransformer extends ObjectTransformer<String, Integer> {

    @Override
    public GenericsPair getPair() {
        return this.genericsPair(String.class, Integer.class);
    }

    @Override
    public Integer transform(String data) {
        return Integer.parseInt(data);
    }
}
