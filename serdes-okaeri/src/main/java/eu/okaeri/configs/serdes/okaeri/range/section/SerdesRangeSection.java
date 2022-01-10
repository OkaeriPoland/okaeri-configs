package eu.okaeri.configs.serdes.okaeri.range.section;

import eu.okaeri.configs.serdes.OkaeriSerdesPack;
import eu.okaeri.configs.serdes.SerdesRegistry;

public class SerdesRangeSection implements OkaeriSerdesPack {

    @Override
    public void register(SerdesRegistry registry) {
        registry.register(new ByteRangeSerializer());
        registry.register(new DoubleRangeSerializer());
        registry.register(new FloatRangeSerializer());
        registry.register(new IntRangeSerializer());
        registry.register(new LongRangeSerializer());
        registry.register(new ShortRangeSerializer());
    }
}
