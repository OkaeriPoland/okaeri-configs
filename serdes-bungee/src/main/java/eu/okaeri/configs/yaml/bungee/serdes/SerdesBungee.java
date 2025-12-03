package eu.okaeri.configs.yaml.bungee.serdes;

import eu.okaeri.configs.serdes.OkaeriSerdes;
import eu.okaeri.configs.serdes.SerdesRegistry;
import eu.okaeri.configs.yaml.bungee.serdes.transformer.StringChatColorTransformer;
import lombok.NonNull;

public class SerdesBungee implements OkaeriSerdes {

    @Override
    public void register(@NonNull SerdesRegistry registry) {

        // transformers
        registry.register(new StringChatColorTransformer());
    }
}
