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
        try {
            return UUID.fromString(data);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Expected UUID (e.g. 550e8400-e29b-41d4-a716-446655440000)");
        }
    }
}
