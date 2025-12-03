package eu.okaeri.configs.yaml.bukkit.serdes.serializer;

import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.DeserializationData;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.serdes.SerializationData;
import lombok.NonNull;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ItemMetaSerializer implements ObjectSerializer<ItemMeta> {

    private static final char COLOR_CHAR = '\u00A7';
    private static final char ALT_COLOR_CHAR = '&';

    @Override
    public boolean supports(@NonNull Class<?> type) {
        return ItemMeta.class.isAssignableFrom(type);
    }

    @Override
    public void serialize(@NonNull ItemMeta itemMeta, @NonNull SerializationData data, @NonNull GenericsDeclaration generics) {

        if (itemMeta.hasDisplayName()) {
            data.set("display", this.decolor(itemMeta.getDisplayName()));
        }

        if (itemMeta.hasLore()) {
            data.setCollection("lore", this.decolor(itemMeta.getLore()), String.class);
        }

        if (!itemMeta.getEnchants().isEmpty()) {
            data.setMap("enchantments", itemMeta.getEnchants(), Enchantment.class, Integer.class);
        }

        if (!itemMeta.getItemFlags().isEmpty()) {
            data.setCollection("flags", itemMeta.getItemFlags(), ItemFlag.class);
        }
    }

    @Override
    public ItemMeta deserialize(@NonNull DeserializationData data, @NonNull GenericsDeclaration generics) {

        String displayName = data.get("display", String.class);
        if (displayName == null) { // legacy
            displayName = data.get("display-name", String.class);
        }

        List<String> lore = data.containsKey("lore")
            ? data.getAsList("lore", String.class)
            : Collections.emptyList();

        Map<Enchantment, Integer> enchantments = data.containsKey("enchantments")
            ? data.getAsMap("enchantments", Enchantment.class, Integer.class)
            : Collections.emptyMap();

        List<ItemFlag> itemFlags = new ArrayList<>(data.containsKey("flags")
            ? data.getAsList("flags", ItemFlag.class)
            : Collections.emptyList());

        if (data.containsKey("item-flags")) { // legacy
            itemFlags.addAll(data.getAsList("item-flags", ItemFlag.class));
        }

        ItemMeta itemMeta = new ItemStack(Material.COBBLESTONE).getItemMeta();
        if (itemMeta == null) {
            throw new IllegalStateException("Cannot extract empty ItemMeta from COBBLESTONE");
        }

        if (displayName != null) {
            itemMeta.setDisplayName(this.color(displayName));
        }

        itemMeta.setLore(this.color(lore));

        enchantments.forEach((enchantment, level) -> itemMeta.addEnchant(enchantment, level, true));
        itemMeta.addItemFlags(itemFlags.toArray(new ItemFlag[0]));

        return itemMeta;
    }

    private List<String> color(List<String> text) {
        return text.stream().map(this::color).collect(Collectors.toList());
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes(ALT_COLOR_CHAR, text);
    }

    private List<String> decolor(List<String> text) {
        return text.stream().map(this::decolor).collect(Collectors.toList());
    }

    private String decolor(String text) {
        return text.replace(COLOR_CHAR + "", ALT_COLOR_CHAR + "");
    }
}
