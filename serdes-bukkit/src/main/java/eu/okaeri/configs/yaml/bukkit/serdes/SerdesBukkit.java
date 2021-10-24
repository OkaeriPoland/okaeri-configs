package eu.okaeri.configs.yaml.bukkit.serdes;

import eu.okaeri.configs.serdes.OkaeriSerdesPack;
import eu.okaeri.configs.serdes.SerdesRegistry;
import eu.okaeri.configs.yaml.bukkit.serdes.itemstack.ItemStackAttachmentResolver;
import eu.okaeri.configs.yaml.bukkit.serdes.serializer.ItemMetaSerializer;
import eu.okaeri.configs.yaml.bukkit.serdes.serializer.ItemStackSerializer;
import eu.okaeri.configs.yaml.bukkit.serdes.serializer.LocationSerializer;
import eu.okaeri.configs.yaml.bukkit.serdes.serializer.PotionEffectSerializer;
import eu.okaeri.configs.yaml.bukkit.serdes.transformer.StringEnchantmentTransformer;
import eu.okaeri.configs.yaml.bukkit.serdes.transformer.StringPotionEffectTypeTransformer;
import eu.okaeri.configs.yaml.bukkit.serdes.transformer.StringWorldTransformer;

public class SerdesBukkit implements OkaeriSerdesPack {

    @Override
    public void register(SerdesRegistry registry) {

        // serializer/deserializer
        registry.register(new ItemMetaSerializer());
        registry.register(new ItemStackSerializer());
        registry.register(new ItemStackAttachmentResolver());
        registry.register(new LocationSerializer());
        registry.register(new PotionEffectSerializer());

        // transformers
        registry.register(new StringEnchantmentTransformer());
        registry.register(new StringPotionEffectTypeTransformer());
        registry.register(new StringWorldTransformer());
    }
}
