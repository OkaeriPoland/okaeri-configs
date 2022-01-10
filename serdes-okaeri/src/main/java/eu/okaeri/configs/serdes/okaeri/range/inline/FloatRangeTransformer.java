package eu.okaeri.configs.serdes.okaeri.range.inline;

import eu.okaeri.commons.range.FloatRange;
import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.BidirectionalTransformer;
import eu.okaeri.configs.serdes.SerdesContext;
import lombok.NonNull;

public class FloatRangeTransformer extends BidirectionalTransformer<String, FloatRange> {

    @Override
    public GenericsPair<String, FloatRange> getPair() {
        return this.genericsPair(String.class, FloatRange.class);
    }

    @Override
    public FloatRange leftToRight(@NonNull String data, @NonNull SerdesContext serdesContext) {
        FloatRange range = FloatRange.valueOf(data);
        if (range == null) {
            throw new RuntimeException("Invalid range: " + data);
        }
        return range;
    }

    @Override
    public String rightToLeft(@NonNull FloatRange range, @NonNull SerdesContext serdesContext) {
        return range.getMin() + "-" + range.getMax();
    }
}
