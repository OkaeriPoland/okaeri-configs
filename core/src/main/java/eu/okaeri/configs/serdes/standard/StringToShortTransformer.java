package eu.okaeri.configs.serdes.standard;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.ObjectTransformer;
import lombok.NonNull;

import java.math.BigDecimal;

public class StringToShortTransformer extends ObjectTransformer<String, Short> {

    @Override
    public GenericsPair<String, Short> getPair() {
        return this.genericsPair(String.class, Short.class);
    }

    @Override
    public Short transform(@NonNull String data) {
        return new BigDecimal(data).shortValueExact();
    }
}
