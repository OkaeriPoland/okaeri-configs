package eu.okaeri.configs.bukkit.serdes.transformer;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.TwoSideObjectTransformer;
import org.bukkit.potion.PotionEffectType;

public class StringPotionEffectTypeTransformer extends TwoSideObjectTransformer<String, PotionEffectType> {

    @Override
    public GenericsPair getPair() {
        return this.genericsPair(String.class, PotionEffectType.class);
    }

    @Override
    public PotionEffectType leftToRight(String data) {
        return PotionEffectType.getByName(data);
    }

    @Override
    public String rightToLeft(PotionEffectType data) {
        return data.getName();
    }
}
