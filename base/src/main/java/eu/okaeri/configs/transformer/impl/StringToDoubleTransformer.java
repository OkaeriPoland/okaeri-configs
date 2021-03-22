package eu.okaeri.configs.transformer.impl;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.transformer.ObjectTransformer;

public class StringToDoubleTransformer extends ObjectTransformer<String, Double> {

    @Override
    public GenericsPair getPair() {
        return this.genericsPair(String.class, Double.class);
    }

    @Override
    public Double transform(String data) {
        return Double.parseDouble(data);
    }
}
