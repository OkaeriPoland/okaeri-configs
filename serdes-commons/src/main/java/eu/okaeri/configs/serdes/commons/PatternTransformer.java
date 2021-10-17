package eu.okaeri.configs.serdes.commons;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.SerdesContext;
import eu.okaeri.configs.serdes.BidirectionalTransformer;
import lombok.NonNull;

import java.util.regex.Pattern;

public class PatternTransformer extends BidirectionalTransformer<String, Pattern> {

    @Override
    public GenericsPair<String, Pattern> getPair() {
        return this.genericsPair(String.class, Pattern.class);
    }

    @Override
    public Pattern leftToRight(@NonNull String data, @NonNull SerdesContext serdesContext) {
        return Pattern.compile(data);
    }

    @Override
    public String rightToLeft(@NonNull Pattern data, @NonNull SerdesContext serdesContext) {
        return data.pattern();
    }
}