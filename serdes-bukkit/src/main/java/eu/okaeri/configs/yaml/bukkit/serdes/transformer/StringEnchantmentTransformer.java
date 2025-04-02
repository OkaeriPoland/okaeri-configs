package eu.okaeri.configs.yaml.bukkit.serdes.transformer;

import eu.okaeri.configs.exception.OkaeriException;
import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.BidirectionalTransformer;
import eu.okaeri.configs.serdes.SerdesContext;
import lombok.NonNull;
import org.bukkit.enchantments.Enchantment;

import java.util.Arrays;
import java.util.stream.Collectors;

public class StringEnchantmentTransformer extends BidirectionalTransformer<String, Enchantment> {

    @Override
    public GenericsPair<String, Enchantment> getPair() {
        return this.genericsPair(String.class, Enchantment.class);
    }

    @Override
    public Enchantment leftToRight(@NonNull String data, @NonNull SerdesContext serdesContext) {
        Enchantment enchantment = Enchantment.getByName(data);
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
