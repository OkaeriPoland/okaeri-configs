package eu.okaeri.configs.serdes.okaeri;

import eu.okaeri.configs.serdes.OkaeriSerdesPack;
import eu.okaeri.configs.serdes.SerdesRegistry;
import eu.okaeri.configs.serdes.okaeri.indexedset.IndexedSetAttachmentResolver;
import eu.okaeri.configs.serdes.okaeri.indexedset.IndexedSetSerializer;
import lombok.NonNull;

public class SerdesOkaeri implements OkaeriSerdesPack {

    @Override
    public void register(@NonNull SerdesRegistry registry) {

        registry.register(new IndexedSetSerializer());
        registry.register(new IndexedSetAttachmentResolver());

        registry.register(new RomanNumeralTransformer());
    }
}
