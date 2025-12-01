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
        try {
            return new BigDecimal(data).longValueExact();
        } catch (NumberFormatException | ArithmeticException e) {
            throw new IllegalArgumentException("Expected long number (e.g. 42, -10, 9999999999)");
        }
    }
}
