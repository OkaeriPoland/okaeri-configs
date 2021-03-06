package eu.okaeri.configs.serdes.standard;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.ObjectTransformer;
import eu.okaeri.configs.serdes.SerdesContext;
import lombok.NonNull;

public class StringToDoubleTransformer extends ObjectTransformer<String, Double> {

    @Override
    public GenericsPair<String, Double> getPair() {
        return this.genericsPair(String.class, Double.class);
    }

    @Override
    public Double transform(@NonNull String data, @NonNull SerdesContext serdesContext) {
        return Double.parseDouble(data);
    }
}
