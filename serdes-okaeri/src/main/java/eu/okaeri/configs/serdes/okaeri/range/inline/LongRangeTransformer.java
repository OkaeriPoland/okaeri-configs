package eu.okaeri.configs.serdes.okaeri.range.inline;

import eu.okaeri.commons.range.LongRange;
import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.BidirectionalTransformer;
import eu.okaeri.configs.serdes.SerdesContext;
import lombok.NonNull;

public class LongRangeTransformer extends BidirectionalTransformer<String, LongRange> {

    @Override
    public GenericsPair<String, LongRange> getPair() {
        return this.genericsPair(String.class, LongRange.class);
    }

    @Override
    public LongRange leftToRight(@NonNull String data, @NonNull SerdesContext serdesContext) {
        LongRange range = LongRange.valueOf(data);
        if (range == null) {
            throw new RuntimeException("Invalid range: " + data);
        }
        return range;
    }

    @Override
    public String rightToLeft(@NonNull LongRange range, @NonNull SerdesContext serdesContext) {
        return range.getMin() + "-" + range.getMax();
    }
}
