package eu.okaeri.configs.serdes.commons;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.SerdesContext;
import eu.okaeri.configs.serdes.BidirectionalTransformer;
import lombok.NonNull;

import java.time.Instant;

public class InstantTransformer extends BidirectionalTransformer<String, Instant> {

    @Override
    public GenericsPair<String, Instant> getPair() {
        return this.genericsPair(String.class, Instant.class);
    }

    @Override
    public Instant leftToRight(@NonNull String data, @NonNull SerdesContext serdesContext) {
        return Instant.parse(data);
    }

    @Override
    public String rightToLeft(@NonNull Instant data, @NonNull SerdesContext serdesContext) {
        return data.toString();
    }
}