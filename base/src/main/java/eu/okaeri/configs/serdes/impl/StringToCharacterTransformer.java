package eu.okaeri.configs.serdes.impl;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.transformer.ObjectTransformer;

public class StringToCharacterTransformer extends ObjectTransformer<String, Character> {

    @Override
    public GenericsPair getPair() {
        return this.genericsPair(String.class, Character.class);
    }

    @Override
    public Character transform(String data) {
        if (data.length() > 1) throw new IllegalArgumentException("char '" + data + "' too long: " + data.length() + ">1");
        return data.charAt(0);
    }
}
