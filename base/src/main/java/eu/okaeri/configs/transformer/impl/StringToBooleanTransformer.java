package eu.okaeri.configs.transformer.impl;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.transformer.ObjectTransformer;

public class StringToBooleanTransformer implements ObjectTransformer<String, Boolean> {

    @Override
    public GenericsPair getPair() {
        return this.genericsPair(String.class, Boolean.class);
    }

    @Override
    public Boolean transform(String data) {
        return Boolean.parseBoolean(data);
    }
}
