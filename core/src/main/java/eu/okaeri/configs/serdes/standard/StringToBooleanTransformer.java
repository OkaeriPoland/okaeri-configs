package eu.okaeri.configs.serdes.standard;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.ObjectTransformer;
import eu.okaeri.configs.serdes.SerdesContext;
import lombok.NonNull;

public class StringToBooleanTransformer extends ObjectTransformer<String, Boolean> {

    @Override
    public GenericsPair<String, Boolean> getPair() {
        return this.genericsPair(String.class, Boolean.class);
    }

    @Override
    public Boolean transform(@NonNull String data, @NonNull SerdesContext serdesContext) {
        return Boolean.parseBoolean(data);
    }
}
