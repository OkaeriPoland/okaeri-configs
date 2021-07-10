package eu.okaeri.configs.serdes.standard;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.ObjectTransformer;
import lombok.NonNull;

import java.math.BigDecimal;

public class StringToByteTransformer extends ObjectTransformer<String, Byte> {

    @Override
    public GenericsPair getPair() {
        return this.genericsPair(String.class, Byte.class);
    }

    @Override
    public Byte transform(@NonNull String data) {
        return new BigDecimal(data).byteValueExact();
    }
}
