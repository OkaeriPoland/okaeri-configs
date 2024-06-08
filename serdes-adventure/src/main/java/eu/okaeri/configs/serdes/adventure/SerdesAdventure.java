package eu.okaeri.configs.serdes.adventure;

import eu.okaeri.configs.serdes.adventure.serializer.TextColorSerializer;
import eu.okaeri.configs.serdes.OkaeriSerdesPack;
import eu.okaeri.configs.serdes.SerdesRegistry;
import lombok.NonNull;

public class SerdesAdventure implements OkaeriSerdesPack {

    @Override
    public void register(@NonNull SerdesRegistry registry) {
        registry.register(new TextColorSerializer());
    }
}
