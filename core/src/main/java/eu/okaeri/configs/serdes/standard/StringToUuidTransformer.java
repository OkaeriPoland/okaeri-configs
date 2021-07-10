package eu.okaeri.configs.serdes.standard;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.ObjectTransformer;
import lombok.NonNull;

import java.util.UUID;

public class StringToUuidTransformer extends ObjectTransformer<String, UUID> {

    @Override
    public GenericsPair getPair() {
        return this.genericsPair(String.class, UUID.class);
    }

    @Override
    public UUID transform(@NonNull String data) {
        return UUID.fromString(data);
    }
}
