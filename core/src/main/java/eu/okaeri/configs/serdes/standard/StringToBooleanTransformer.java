package eu.okaeri.configs.serdes.standard;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.ObjectTransformer;
import lombok.NonNull;

public class StringToBooleanTransformer extends ObjectTransformer<String, Boolean> {

    @Override
    public GenericsPair getPair() {
        return this.genericsPair(String.class, Boolean.class);
    }

    @Override
    public Boolean transform(@NonNull String data) {
        return Boolean.parseBoolean(data);
    }
}
