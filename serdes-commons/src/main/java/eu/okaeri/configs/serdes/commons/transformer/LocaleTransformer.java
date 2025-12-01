package eu.okaeri.configs.serdes.commons.transformer;

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
        String normalized = this.normalize(data);
        Locale locale = Locale.forLanguageTag(normalized);
        // Locale.forLanguageTag returns empty locale for invalid tags
        if (locale.getLanguage().isEmpty() && !normalized.isEmpty()) {
            throw new IllegalArgumentException("Expected locale (e.g. en, en-US, pl-PL)");
        }
        return locale;
    }

    @Override
    public String rightToLeft(@NonNull Locale data, @NonNull SerdesContext serdesContext) {
        return this.normalize(data.toString());
    }

    private String normalize(String localeTag) {
        return localeTag.replace("_", "-");
    }
}
