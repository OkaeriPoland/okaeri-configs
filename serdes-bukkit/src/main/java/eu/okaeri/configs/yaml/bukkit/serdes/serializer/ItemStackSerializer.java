package eu.okaeri.configs.yaml.bukkit.serdes.serializer;

import eu.okaeri.configs.exception.OkaeriException;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.DeserializationData;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.serdes.SerializationData;
import eu.okaeri.configs.yaml.bukkit.serdes.itemstack.ItemStackFailsafe;
import eu.okaeri.configs.yaml.bukkit.serdes.itemstack.ItemStackFormat;
import eu.okaeri.configs.yaml.bukkit.serdes.itemstack.ItemStackSpecData;
import eu.okaeri.configs.yaml.bukkit.serdes.serializer.experimental.CraftItemStackSerializer;
import eu.okaeri.configs.yaml.bukkit.serdes.transformer.experimental.StringBase64ItemStackTransformer;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

@NoArgsConstructor
public class ItemStackSerializer implements ObjectSerializer<ItemStack> {

    private static final ItemMetaSerializer ITEM_META_SERIALIZER = new ItemMetaSerializer();
    private static final StringBase64ItemStackTransformer ITEM_STACK_TRANSFORMER = new StringBase64ItemStackTransformer();
    private static final CraftItemStackSerializer CRAFT_ITEM_STACK_SERIALIZER = new CraftItemStackSerializer(true);
    private ItemStackFailsafe failsafe = ItemStackFailsafe.NONE;

    /**
     * @deprecated Use {@link ItemStackSerializer#ItemStackSerializer(ItemStackFailsafe)}
     */
    @Deprecated
    public ItemStackSerializer(boolean failsafe) {
        this.failsafe = failsafe ? ItemStackFailsafe.BASE64 : ItemStackFailsafe.NONE;
    }

    public ItemStackSerializer(@NonNull ItemStackFailsafe failsafe) {
        this.failsafe = failsafe;
    }

    @Override
    public boolean supports(@NonNull Class<?> type) {
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

        if (this.failsafe == ItemStackFailsafe.NONE) {
            return;
        }

        // check if serialized stack is deserializable
        DeserializationData deserializationData = new DeserializationData(data.asMap(), data.getConfigurer(), data.getContext());
        ItemStack deserializedStack = this.deserialize(deserializationData, generics);

        // human-friendly form is most likely complete
        if (CraftItemStackSerializer.compareDeep(deserializedStack, itemStack)) {
            return;
        }

        // built-in serdes failed, try fallback
        data.clear();

        // bukkit: built-in bukkit yaml serialization
        if (this.failsafe == ItemStackFailsafe.BUKKIT) {
            CRAFT_ITEM_STACK_SERIALIZER.serialize(itemStack, data, generics);
            return;
        }

        // base64: old default
        if (this.failsafe == ItemStackFailsafe.BASE64) {
            String base64Stack = ITEM_STACK_TRANSFORMER.leftToRight(itemStack, data.getContext());
            data.add("base64", base64Stack);
            return;
        }

        // ???
        throw new OkaeriException("Unknown failsafe: " + this.failsafe);
    }

    @Override
    public ItemStack deserialize(@NonNull DeserializationData data, @NonNull GenericsDeclaration generics) {

        // bukkit via YamlBukkitConfigurer
        if (data.getValueRaw() instanceof ItemStack) {
            return (ItemStack) data.getValueRaw();
        }

        // bukkit
        if ("org.bukkit.inventory.ItemStack".equals(data.get("==", String.class))
            || (data.containsKey("v") && data.containsKey("type")) // bukkit's serdes basically since forever
            || (data.containsKey("DataVersion") && data.containsKey("id"))) { // starting something circa 1.21.6-8
            return CRAFT_ITEM_STACK_SERIALIZER.deserialize(data, generics);
        }

        // base64
        if (data.containsKey("base64")) {
            String base64Stack = data.get("base64", String.class);
            return ITEM_STACK_TRANSFORMER.rightToLeft(base64Stack, data.getContext());
        }

        // not itemstack?
        if (!data.containsKey("material")) {
            throw new IllegalArgumentException("Invalid stack: " + data.asMap());
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

        // DO NOT set the durability when unnecessary
        // on 1.21+ this is like a dirt and a dirt:0
        // these are actually different and dont stack
        if (durability != 0) {
            // then override durability with setter
            itemStack.setDurability(durability);
        }

        // woah, it works
        return itemStack;
    }
}
