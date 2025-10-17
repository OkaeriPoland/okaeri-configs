# Platform Integration - Bukkit

Complete guide to using okaeri-configs with Minecraft Bukkit/Spigot/Paper plugins.

## Table of Contents

- [Installation](#installation)
- [Setup](#setup)
- [Supported Types](#supported-types)
- [ItemStack Serialization](#itemstack-serialization)
- [Location Serialization](#location-serialization)
- [Other Bukkit Types](#other-bukkit-types)
- [Complete Example](#complete-example)
- [Best Practices](#best-practices)

## Installation

**Maven:**
```xml
<!-- Bukkit YAML configurer -->
<dependency>
    <groupId>eu.okaeri</groupId>
    <artifactId>okaeri-configs-yaml-bukkit</artifactId>
    <version>{VERSION}</version>
</dependency>

<!-- Bukkit serdes (serialization for Bukkit types) -->
<dependency>
    <groupId>eu.okaeri</groupId>
    <artifactId>okaeri-configs-serdes-bukkit</artifactId>
    <version>{VERSION}</version>
</dependency>
```

**Gradle (Kotlin DSL):**
```kotlin
implementation("eu.okaeri:okaeri-configs-yaml-bukkit:{VERSION}")
implementation("eu.okaeri:okaeri-configs-serdes-bukkit:{VERSION}")
```

**Gradle (Groovy DSL):**
```groovy
implementation 'eu.okaeri:okaeri-configs-yaml-bukkit:{VERSION}'
implementation 'eu.okaeri:okaeri-configs-serdes-bukkit:{VERSION}'
```

## Setup

### Basic Plugin Configuration

```java
import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.yaml.bukkit.YamlBukkitConfigurer;
import eu.okaeri.configs.yaml.bukkit.serdes.SerdesBukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class MyPlugin extends JavaPlugin {

    private PluginConfig config;

    @Override
    public void onEnable() {
        this.config = ConfigManager.create(PluginConfig.class, (it) -> {
            it.withConfigurer(new YamlBukkitConfigurer(), new SerdesBukkit());
            it.withBindFile(new File(getDataFolder(), "config.yml"));
            it.withRemoveOrphans(true);
            it.saveDefaults();
            it.load(true);
        });

        getLogger().info("Config loaded successfully!");
    }
}
```

## Supported Types

The `SerdesBukkit` pack provides serialization support for the following Bukkit types:

### Core Types

| Type | Description | Serializer/Transformer |
|------|-------------|----------------------|
| `ItemStack` | Minecraft items with metadata | `ItemStackSerializer` |
| `ItemMeta` | Item metadata (display name, lore, enchants) | `ItemMetaSerializer` |
| `Location` | World coordinates with yaw/pitch | `LocationSerializer` |
| `Vector` | 3D vector (x, y, z) | `VectorSerializer` |
| `PotionEffect` | Potion effects with duration/amplifier | `PotionEffectSerializer` |
| `World` | World references (by name) | `StringWorldTransformer` |

### Enum-like Types (Transformers)

| Type | Description | Transformer |
|------|-------------|------------|
| `Enchantment` | Item enchantments | `StringEnchantmentTransformer` |
| `PotionEffectType` | Potion effect types | `StringPotionEffectTypeTransformer` |
| `Biome` | World biomes (1.13+) | `StringBiomeTransformer` |
| `Tag` | Block/item tags (1.13+) | `StringTagTransformer` |

## ItemStack Serialization

The `ItemStackSerializer` is the most complex serializer in the Bukkit serdes pack, with multiple serialization formats and failsafe mechanisms.

### @ItemStackSpec Annotation

Control how ItemStacks are serialized using the `@ItemStackSpec` annotation:

```java
import eu.okaeri.configs.yaml.bukkit.serdes.itemstack.ItemStackSpec;
import eu.okaeri.configs.yaml.bukkit.serdes.itemstack.ItemStackFormat;
import org.bukkit.inventory.ItemStack;

public class ItemConfig extends OkaeriConfig {

    @Comment("Starter sword (NATURAL format - nested meta)")
    @ItemStackSpec(format = ItemStackFormat.NATURAL)
    private ItemStack starterSword = new ItemStack(Material.DIAMOND_SWORD);

    @Comment("Reward item (COMPACT format - flat structure)")
    @ItemStackSpec(format = ItemStackFormat.COMPACT)
    private ItemStack rewardItem = new ItemStack(Material.EMERALD);
}
```

### ItemStack Formats

#### NATURAL Format (Default)

Nested structure with separate `meta` section. More readable for complex items.

**Config output:**
```yaml
starterSword:
  material: DIAMOND_SWORD
  amount: 1
  meta:
    display-name: "&6Starter Sword"
    lore:
      - "&7A basic sword"
      - "&7Given to new players"
    enchants:
      DAMAGE_ALL: 2
      DURABILITY: 1
```

#### COMPACT Format

Flattened structure with meta fields at the same level. More compact for simple items.

**Config output:**
```yaml
rewardItem:
  material: EMERALD
  amount: 5
  display-name: "&aReward Emerald"
  lore:
    - "&7Quest completion reward"
```

### ItemStack Failsafe Modes

The serializer includes automatic failsafe mechanisms when human-readable serialization might lose data:

```java
import eu.okaeri.configs.yaml.bukkit.serdes.itemstack.ItemStackFailsafe;
import eu.okaeri.configs.yaml.bukkit.serdes.serializer.ItemStackSerializer;

// Register with failsafe mode
config.withSerdesPack(registry -> {
    registry.registerExclusive(ItemStack.class,
        new ItemStackSerializer(ItemStackFailsafe.BUKKIT));
});
```

**Available failsafe modes:**

| Mode | Behavior | Use Case |
|------|----------|----------|
| `NONE` | No failsafe, use human-readable format only | Simple items, editing by hand |
| `BASE64` | Falls back to base64 encoding if data loss detected | Legacy compatibility |
| `BUKKIT` | Falls back to Bukkit's native YAML serialization | Complex items with custom NBT |

**How failsafe works:**

1. Serializer attempts human-readable format (NATURAL or COMPACT)
2. Deserializes the data to verify no information was lost
3. If deserialized ItemStack differs from original, falls back to specified failsafe
4. Uses failsafe format (BASE64 or BUKKIT serialization)

**Example with complex item:**

```yaml
# Human-readable format worked - no custom NBT
simpleItem:
  material: DIAMOND_SWORD
  enchants:
    DAMAGE_ALL: 5

# Failsafe triggered - has complex NBT data
complexItem:
  ==: org.bukkit.inventory.ItemStack
  v: 3700
  type: PLAYER_HEAD
  meta:
    ==: ItemMeta
    meta-type: SKULL
    # ... Bukkit's native serialization
```

### ItemStack Serialization Details

The `ItemStackSerializer` handles several special cases:

**1. Amount field (optional)**
```yaml
# Amount defaults to 1, not written when 1
diamonds:
  material: DIAMOND
  amount: 64  # Only written when not 1
```

**2. Durability field (optional)**
```yaml
# Durability only written when != 0
# IMPORTANT: On 1.21+, durability:0 is different from no durability
damagedSword:
  material: DIAMOND_SWORD
  durability: 100  # Damaged item
```

**3. Meta application order (critical for 1.16+)**

The serializer applies ItemMeta **before** durability:

```java
ItemStack itemStack = new ItemStack(material, amount);
itemStack.setItemMeta(itemMeta);  // Applied FIRST
if (durability != 0) {
    itemStack.setDurability(durability);  // Applied SECOND
}
```

This prevents data loss on 1.16+ where ItemMeta contains additional attributes.

**4. Format conversion**

The serializer automatically converts between formats when loading:

- COMPACT → NATURAL: Detects flat `display` or `display-name` fields
- NATURAL → COMPACT: Detects nested `meta` field

### ItemStack Code Example

```java
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.yaml.bukkit.serdes.itemstack.ItemStackSpec;
import eu.okaeri.configs.yaml.bukkit.serdes.itemstack.ItemStackFormat;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class ItemsConfig extends OkaeriConfig {

    @Comment("Starter kit items")
    @ItemStackSpec(format = ItemStackFormat.NATURAL)
    private List<ItemStack> starterKit = new ArrayList<>();

    public ItemsConfig() {
        // Create example items
        ItemStack sword = new ItemStack(Material.IRON_SWORD);
        ItemMeta swordMeta = sword.getItemMeta();
        swordMeta.setDisplayName("§6Starter Sword");
        swordMeta.setLore(Arrays.asList("§7Basic weapon", "§7for new players"));
        swordMeta.addEnchant(Enchantment.DAMAGE_ALL, 1, true);
        sword.setItemMeta(swordMeta);

        starterKit.add(sword);
        starterKit.add(new ItemStack(Material.BREAD, 16));
    }
}
```

## Location Serialization

Locations serialize to a clean, readable format:

```java
private Location spawnLocation;
```

**Config output:**
```yaml
spawnLocation:
  world: world
  x: 100.5
  y: 64.0
  z: -50.25
  yaw: 90.0
  pitch: 0.0
```

**Complete example:**

```java
import org.bukkit.Location;

public class LocationConfig extends OkaeriConfig {

    @Comment("Player spawn point")
    private Location spawn;

    @Comment("Arena waypoints")
    private Map<String, Location> waypoints = new LinkedHashMap<>();

    public LocationConfig() {
        waypoints.put("red-base", null);  // Will be set in-game
        waypoints.put("blue-base", null);
        waypoints.put("center", null);
    }
}
```

## Other Bukkit Types

### Vector

```java
import org.bukkit.util.Vector;

private Vector velocity = new Vector(0, 1, 0);
```

**Config output:**
```yaml
velocity:
  x: 0.0
  y: 1.0
  z: 0.0
```

### PotionEffect

```java
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

private List<PotionEffect> effects = List.of(
    new PotionEffect(PotionEffectType.SPEED, 200, 1),
    new PotionEffect(PotionEffectType.JUMP, 200, 0)
);
```

**Config output:**
```yaml
effects:
  - type: SPEED
    duration: 200
    amplifier: 1
    ambient: true
    particles: true
  - type: JUMP
    duration: 200
    amplifier: 0
    ambient: true
    particles: true
```

### Enchantment (Transformer)

```java
import org.bukkit.enchantments.Enchantment;

private Map<Enchantment, Integer> enchants = Map.of(
    Enchantment.DAMAGE_ALL, 5,
    Enchantment.DURABILITY, 3
);
```

**Config output:**
```yaml
enchants:
  DAMAGE_ALL: 5
  DURABILITY: 3
```

### World (Transformer)

Worlds are serialized by name (String):

```java
import org.bukkit.World;

private World defaultWorld;  // Serialized as world name
```

**Config output:**
```yaml
defaultWorld: world
```

## Complete Example

Full plugin config using various Bukkit types:

```java
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.*;
import eu.okaeri.configs.yaml.bukkit.serdes.itemstack.ItemStackSpec;
import eu.okaeri.configs.yaml.bukkit.serdes.itemstack.ItemStackFormat;
import lombok.*;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@Header("################################")
@Header("#   My Plugin Configuration    #")
@Header("################################")
@Getter
@Setter
public class PluginConfig extends OkaeriConfig {

    @Comment("Plugin settings")
    private PluginSettings plugin = new PluginSettings();

    @Comment("Spawn configuration")
    private SpawnConfig spawn = new SpawnConfig();

    @Comment("Starter items for new players")
    private ItemsConfig items = new ItemsConfig();

    @Getter
    @Setter
    public static class PluginSettings extends OkaeriConfig {
        private String serverName = "My Server";
        private boolean debugMode = false;
        private String language = "en";
    }

    @Getter
    @Setter
    public static class SpawnConfig extends OkaeriConfig {

        @Comment("Spawn location")
        private Location location;

        @Comment("Teleport to spawn on join")
        private boolean teleportOnJoin = true;

        @Comment("Potion effects applied at spawn")
        private List<PotionEffect> effects = List.of(
            new PotionEffect(PotionEffectType.REGENERATION, 100, 1),
            new PotionEffect(PotionEffectType.SATURATION, 100, 0)
        );
    }

    @Getter
    @Setter
    public static class ItemsConfig extends OkaeriConfig {

        @Comment("Starter weapon (detailed format)")
        @ItemStackSpec(format = ItemStackFormat.NATURAL)
        private ItemStack starterWeapon;

        @Comment("Welcome gift (compact format)")
        @ItemStackSpec(format = ItemStackFormat.COMPACT)
        private ItemStack welcomeGift;

        @Comment("Full starter kit")
        @ItemStackSpec(format = ItemStackFormat.NATURAL)
        private List<ItemStack> starterKit = new ArrayList<>();
    }
}
```

**Plugin initialization:**

```java
import org.bukkit.plugin.java.JavaPlugin;
import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.yaml.bukkit.YamlBukkitConfigurer;
import eu.okaeri.configs.yaml.bukkit.serdes.SerdesBukkit;

public class MyPlugin extends JavaPlugin {

    private PluginConfig config;

    @Override
    public void onEnable() {
        // Load configuration
        this.config = ConfigManager.create(PluginConfig.class, (it) -> {
            it.withConfigurer(new YamlBukkitConfigurer(), new SerdesBukkit());
            it.withBindFile(new File(getDataFolder(), "config.yml"));
            it.withRemoveOrphans(true);
            it.saveDefaults();
            it.load(true);
        });

        // Use configuration
        getLogger().info("Loaded config for: " + config.getPlugin().getServerName());

        if (config.getSpawn().isTeleportOnJoin()) {
            getLogger().info("Spawn teleportation enabled");
        }
    }

    public PluginConfig getPluginConfig() {
        return config;
    }
}
```

## Best Practices

### ✅ Do's

1. **Use YamlBukkitConfigurer for Bukkit plugins:**
   ```java
   it.withConfigurer(new YamlBukkitConfigurer(), new SerdesBukkit());
   ```

2. **Use failsafe for items with custom NBT:**
   ```java
   registry.registerExclusive(ItemStack.class,
       new ItemStackSerializer(ItemStackFailsafe.BUKKIT));
   ```

3. **Store Worlds by name:**
   ```java
   private String worldName = "world";  // Better
   private World world;  // Works but serialized as name anyway
   ```

4. **Provide default Locations carefully:**
   ```java
   private Location spawn;  // null by default - set in-game
   ```

### ❌ Don'ts

1. **Don't use SnakeYAML configurer for Bukkit:**
   ```java
   // Wrong - Bukkit types won't serialize
   it.withConfigurer(new YamlSnakeYamlConfigurer());

   // Correct
   it.withConfigurer(new YamlBukkitConfigurer(), new SerdesBukkit());
   ```

2. **Don't forget SerdesBukkit:**
   ```java
   // Wrong - Bukkit types not registered
   it.withConfigurer(new YamlBukkitConfigurer());

   // Correct
   it.withConfigurer(new YamlBukkitConfigurer(), new SerdesBukkit());
   ```

3. **Don't manually set durability:0 on 1.21+:**
   ```yaml
   # Wrong - creates non-stacking items on 1.21+
   item:
     material: DIRT
     durability: 0

   # Correct - omit durability when 0
   item:
     material: DIRT
   ```

4. **Don't store complex objects in Locations:**
   ```java
   // Wrong - World is serialized as name, loses reference
   Location loc = player.getLocation();
   // Later: loc.getWorld() returns null if world unloaded

   // Correct - Store world name separately if needed
   private String worldName;
   private double x, y, z, yaw, pitch;
   ```

## Version Compatibility

- **Bukkit/Spigot/Paper:** 1.8.8+
- **ItemStack serialization:** Full support for 1.8.8 - 1.21+
- **Biome transformer:** Requires 1.13+ (when Biome became non-enum)
- **Tag transformer:** Requires 1.13+ (when Tag API was added)
- **ItemStack durability:** Special handling for 1.21+ stacking behavior

## Next Steps

- **[Serdes Extensions](Serdes-Extensions)** - Other serdes packs (Bungee, Commons, etc.)
- **[Examples & Recipes](Examples-and-Recipes)** - Complete plugin configurations
- **[Validation](Validation)** - Add validation to your Bukkit configs

## See Also

- **[Getting Started](Getting-Started)** - General setup guide
- **[Configuration Basics](Configuration-Basics)** - Understanding OkaeriConfig
- **[Supported Types](Supported-Types)** - Core type support
