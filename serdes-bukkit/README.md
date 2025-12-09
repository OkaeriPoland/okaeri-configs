# Okaeri Configs | Bukkit

Serializers/Deserializers/Transformers for Bukkit types. See [yaml-bukkit](https://github.com/OkaeriPoland/okaeri-configs/tree/master/yaml-bukkit).

```java
new SerdesBukkit()
```

## Installation

### Maven

Add dependency to the `dependencies` section:

```xml
<dependency>
  <groupId>eu.okaeri</groupId>
  <artifactId>okaeri-configs-serdes-bukkit</artifactId>
  <version>6.0.0-beta.24</version>
</dependency>
```

### Gradle (Kotlin)

Add dependency to the `dependencies` section:

```kotlin
implementation("eu.okaeri:okaeri-configs-serdes-bukkit:6.0.0-beta.24")
```

## Supported types

### Serializers

| Class | Params |
|-|-|
| org.bukkit.inventory.meta.ItemMeta | display-name, lore, enchantments, item-flags |
| org.bukkit.inventory.ItemStack | material, amount, durability, item-meta |
| org.bukkit.Location | world, x, y, z, yaw, pitch |
| org.bukkit.potion.PotionEffect | amplifier, duration, type |
| org.bukkit.inventory.ShapedRecipe | key, shape, ingredients, result |
| org.bukkit.util.Vector | x, y, z |

Note: ShapedRecipeSerializer is not registered by default.

### Transformers

| Side | Side | Type |
|-|-|-|
| java.lang.String | org.bukkit.enchantments.Enchantment | Two-side |
| java.lang.String | org.bukkit.potion.PotionEffectType | Two-side |
| java.lang.String | org.bukkit.Tag | Two-side |
| java.lang.String | org.bukkit.World | Two-side |

### Transformers (experimental)

| Transformer | Side | Side | Type | Note |
|-|-|-|-|-|
| StringBase64ItemStackTransformer | java.lang.String | org.bukkit.inventory.ItemStack | Two-side | Available as ItemStackSerializer mode override (failsafe). Base64 encodes/decodes ItemStack using BukkitObject streams, stability between versions highly depends on the underlying server-side implementation and has not been determined. Intended use is storage-only. See class javadocs for more details. |
