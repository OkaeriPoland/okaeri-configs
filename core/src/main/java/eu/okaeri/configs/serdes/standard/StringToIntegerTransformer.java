package eu.okaeri.configs.serdes.standard;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.ObjectTransformer;
import lombok.NonNull;

import java.math.BigDecimal;

public class StringToIntegerTransformer extends ObjectTransformer<String, Integer> {

    @Override
    public GenericsPair getPair() {
        return this.genericsPair(String.class, Integer.class);
    }

    @Override
    public Integer transform(@NonNull String data) {
        return new BigDecimal(data).intValueExact();
    }
}
