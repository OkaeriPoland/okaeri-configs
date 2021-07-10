package eu.okaeri.configs.serdes.standard;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.ObjectTransformer;
import lombok.NonNull;

public class StringToDoubleTransformer extends ObjectTransformer<String, Double> {

    @Override
    public GenericsPair getPair() {
        return this.genericsPair(String.class, Double.class);
    }

    @Override
    public Double transform(@NonNull String data) {
        return Double.parseDouble(data);
    }
}
