package eu.okaeri.configs.serdes.impl;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.transformer.ObjectTransformer;

import java.math.BigInteger;

public class StringToBigIntegerTransformer extends ObjectTransformer<String, BigInteger> {

    @Override
    public GenericsPair getPair() {
        return this.genericsPair(String.class, BigInteger.class);
    }

    @Override
    public BigInteger transform(String data) {
        return new BigInteger(data);
    }
}
