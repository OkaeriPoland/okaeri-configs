package eu.okaeri.configs.yaml.bukkit.serdes.transformer;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.SerdesContext;
import eu.okaeri.configs.serdes.BidirectionalTransformer;
import lombok.NonNull;
import org.bukkit.enchantments.Enchantment;

public class StringEnchantmentTransformer extends BidirectionalTransformer<String, Enchantment> {

    @Override
    public GenericsPair<String, Enchantment> getPair() {
        return this.genericsPair(String.class, Enchantment.class);
    }

    @Override
    public Enchantment leftToRight(@NonNull String data, @NonNull SerdesContext serdesContext) {
        return Enchantment.getByName(data);
    }

    @Override
    public String rightToLeft(@NonNull Enchantment data, @NonNull SerdesContext serdesContext) {
        return data.getName();
    }
}
