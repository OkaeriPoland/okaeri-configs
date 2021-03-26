package eu.okaeri.configs.yaml.bukkit.serdes.serializer;

import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.DeserializationData;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.serdes.SerializationData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PotionEffectSerializer implements ObjectSerializer<PotionEffect> {

    @Override
    public boolean supports(Class<? super PotionEffect> type) {
        return type.isAssignableFrom(PotionEffect.class);
    }

    @Override
    public void serialize(PotionEffect potionEffect, SerializationData data) {
        data.add("amplifier", potionEffect.getAmplifier());
        data.add("duration", potionEffect.getDuration());
        data.add("type", potionEffect.getType(), PotionEffectType.class);
    }

    @Override
    public PotionEffect deserialize(DeserializationData data, GenericsDeclaration generics) {

        int amplifier = data.get("amplifier", Integer.class);
        int duration = data.get("duration", Byte.class);
        PotionEffectType potionEffectType = data.get("type", PotionEffectType.class);

        return new PotionEffect(potionEffectType, duration, amplifier);
    }
}
