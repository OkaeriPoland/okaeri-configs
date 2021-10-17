package eu.okaeri.configs.yaml.bukkit.serdes.transformer;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.SerdesContext;
import eu.okaeri.configs.serdes.BidirectionalTransformer;
import lombok.NonNull;
import org.bukkit.potion.PotionEffectType;

public class StringPotionEffectTypeTransformer extends BidirectionalTransformer<String, PotionEffectType> {

    @Override
    public GenericsPair<String, PotionEffectType> getPair() {
        return this.genericsPair(String.class, PotionEffectType.class);
    }

    @Override
    public PotionEffectType leftToRight(@NonNull String data, @NonNull SerdesContext serdesContext) {
        return PotionEffectType.getByName(data);
    }

    @Override
    public String rightToLeft(@NonNull PotionEffectType data, @NonNull SerdesContext serdesContext) {
        return data.getName();
    }
}
