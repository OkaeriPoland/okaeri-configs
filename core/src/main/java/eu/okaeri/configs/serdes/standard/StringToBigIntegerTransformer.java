package eu.okaeri.configs.serdes.standard;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.ObjectTransformer;
import eu.okaeri.configs.serdes.SerdesContext;
import lombok.NonNull;

import java.math.BigInteger;

public class StringToBigIntegerTransformer extends ObjectTransformer<String, BigInteger> {

    @Override
    public GenericsPair<String, BigInteger> getPair() {
        return this.genericsPair(String.class, BigInteger.class);
    }

    @Override
    public BigInteger transform(@NonNull String data, @NonNull SerdesContext serdesContext) {
        return new BigInteger(data);
    }
}
