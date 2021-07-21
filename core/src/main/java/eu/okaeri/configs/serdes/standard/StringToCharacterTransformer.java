package eu.okaeri.configs.serdes.standard;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.ObjectTransformer;
import lombok.NonNull;

public class StringToCharacterTransformer extends ObjectTransformer<String, Character> {

    @Override
    public GenericsPair<String, Character> getPair() {
        return this.genericsPair(String.class, Character.class);
    }

    @Override
    public Character transform(@NonNull String data) {
        if (data.length() > 1) throw new IllegalArgumentException("char '" + data + "' too long: " + data.length() + ">1");
        return data.charAt(0);
    }
}
