package eu.okaeri.configs.serdes.commons;

import eu.okaeri.configs.serdes.OkaeriSerdes;
import eu.okaeri.configs.serdes.SerdesRegistry;
import eu.okaeri.configs.serdes.commons.duration.DurationAttachmentResolver;
import eu.okaeri.configs.serdes.commons.duration.DurationTransformer;
import eu.okaeri.configs.serdes.commons.serializer.InstantSerializer;
import eu.okaeri.configs.serdes.commons.transformer.LocaleTransformer;
import eu.okaeri.configs.serdes.commons.transformer.PatternTransformer;
import lombok.NonNull;

// types that exceed standard typeset
public class SerdesCommons implements OkaeriSerdes {

    @Override
    public void register(@NonNull SerdesRegistry registry) {

        registry.register(new DurationTransformer());
        registry.register(new DurationAttachmentResolver());

        registry.register(new InstantSerializer(false));

        registry.register(new LocaleTransformer());
        registry.register(new PatternTransformer());
    }
}
