package eu.okaeri.configs.yaml.bukkit.serdes;

import eu.okaeri.configs.serdes.OkaeriSerdesPack;
import eu.okaeri.configs.serdes.SerdesRegistry;
import eu.okaeri.configs.yaml.bukkit.serdes.itemstack.ItemStackAttachmentResolver;
import eu.okaeri.configs.yaml.bukkit.serdes.serializer.*;
import eu.okaeri.configs.yaml.bukkit.serdes.transformer.StringEnchantmentTransformer;
import eu.okaeri.configs.yaml.bukkit.serdes.transformer.StringPotionEffectTypeTransformer;
import eu.okaeri.configs.yaml.bukkit.serdes.transformer.StringTagTransformer;
import eu.okaeri.configs.yaml.bukkit.serdes.transformer.StringWorldTransformer;
import lombok.NonNull;

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
        registry.register(new StringEnchantmentTransformer());
        registry.register(new StringPotionEffectTypeTransformer());
        registry.register(new StringWorldTransformer());
    }
}
