package eu.okaeri.configs.yaml.bukkit.serdes.serializer;

import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.DeserializationData;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.serdes.SerializationData;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ItemMetaSerializer implements ObjectSerializer<ItemMeta> {

    private static final char COLOR_CHAR = '\u00A7';
    private static final char ALT_COLOR_CHAR = '&';

    @Override
    public boolean supports(Class<? super ItemMeta> type) {
        return type.isAssignableFrom(ItemMeta.class);
    }

    @Override
    public void serialize(ItemMeta itemMeta, SerializationData data) {

        if (itemMeta.hasDisplayName()) {
            data.add("display-name", StringUtils.replace(itemMeta.getDisplayName(), COLOR_CHAR + "", ALT_COLOR_CHAR + ""));
        }

        if (itemMeta.hasLore()) {
            data.add("lore", itemMeta.getLore());
        }

        if (!itemMeta.getEnchants().isEmpty()) {
            data.addAsMap("enchantments", itemMeta.getEnchants(), Enchantment.class, Integer.class);
        }

        if (!itemMeta.getItemFlags().isEmpty()) {
            data.addCollection("item-flags", itemMeta.getItemFlags(), ItemFlag.class);
        }
    }

    @Override
    public ItemMeta deserialize(DeserializationData data, GenericsDeclaration generics) {

        String displayName = data.get("display-name", String.class);

        List<String> lore = data.containsKey("lore")
                ? data.getAsList("lore", String.class)
                : Collections.emptyList();

        Map<Enchantment, Integer> enchantments = data.containsKey("enchantments")
                ? data.getAsMap("enchantments", Enchantment.class, Integer.class)
                : Collections.emptyMap();

        List<ItemFlag> itemFlags = data.containsKey("item-flags")
                ? data.getAsList("item-flags", ItemFlag.class)
                : Collections.emptyList();

        ItemMeta itemMeta = new ItemStack(Material.COBBLESTONE).getItemMeta();
        if (displayName != null) {
            itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes(ALT_COLOR_CHAR, displayName));
        }

        itemMeta.setLore(lore);
        enchantments.forEach((enchantment, level) -> itemMeta.addEnchant(enchantment, level, true));
        itemMeta.addItemFlags(itemFlags.toArray(new ItemFlag[0]));

        return itemMeta;
    }
}
