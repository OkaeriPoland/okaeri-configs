package eu.okaeri.configs.bukkit.serdes;

import eu.okaeri.configs.bukkit.serdes.impl.ItemMetaSerializer;
import eu.okaeri.configs.bukkit.serdes.impl.ItemStackSerializer;
import eu.okaeri.configs.bukkit.serdes.impl.LocationSerializer;
import eu.okaeri.configs.bukkit.serdes.impl.PotionEffectSerializer;
import eu.okaeri.configs.bukkit.serdes.transformer.StringEnchantmentTransformer;
import eu.okaeri.configs.bukkit.serdes.transformer.StringWorldTransformer;
import eu.okaeri.configs.serdes.OkaeriSerdesPack;
import eu.okaeri.configs.transformer.TransformerRegistry;

public class BukkitSerdes implements OkaeriSerdesPack {

    @Override
    public void register() {

        // serializer/deserializer
        TransformerRegistry.register(new ItemMetaSerializer());
        TransformerRegistry.register(new ItemStackSerializer());
        TransformerRegistry.register(new LocationSerializer());
        TransformerRegistry.register(new PotionEffectSerializer());

        // transformers
        TransformerRegistry.register(new StringWorldTransformer());
        TransformerRegistry.register(new StringEnchantmentTransformer());
    }
}
