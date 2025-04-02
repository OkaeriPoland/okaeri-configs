package eu.okaeri.configs.yaml.bukkit.serdes.transformer;

import eu.okaeri.configs.exception.OkaeriException;
import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.BidirectionalTransformer;
import eu.okaeri.configs.serdes.SerdesContext;
import lombok.NonNull;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class StringPotionEffectTypeTransformer extends BidirectionalTransformer<String, PotionEffectType> {

    private static Map<String, PotionEffectType> byName = new HashMap<>();

    @Override
    public GenericsPair<String, PotionEffectType> getPair() {
        return this.genericsPair(String.class, PotionEffectType.class);
    }

    @Override
    public PotionEffectType leftToRight(@NonNull String data, @NonNull SerdesContext serdesContext) {
        PotionEffectType potionEffectType = PotionEffectType.getByName(data);
        if (potionEffectType == null) {
            if (byName.isEmpty()) {
                for (PotionEffectType p : PotionEffectType.values()) {
                    byName.put(p.getName().toUpperCase(Locale.ROOT), p);
                }
            }
            potionEffectType = byName.get(data.toUpperCase(Locale.ROOT));
        }
        if (potionEffectType == null) {
            String available = Arrays.stream(PotionEffectType.values()).map(PotionEffectType::getName).collect(Collectors.joining(", "));
            throw new OkaeriException("Unknown potion effect type: " + data + " (Available: " + available + ")");
        }
        return potionEffectType;
    }

    @Override
    public String rightToLeft(@NonNull PotionEffectType data, @NonNull SerdesContext serdesContext) {
        return data.getName();
    }
}
