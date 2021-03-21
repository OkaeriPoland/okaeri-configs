package eu.okaeri.configs.transformer.impl;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.transformer.ObjectTransformer;

public class StringToByteTransformer implements ObjectTransformer<String, Byte> {

    @Override
    public GenericsPair getPair() {
        return this.genericsPair(String.class, Byte.class);
    }

    @Override
    public Byte transform(String data) {
        return Byte.parseByte(data);
    }
}
