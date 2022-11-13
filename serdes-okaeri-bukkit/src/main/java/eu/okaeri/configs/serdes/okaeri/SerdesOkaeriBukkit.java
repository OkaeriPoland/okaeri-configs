package eu.okaeri.configs.serdes.okaeri;

import eu.okaeri.configs.serdes.OkaeriSerdesPack;
import eu.okaeri.configs.serdes.SerdesRegistry;
import eu.okaeri.configs.serdes.okaeri.material.TagMaterialSetSerializer;
import lombok.NonNull;

public class SerdesOkaeriBukkit implements OkaeriSerdesPack {

    @Override
    public void register(@NonNull SerdesRegistry registry) {
        whenClass("org.bukkit.Tag", () -> registry.register(new TagMaterialSetSerializer()));
    }

    private static void whenClass(@NonNull String name, @NonNull Runnable runnable) {
        try {
            Class.forName(name);
            runnable.run();
        } catch (ClassNotFoundException ignored) {
        }
    }
}
