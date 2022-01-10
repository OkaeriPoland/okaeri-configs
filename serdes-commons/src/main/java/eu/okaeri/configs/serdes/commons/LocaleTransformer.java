package eu.okaeri.configs.serdes.commons;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.BidirectionalTransformer;
import eu.okaeri.configs.serdes.SerdesContext;
import lombok.NonNull;

import java.util.Locale;

public class LocaleTransformer extends BidirectionalTransformer<String, Locale> {

    @Override
    public GenericsPair<String, Locale> getPair() {
        return this.genericsPair(String.class, Locale.class);
    }

    @Override
    public Locale leftToRight(@NonNull String data, @NonNull SerdesContext serdesContext) {
        return Locale.forLanguageTag(this.normalize(data));
    }

    @Override
    public String rightToLeft(@NonNull Locale data, @NonNull SerdesContext serdesContext) {
        return this.normalize(data.toString());
    }

    private String normalize(String localeTag) {
        return localeTag.replace("_", "-");
    }
}
