package eu.okaeri.configs.serdes.commons;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.TwoSideObjectTransformer;

import java.util.regex.Pattern;

public class PatternTransformer extends TwoSideObjectTransformer<String, Pattern> {

    @Override
    public GenericsPair getPair() {
        return this.genericsPair(String.class, Pattern.class);
    }

    @Override
    public Pattern leftToRight(String data) {
        return Pattern.compile(data);
    }

    @Override
    public String rightToLeft(Pattern data) {
        return data.pattern();
    }
}