package eu.okaeri.configs.serdes.impl;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.transformer.ObjectTransformer;

public class ObjectToStringTransformer extends ObjectTransformer<Object, String> {

    @Override
    public GenericsPair getPair() {
        return this.genericsPair(Object.class, String.class);
    }

    @Override
    public String transform(Object data) {
        return data.toString();
    }
}
