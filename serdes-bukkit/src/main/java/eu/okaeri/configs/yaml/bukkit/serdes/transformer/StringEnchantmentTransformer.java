package eu.okaeri.configs.yaml.bukkit.serdes.transformer;

import eu.okaeri.configs.exception.OkaeriException;
import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.BidirectionalTransformer;
import eu.okaeri.configs.serdes.SerdesContext;
import lombok.NonNull;
import org.bukkit.enchantments.Enchantment;

import java.util.*;
import java.util.stream.Collectors;

public class StringEnchantmentTransformer extends BidirectionalTransformer<String, Enchantment> {

    private static Map<String, Enchantment> byName = new HashMap<>();

    @Override
    public GenericsPair<String, Enchantment> getPair() {
        return this.genericsPair(String.class, Enchantment.class);
    }

    @Override
    public Enchantment leftToRight(@NonNull String data, @NonNull SerdesContext serdesContext) {
        Enchantment enchantment = Enchantment.getByName(data);
        if (enchantment == null) {
            if (byName.isEmpty()) {
                // At some point the byName map from Enchantment was removed and getByName
                // was reimplemented as resolve by the enchantment key causing the following:
                // - UNBREAKING.getName() -> DURABILITY
                // - getByName("DURABILITY") -> null
                for (Enchantment e : Enchantment.values()) {
                    byName.put(e.getName().toUpperCase(Locale.ROOT), e);
                }
            }
            enchantment = byName.get(data.toUpperCase(Locale.ROOT));
        }
        if (enchantment == null) {
            String available = Arrays.stream(Enchantment.values()).map(Enchantment::getName).collect(Collectors.joining(", "));
            throw new OkaeriException("Unknown enchantment: " + data + " (Available: " + available);
        }
        return enchantment;
    }

    @Override
    public String rightToLeft(@NonNull Enchantment data, @NonNull SerdesContext serdesContext) {
        return data.getName();
    }
}
