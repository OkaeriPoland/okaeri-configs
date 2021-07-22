package eu.okaeri.configs.serdes.standard;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.ObjectTransformer;
import eu.okaeri.configs.serdes.SerdesContext;
import lombok.NonNull;

import java.math.BigDecimal;

public class StringToBigDecimalTransformer extends ObjectTransformer<String, BigDecimal> {

    @Override
    public GenericsPair<String, BigDecimal> getPair() {
        return this.genericsPair(String.class, BigDecimal.class);
    }

    @Override
    public BigDecimal transform(@NonNull String data, @NonNull SerdesContext serdesContext) {
        return new BigDecimal(data);
    }
}
