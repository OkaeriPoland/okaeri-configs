package eu.okaeri.configs.serdes.impl;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.ObjectTransformer;

import java.math.BigDecimal;

public class StringToShortTransformer extends ObjectTransformer<String, Short> {

    @Override
    public GenericsPair getPair() {
        return this.genericsPair(String.class, Short.class);
    }

    @Override
    public Short transform(String data) {
        return new BigDecimal(data).shortValueExact();
    }
}
