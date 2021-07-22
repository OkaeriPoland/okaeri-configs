package eu.okaeri.configs.serdes.commons;

import eu.okaeri.configs.serdes.OkaeriSerdesPack;
import eu.okaeri.configs.serdes.SerdesRegistry;
import eu.okaeri.configs.serdes.commons.duration.DurationAttachmentResolver;
import eu.okaeri.configs.serdes.commons.duration.DurationTransformer;

// types that exceed standard typeset
public class SerdesCommons implements OkaeriSerdesPack {

    @Override
    public void register(SerdesRegistry registry) {

        registry.register(new DurationTransformer());
        registry.register(new DurationAttachmentResolver());

        registry.register(new InstantTransformer());
        registry.register(new PatternTransformer());
    }
}
