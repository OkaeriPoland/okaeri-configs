package eu.okaeri.configs.yaml.bukkit.serdes.transformer;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.TwoSideObjectTransformer;
import lombok.NonNull;
import org.bukkit.enchantments.Enchantment;

public class StringEnchantmentTransformer extends TwoSideObjectTransformer<String, Enchantment> {

    @Override
    public GenericsPair getPair() {
        return this.genericsPair(String.class, Enchantment.class);
    }

    @Override
    public Enchantment leftToRight(@NonNull String data) {
        return Enchantment.getByName(data);
    }

    @Override
    public String rightToLeft(@NonNull Enchantment data) {
        return data.getName();
    }
}
