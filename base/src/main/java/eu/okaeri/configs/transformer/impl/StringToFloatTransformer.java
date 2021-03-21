package eu.okaeri.configs.transformer.impl;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.transformer.ObjectTransformer;

public class StringToFloatTransformer implements ObjectTransformer<String, Float> {

    @Override
    public GenericsPair getPair() {
        return this.genericsPair(String.class, Float.class);
    }

    @Override
    public Float transform(String data) {
        return Float.parseFloat(data);
    }
}
