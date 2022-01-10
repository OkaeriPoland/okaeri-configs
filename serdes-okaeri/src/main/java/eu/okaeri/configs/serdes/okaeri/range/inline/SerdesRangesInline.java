package eu.okaeri.configs.serdes.okaeri.range.inline;

import eu.okaeri.configs.serdes.OkaeriSerdesPack;
import eu.okaeri.configs.serdes.SerdesRegistry;

public class SerdesRangesInline implements OkaeriSerdesPack {

    @Override
    public void register(SerdesRegistry registry) {
        registry.register(new ByteRangeTransformer());
        registry.register(new DoubleRangeTransformer());
        registry.register(new FloatRangeTransformer());
        registry.register(new IntRangeTransformer());
        registry.register(new LongRangeTransformer());
        registry.register(new ShortRangeTransformer());
    }
}
