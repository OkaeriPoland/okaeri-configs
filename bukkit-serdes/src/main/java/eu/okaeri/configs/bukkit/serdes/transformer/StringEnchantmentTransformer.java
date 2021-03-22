package eu.okaeri.configs.bukkit.serdes.transformer;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.TwoSideObjectTransformer;
import org.bukkit.enchantments.Enchantment;

public class StringEnchantmentTransformer extends TwoSideObjectTransformer<String, Enchantment> {

    @Override
    public GenericsPair getPair() {
        return this.genericsPair(String.class, Enchantment.class);
    }

    @Override
    public Enchantment leftToRight(String data) {
        return Enchantment.getByName(data);
    }

    @Override
    public String rightToLeft(Enchantment data) {
        return data.getName();
    }
}
