package eu.okaeri.configs.yaml.bukkit.serdes.transformer;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.TwoSideObjectTransformer;
import lombok.NonNull;
import org.bukkit.potion.PotionEffectType;

public class StringPotionEffectTypeTransformer extends TwoSideObjectTransformer<String, PotionEffectType> {

    @Override
    public GenericsPair getPair() {
        return this.genericsPair(String.class, PotionEffectType.class);
    }

    @Override
    public PotionEffectType leftToRight(@NonNull String data) {
        return PotionEffectType.getByName(data);
    }

    @Override
    public String rightToLeft(@NonNull PotionEffectType data) {
        return data.getName();
    }
}
