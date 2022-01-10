package eu.okaeri.configs.serdes.okaeri.range.inline;

import eu.okaeri.commons.range.ShortRange;
import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.BidirectionalTransformer;
import eu.okaeri.configs.serdes.SerdesContext;
import lombok.NonNull;

public class ShortRangeTransformer extends BidirectionalTransformer<String, ShortRange> {

    @Override
    public GenericsPair<String, ShortRange> getPair() {
        return this.genericsPair(String.class, ShortRange.class);
    }

    @Override
    public ShortRange leftToRight(@NonNull String data, @NonNull SerdesContext serdesContext) {
        ShortRange range = ShortRange.valueOf(data);
        if (range == null) {
            throw new RuntimeException("Invalid range: " + data);
        }
        return range;
    }

    @Override
    public String rightToLeft(@NonNull ShortRange range, @NonNull SerdesContext serdesContext) {
        return range.getMin() + "-" + range.getMax();
    }
}
