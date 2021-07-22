package eu.okaeri.configs.serdes.standard;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.ObjectTransformer;
import eu.okaeri.configs.serdes.SerdesContext;
import lombok.NonNull;

import java.util.UUID;

public class StringToUuidTransformer extends ObjectTransformer<String, UUID> {

    @Override
    public GenericsPair<String, UUID> getPair() {
        return this.genericsPair(String.class, UUID.class);
    }

    @Override
    public UUID transform(@NonNull String data, @NonNull SerdesContext serdesContext) {
        return UUID.fromString(data);
    }
}
