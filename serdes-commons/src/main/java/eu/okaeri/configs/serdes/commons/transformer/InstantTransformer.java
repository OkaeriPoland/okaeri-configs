package eu.okaeri.configs.serdes.commons.transformer;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.BidirectionalTransformer;
import eu.okaeri.configs.serdes.SerdesContext;
import lombok.NonNull;

import java.time.Instant;

public class InstantTransformer extends BidirectionalTransformer<String, Instant> {

    @Override
    public GenericsPair<String, Instant> getPair() {
        return this.genericsPair(String.class, Instant.class);
    }

    @Override
    public Instant leftToRight(@NonNull String data, @NonNull SerdesContext serdesContext) {
        try {
            return Instant.parse(data);
        } catch (Exception e) {
            throw new IllegalArgumentException("Expected ISO-8601 instant (e.g. 2006-01-02T15:04:05Z)");
        }
    }

    @Override
    public String rightToLeft(@NonNull Instant data, @NonNull SerdesContext serdesContext) {
        return data.toString();
    }
}
