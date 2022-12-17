package eu.okaeri.configs.yaml.bukkit.serdes.serializer.experimental;

import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.DeserializationData;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.serdes.SerializationData;
import eu.okaeri.configs.yaml.bukkit.serdes.transformer.experimental.StringBase64ItemStackTransformer;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.yaml.snakeyaml.Yaml;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Alternative ItemStack serialization based on bukkit's
 * YamlConfiguration ability to serialize ItemStacks.
 *
 * Example output:
 * <pre>
 * {@code
 * stack:
 *   type: LEATHER_CHESTPLATE
 *   meta:
 *     ==: ItemMeta
 *     meta-type: LEATHER_ARMOR
 *     display-name: §bArmor
 *     lore:
 *     - §fWoah!
 *     enchants:
 *       DURABILITY: 10
 *     ItemFlags:
 *     - HIDE_ENCHANTS
 *     Unbreakable: true
 *     color:
 *       ==: Color
 *       RED: 0
 *       BLUE: 255
 *       GREEN: 0
 * }
 * </pre>
 *
 * Example override:
 * <pre>
 * {@code registry.register(new SerdesBukkit());}
 * {@code registry.registerExclusive(ItemStack.class, new CraftItemStackSerializer());}
 * </pre>
 *
 * Note: for persistence (non-config) purposes you may want
 * to use {@link StringBase64ItemStackTransformer} instead.
 */
public class CraftItemStackSerializer implements ObjectSerializer<ItemStack> {

    private static final Yaml YAML = new Yaml();

    @Override
    public boolean supports(@NonNull Class<? super ItemStack> type) {
        return ItemStack.class.isAssignableFrom(type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void serialize(@NonNull ItemStack stack, @NonNull SerializationData data, @NonNull GenericsDeclaration generics) {

        YamlConfiguration craftConfig = new YamlConfiguration();
        craftConfig.set("_", stack);

        Map<String, Map<String, Object>> root = YAML.load(craftConfig.saveToString());
        Map<String, Object> itemMap = root.get("_");

        itemMap.remove("==");
        itemMap.forEach(data::add);
    }

    @Override
    @SneakyThrows
    @SuppressWarnings("unchecked")
    public ItemStack deserialize(@NonNull DeserializationData data, @NonNull GenericsDeclaration generics) {

        Map<String, Object> itemMap = new LinkedHashMap<>();
        itemMap.put("==", "org.bukkit.inventory.ItemStack");
        itemMap.putAll(data.asMap());

        YamlConfiguration craftConfig = new YamlConfiguration();
        craftConfig.set("_", itemMap);
        craftConfig.loadFromString(craftConfig.saveToString());

        return craftConfig.getItemStack("_");
    }
}
