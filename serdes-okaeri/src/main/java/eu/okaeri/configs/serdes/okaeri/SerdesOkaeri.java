package eu.okaeri.configs.serdes.okaeri;

import eu.okaeri.configs.serdes.OkaeriSerdes;
import eu.okaeri.configs.serdes.SerdesRegistry;
import eu.okaeri.configs.serdes.okaeri.indexedset.IndexedSetAttachmentResolver;
import eu.okaeri.configs.serdes.okaeri.indexedset.IndexedSetSerializer;
import eu.okaeri.configs.serdes.okaeri.range.*;
import lombok.NonNull;

public class SerdesOkaeri implements OkaeriSerdes {

    @Override
    public void register(@NonNull SerdesRegistry registry) {

        // IndexedSet support
        registry.register(new IndexedSetSerializer());
        registry.register(new IndexedSetAttachmentResolver());

        // RomanNumeral support
        registry.register(new RomanNumeralTransformer());

        // Range support
        registry.register(new RangeAttachmentResolver());
        registry.register(new ByteRangeSerializer());
        registry.register(new ShortRangeSerializer());
        registry.register(new IntRangeSerializer());
        registry.register(new LongRangeSerializer());
        registry.register(new FloatRangeSerializer());
        registry.register(new DoubleRangeSerializer());
    }
}
