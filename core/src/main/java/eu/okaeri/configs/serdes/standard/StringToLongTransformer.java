package eu.okaeri.configs.serdes.standard;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.ObjectTransformer;

import java.math.BigDecimal;

public class StringToLongTransformer extends ObjectTransformer<String, Long> {

    @Override
    public GenericsPair getPair() {
        return this.genericsPair(String.class, Long.class);
    }

    @Override
    public Long transform(String data) {
        return new BigDecimal(data).longValueExact();
    }
}
