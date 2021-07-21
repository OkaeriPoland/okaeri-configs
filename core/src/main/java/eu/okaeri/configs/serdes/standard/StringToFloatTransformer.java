package eu.okaeri.configs.serdes.standard;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.ObjectTransformer;
import lombok.NonNull;

public class StringToFloatTransformer extends ObjectTransformer<String, Float> {

    @Override
    public GenericsPair<String, Float> getPair() {
        return this.genericsPair(String.class, Float.class);
    }

    @Override
    public Float transform(@NonNull String data) {
        return Float.parseFloat(data);
    }
}
