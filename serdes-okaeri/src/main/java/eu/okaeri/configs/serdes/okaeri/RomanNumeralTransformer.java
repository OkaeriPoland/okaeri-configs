package eu.okaeri.configs.serdes.okaeri;

import eu.okaeri.commons.RomanNumeral;
import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.BidirectionalTransformer;
import eu.okaeri.configs.serdes.SerdesContext;
import lombok.NonNull;

public class RomanNumeralTransformer extends BidirectionalTransformer<String, RomanNumeral> {

    @Override
    public GenericsPair<String, RomanNumeral> getPair() {
        return this.genericsPair(String.class, RomanNumeral.class);
    }

    @Override
    public RomanNumeral leftToRight(@NonNull String data, @NonNull SerdesContext serdesContext) {
        return new RomanNumeral(data);
    }

    @Override
    public String rightToLeft(RomanNumeral data, @NonNull SerdesContext serdesContext) {
        return data.toString();
    }
}
