package eu.okaeri.configs.serdes.impl;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.transformer.ObjectTransformer;

public class StringToShortTransformer extends ObjectTransformer<String, Short> {

    @Override
    public GenericsPair getPair() {
        return this.genericsPair(String.class, Short.class);
    }

    @Override
    public Short transform(String data) {
        return Short.parseShort(data);
    }
}
