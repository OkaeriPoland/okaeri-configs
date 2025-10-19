package eu.okaeri.configs.yaml.bungee.serdes;

import eu.okaeri.configs.serdes.OkaeriSerdesPack;
import eu.okaeri.configs.serdes.SerdesRegistry;
import eu.okaeri.configs.yaml.bungee.serdes.transformer.StringChatColorTransformer;
import lombok.NonNull;

public class SerdesBungee implements OkaeriSerdesPack {

    @Override
    public void register(@NonNull SerdesRegistry registry) {

        // transformers
        registry.register(new StringChatColorTransformer());
    }
}
