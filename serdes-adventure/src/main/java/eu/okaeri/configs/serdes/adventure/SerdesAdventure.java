package eu.okaeri.configs.serdes.adventure;

import eu.okaeri.configs.serdes.adventure.serializer.MiniComponentSerializer;
import eu.okaeri.configs.serdes.adventure.serializer.TextColorSerializer;
import eu.okaeri.configs.serdes.OkaeriSerdes;
import eu.okaeri.configs.serdes.SerdesRegistry;
import lombok.NonNull;

public class SerdesAdventure implements OkaeriSerdes {

    @Override
    public void register(@NonNull SerdesRegistry registry) {
        registry.register(new MiniComponentSerializer());
        registry.register(new TextColorSerializer());
    }
}
