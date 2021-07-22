# Okaeri Configs | Bukkit

Serializers/Deserializers/Transformers for Bukkit types. See [yaml-bukkit](https://github.com/OkaeriPoland/okaeri-configs/tree/master/yaml-bukkit).

## Installation
### Maven
Add dependency to the `dependencies` section:
```xml
<dependency>
  <groupId>eu.okaeri</groupId>
  <artifactId>okaeri-configs-serdes-bukkit</artifactId>
  <version>3.0.0</version>
</dependency>
```
### Gradle
Add dependency to the `maven` section:
```groovy
implementation 'eu.okaeri:okaeri-configs-serdes-bukkit:3.0.0'
```

## Supported types

### Serializers

| Class | Params |
|-|-|
| org.bukkit.inventory.meta.ItemMeta | display-name, lore, enchantments, item-flags |
| org.bukkit.inventory.ItemStack | material, amount, durability, item-meta |
| org.bukkit.Location | world, x, y, z, yaw, pitch |
| org.bukkit.potion.PotionEffect | amplifier, duration, type |

### Transformers

| Side | Side | Type |
|-|-|-|
| java.lang.String | org.bukkit.enchantments.Enchantment | Two-side |
| java.lang.String | org.bukkit.potion.PotionEffectType | Two-side |
| java.lang.String | org.bukkit.World | Two-side |
