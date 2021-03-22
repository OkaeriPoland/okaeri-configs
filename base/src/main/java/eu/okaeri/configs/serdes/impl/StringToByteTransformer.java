package eu.okaeri.configs.serdes.impl;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.ObjectTransformer;

public class StringToByteTransformer extends ObjectTransformer<String, Byte> {

    @Override
    public GenericsPair getPair() {
        return this.genericsPair(String.class, Byte.class);
    }

    @Override
    public Byte transform(String data) {
        return Byte.parseByte(data);
    }
}
