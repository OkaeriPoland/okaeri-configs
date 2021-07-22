package eu.okaeri.configs.serdes.standard;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.ObjectTransformer;
import eu.okaeri.configs.serdes.SerdesContext;
import lombok.NonNull;

import java.math.BigDecimal;

public class StringToLongTransformer extends ObjectTransformer<String, Long> {

    @Override
    public GenericsPair<String, Long> getPair() {
        return this.genericsPair(String.class, Long.class);
    }

    @Override
    public Long transform(@NonNull String data, @NonNull SerdesContext serdesContext) {
        return new BigDecimal(data).longValueExact();
    }
}
