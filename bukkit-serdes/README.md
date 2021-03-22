# Okaeri Configs | Bukkit

Serializers/Deserializers/Transformers for Bukkit types. See [bukkit](https://github.com/OkaeriPoland/okaeri-configs/tree/master/bukkit).

## Supported types

### Serializers

| Class | Params |
|-|-|
| org.bukkit.inventory.meta.ItemMeta | display-name, lore, enchantments, item-flags |
| org.bukkit.inventory.ItemStack | material, amount, durability, item-meta |
| org.bukkit.Location | world, x, y, z, yaw, pitch |
| org.bukkit.potion.PotionEffect | amplifier, duration, type |

### Transformers

| Side | Side |
|-|-|
| java.lang.String | org.bukkit.enchantments.Enchantment |
| java.lang.String | org.bukkit.potion.PotionEffectType |
| java.lang.String | org.bukkit.World |
