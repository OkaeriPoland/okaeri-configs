package eu.okaeri.configs.yaml.bukkit.serdes;

import eu.okaeri.configs.serdes.OkaeriSerdesPack;
import eu.okaeri.configs.serdes.SerdesRegistry;
import eu.okaeri.configs.yaml.bukkit.serdes.itemstack.ItemStackAttachmentResolver;
import eu.okaeri.configs.yaml.bukkit.serdes.serializer.*;
import eu.okaeri.configs.yaml.bukkit.serdes.transformer.*;
import lombok.NonNull;
import org.bukkit.block.Biome;

public class SerdesBukkit implements OkaeriSerdesPack {

    @Override
    public void register(@NonNull SerdesRegistry registry) {

        // serializer/deserializer
        registry.register(new ItemMetaSerializer());
        registry.register(new ItemStackSerializer());
        registry.register(new ItemStackAttachmentResolver());
        registry.register(new LocationSerializer());
        registry.register(new PotionEffectSerializer());
//        registry.register(new ShapedRecipeSerializer(JavaPlugin.getPlugin(MyPlugin.class)));
        registry.register(new VectorSerializer());

        // transformers
        if (!Biome.class.isEnum()) registry.register(new StringBiomeTransformer());
        registry.register(new StringEnchantmentTransformer());
        registry.register(new StringPotionEffectTypeTransformer());
        whenClass("org.bukkit.Tag", () -> registry.register(new StringTagTransformer()));
        registry.register(new StringWorldTransformer());
    }

    private static void whenClass(@NonNull String name, @NonNull Runnable runnable) {
        try {
            Class.forName(name);
            runnable.run();
        } catch (ClassNotFoundException ignored) {
        }
    }
}
