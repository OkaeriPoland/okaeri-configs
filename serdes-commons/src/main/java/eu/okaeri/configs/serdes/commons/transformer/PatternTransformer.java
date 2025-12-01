package eu.okaeri.configs.serdes.commons.transformer;

import eu.okaeri.configs.exception.ValueIndexedException;
import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.BidirectionalTransformer;
import eu.okaeri.configs.serdes.SerdesContext;
import lombok.NonNull;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class PatternTransformer extends BidirectionalTransformer<String, Pattern> {

    @Override
    public GenericsPair<String, Pattern> getPair() {
        return this.genericsPair(String.class, Pattern.class);
    }

    @Override
    public Pattern leftToRight(@NonNull String data, @NonNull SerdesContext serdesContext) {
        try {
            return Pattern.compile(data);
        } catch (PatternSyntaxException e) {
            throw new ValueIndexedException(e.getDescription(), e.getIndex(), e);
        }
    }

    @Override
    public String rightToLeft(@NonNull Pattern data, @NonNull SerdesContext serdesContext) {
        return data.pattern();
    }
}
