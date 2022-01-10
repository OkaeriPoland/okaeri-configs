package eu.okaeri.configs.serdes.okaeri.range.inline;

import eu.okaeri.commons.range.ByteRange;
import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.BidirectionalTransformer;
import eu.okaeri.configs.serdes.SerdesContext;
import lombok.NonNull;

public class ByteRangeTransformer extends BidirectionalTransformer<String, ByteRange> {

    @Override
    public GenericsPair<String, ByteRange> getPair() {
        return this.genericsPair(String.class, ByteRange.class);
    }

    @Override
    public ByteRange leftToRight(@NonNull String data, @NonNull SerdesContext serdesContext) {
        ByteRange range = ByteRange.valueOf(data);
        if (range == null) {
            throw new RuntimeException("Invalid range: " + data);
        }
        return range;
    }

    @Override
    public String rightToLeft(@NonNull ByteRange range, @NonNull SerdesContext serdesContext) {
        return range.getMin() + "-" + range.getMax();
    }
}
