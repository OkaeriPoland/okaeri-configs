package eu.okaeri.configs.serdes.standard;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.ObjectTransformer;

public class StringToFloatTransformer extends ObjectTransformer<String, Float> {

    @Override
    public GenericsPair getPair() {
        return this.genericsPair(String.class, Float.class);
    }

    @Override
    public Float transform(String data) {
        return Float.parseFloat(data);
    }
}
