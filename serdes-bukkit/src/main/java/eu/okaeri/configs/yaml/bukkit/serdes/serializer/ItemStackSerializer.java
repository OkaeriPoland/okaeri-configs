package eu.okaeri.configs.yaml.bukkit.serdes.serializer;

import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.DeserializationData;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.serdes.SerializationData;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemStackSerializer implements ObjectSerializer<ItemStack> {

    @Override
    public boolean supports(Class<? super ItemStack> type) {
        return type.isAssignableFrom(ItemStack.class);
    }

    @Override
    public void serialize(ItemStack itemStack, SerializationData data) {

        data.add("material", itemStack.getType());

        if (itemStack.getAmount() != 1) {
            data.add("amount", itemStack.getAmount());
        }

        if (itemStack.getDurability() != 0) {
            data.add("durability", itemStack.getDurability());
        }

        if (itemStack.hasItemMeta()) {
            data.add("item-meta", itemStack.getItemMeta(), ItemMeta.class);
        }
    }

    @Override
    public ItemStack deserialize(DeserializationData data, GenericsDeclaration generics) {

        String materialName = data.get("material", String.class);
        Material material = Material.valueOf(materialName);

        int amount = data.containsKey("amount")
                ? data.get("amount", Integer.class)
                : 1;

        int durability = data.containsKey("durability")
                ? data.get("durability", Integer.class)
                : 0;

        ItemMeta itemMeta = data.containsKey("item-meta")
                ? data.get("item-meta", ItemMeta.class)
                : null;

        ItemStack itemStack = new ItemStack(material, amount);
        itemStack.setDurability((short) durability);
        itemStack.setItemMeta(itemMeta);

        return itemStack;
    }
}
