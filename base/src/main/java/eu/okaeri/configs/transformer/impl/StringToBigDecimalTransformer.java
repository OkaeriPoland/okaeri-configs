package eu.okaeri.configs.transformer.impl;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.transformer.ObjectTransformer;

import java.math.BigDecimal;

public class StringToBigDecimalTransformer extends ObjectTransformer<String, BigDecimal> {

    @Override
    public GenericsPair getPair() {
        return this.genericsPair(String.class, BigDecimal.class);
    }

    @Override
    public BigDecimal transform(String data) {
        return new BigDecimal(data);
    }
}
