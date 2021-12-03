package eu.okaeri.configs.yaml.bukkit.serdes.serializer;

import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.DeserializationData;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.serdes.SerializationData;
import lombok.NonNull;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PotionEffectSerializer implements ObjectSerializer<PotionEffect> {

    @Override
    public boolean supports(@NonNull Class<? super PotionEffect> type) {
        return PotionEffect.class.isAssignableFrom(type);
    }

    @Override
    public void serialize(@NonNull PotionEffect potionEffect, @NonNull SerializationData data, @NonNull GenericsDeclaration generics) {
        data.add("amplifier", potionEffect.getAmplifier());
        data.add("duration", potionEffect.getDuration());
        data.add("type", potionEffect.getType(), PotionEffectType.class);
    }

    @Override
    public PotionEffect deserialize(@NonNull DeserializationData data, @NonNull GenericsDeclaration generics) {

        int amplifier = data.get("amplifier", Integer.class);
        int duration = data.get("duration", Integer.class);
        PotionEffectType potionEffectType = data.get("type", PotionEffectType.class);

        return new PotionEffect(potionEffectType, duration, amplifier);
    }
}
