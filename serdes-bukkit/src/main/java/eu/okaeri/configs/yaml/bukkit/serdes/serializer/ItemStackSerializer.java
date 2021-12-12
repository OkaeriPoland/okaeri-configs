package eu.okaeri.configs.yaml.bukkit.serdes.serializer;

import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.DeserializationData;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.serdes.SerializationData;
import eu.okaeri.configs.yaml.bukkit.serdes.itemstack.ItemStackFormat;
import eu.okaeri.configs.yaml.bukkit.serdes.itemstack.ItemStackSpecData;
import lombok.NonNull;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemStackSerializer implements ObjectSerializer<ItemStack> {

    private static final ItemMetaSerializer ITEM_META_SERIALIZER = new ItemMetaSerializer();

    @Override
    public boolean supports(@NonNull Class<? super ItemStack> type) {
        return ItemStack.class.isAssignableFrom(type);
    }

    @Override
    public void serialize(@NonNull ItemStack itemStack, @NonNull SerializationData data, @NonNull GenericsDeclaration generics) {

        data.add("material", itemStack.getType());

        if (itemStack.getAmount() != 1) {
            data.add("amount", itemStack.getAmount());
        }

        if (itemStack.getDurability() != 0) {
            data.add("durability", itemStack.getDurability());
        }

        ItemStackFormat format = data.getContext().getAttachment(ItemStackSpecData.class)
            .map(ItemStackSpecData::getFormat)
            .orElse(ItemStackFormat.NATURAL);

        if (!itemStack.hasItemMeta()) {
            return;
        }

        switch (format) {
            case NATURAL:
                data.add("item-meta", itemStack.getItemMeta(), ItemMeta.class);
                break;
            case COMPACT:
                ITEM_META_SERIALIZER.serialize(itemStack.getItemMeta(), data, generics);
                break;
            default:
                throw new IllegalArgumentException("Unknown format: " + format);
        }
    }

    @Override
    public ItemStack deserialize(@NonNull DeserializationData data, @NonNull GenericsDeclaration generics) {

        String materialName = data.get("material", String.class);
        Material material = Material.valueOf(materialName);

        int amount = data.containsKey("amount")
            ? data.get("amount", Integer.class)
            : 1;

        short durability = data.containsKey("durability")
            ? data.get("durability", Short.class)
            : 0;

        ItemStackFormat format = data.getContext().getAttachment(ItemStackSpecData.class)
            .map(ItemStackSpecData::getFormat)
            .orElse(ItemStackFormat.NATURAL);

        ItemMeta itemMeta;
        switch (format) {
            case NATURAL:
                // support conversion COMPACT->NATURAL
                if (data.containsKey("display-name")) {
                    itemMeta = ITEM_META_SERIALIZER.deserialize(data, generics);
                }
                // standard deserialize
                else {
                    itemMeta = data.containsKey("item-meta")
                        ? data.get("item-meta", ItemMeta.class)
                        : null;
                }
                break;
            case COMPACT:
                // support conversion NATURAL->COMPACT
                if (data.containsKey("item-meta")) {
                    itemMeta = data.get("item-meta", ItemMeta.class);
                }
                // standard deserialize
                else {
                    itemMeta = ITEM_META_SERIALIZER.deserialize(data, generics);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown format: " + format);
        }

        // create ItemStack base
        ItemStack itemStack = new ItemStack(material, amount);
        // set ItemMeta FIRST due to 1.16+ server
        // ItemStacks storing more and more data
        // here, in the attributes of ItemMeta
        itemStack.setItemMeta(itemMeta);
        // then override durability with setter
        itemStack.setDurability(durability);

        // woah, it works
        return itemStack;
    }
}
