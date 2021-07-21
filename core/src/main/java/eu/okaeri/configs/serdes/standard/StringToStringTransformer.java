package eu.okaeri.configs.serdes.standard;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.ObjectTransformer;
import lombok.NonNull;

public class StringToStringTransformer extends ObjectTransformer<String, String> {

    @Override
    public GenericsPair<String, String> getPair() {
        return this.genericsPair(String.class, String.class);
    }

    @Override
    public String transform(@NonNull String data) {
        return data;
    }
}
