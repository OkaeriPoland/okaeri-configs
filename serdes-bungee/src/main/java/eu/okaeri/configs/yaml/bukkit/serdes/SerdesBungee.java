package eu.okaeri.configs.yaml.bukkit.serdes;

import eu.okaeri.configs.serdes.OkaeriSerdesPack;
import eu.okaeri.configs.serdes.SerdesRegistry;
import eu.okaeri.configs.yaml.bukkit.serdes.transformer.StringChatColorTransformer;

public class SerdesBungee implements OkaeriSerdesPack {

    @Override
    public void register(SerdesRegistry registry) {

        // transformers
        registry.register(new StringChatColorTransformer());
    }
}
