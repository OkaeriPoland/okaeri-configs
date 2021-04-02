package eu.okaeri.configs.serdes.impl;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.ObjectTransformer;

import java.math.BigDecimal;

public class StringToIntegerTransformer extends ObjectTransformer<String, Integer> {

    @Override
    public GenericsPair getPair() {
        return this.genericsPair(String.class, Integer.class);
    }

    @Override
    public Integer transform(String data) {
        return new BigDecimal(data).intValueExact();
    }
}
