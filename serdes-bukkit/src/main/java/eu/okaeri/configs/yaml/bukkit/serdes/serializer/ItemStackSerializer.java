package eu.okaeri.configs.yaml.bukkit.serdes.serializer;

import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.DeserializationData;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.serdes.SerializationData;
import eu.okaeri.configs.yaml.bukkit.serdes.itemstack.ItemStackFormat;
import eu.okaeri.configs.yaml.bukkit.serdes.itemstack.ItemStackSpecData;
import eu.okaeri.configs.yaml.bukkit.serdes.transformer.experimental.StringBase64ItemStackTransformer;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

@NoArgsConstructor
@AllArgsConstructor
public class ItemStackSerializer implements ObjectSerializer<ItemStack> {

    private static final ItemMetaSerializer ITEM_META_SERIALIZER = new ItemMetaSerializer();
    private static final StringBase64ItemStackTransformer ITEM_STACK_TRANSFORMER = new StringBase64ItemStackTransformer();
    private boolean failsafe = false;

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
                data.add("meta", itemStack.getItemMeta(), ItemMeta.class);
                break;
            case COMPACT:
                ITEM_META_SERIALIZER.serialize(itemStack.getItemMeta(), data, generics);
                break;
            default:
                throw new IllegalArgumentException("Unknown format: " + format);
        }

        if (!this.failsafe) {
            return;
        }

        // check if serialized stack is deserializable
        DeserializationData deserializationData = new DeserializationData(data.asMap(), data.getConfigurer(), data.getContext());
        ItemStack deserializedStack = this.deserialize(deserializationData, generics);

        // human-friendly form is most likely complete
        if (deserializedStack.equals(itemStack)) {
            return;
        }

        // human-friendly failed, use base64 instead
        data.clear();
        String base64Stack = ITEM_STACK_TRANSFORMER.leftToRight(itemStack, data.getContext());
        data.add("base64", base64Stack);
    }

    @Override
    public ItemStack deserialize(@NonNull DeserializationData data, @NonNull GenericsDeclaration generics) {

        // base64
        if (data.containsKey("base64")) {
            String base64Stack = data.get("base64", String.class);
            return ITEM_STACK_TRANSFORMER.rightToLeft(base64Stack, data.getContext());
        }

        // human-friendly
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
                if (data.containsKey("display") || data.containsKey("display-name")/* legacy */) {
                    itemMeta = ITEM_META_SERIALIZER.deserialize(data, generics);
                }
                // standard deserialize
                else {
                    itemMeta = data.containsKey("meta")
                        ? data.get("meta", ItemMeta.class)
                        : data.get("item-meta", ItemMeta.class); // legacy
                }
                break;
            case COMPACT:
                // support conversion NATURAL->COMPACT
                if (data.containsKey("meta")) {
                    itemMeta = data.get("meta", ItemMeta.class);
                } else if (data.containsKey("item-meta")) { // legacy
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
