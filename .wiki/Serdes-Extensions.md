# Serdes Extensions

Serdes extensions add support for additional types beyond the built-in ones. Each extension provides serializers, deserializers, and transformers for specific libraries or platforms.

## Table of Contents

- [What are Serdes Extensions?](#what-are-serdes-extensions)
- [How to Use](#how-to-use)
- [Available Extensions](#available-extensions)
- [SerdesCommons](#serdescommons)
- [SerdesBukkit](#serdesbukkit)
- [SerdesBungee](#serdesbungee)
- [SerdesAdventure](#serdesadventure)
- [SerdesBucket4j](#serdesbucket4j)
- [SerdesOkaeri](#serdesokaeri)
- [SerdesOkaeriBukkit](#serdesokaeri-bukkit)

## What are Serdes Extensions?

**Serdes** = **Ser**ializers + **Des**erializers

Extensions add support for types that aren't part of Java's standard library. For example:
- `java.time.Instant` (needs serdes-commons)
- `org.bukkit.Location` (needs serdes-bukkit)
- `net.kyori.adventure.text.Component` (needs serdes-adventure)

Without the appropriate extension, these types cannot be saved/loaded in configs.

## How to Use

Add the extension when creating your config:

```java
import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;
import eu.okaeri.configs.serdes.commons.SerdesCommons;

MyConfig config = ConfigManager.create(MyConfig.class, (it) -> {
    it.configure(opt -> {
        // Add serdes extension(s) after the configurer
        opt.configurer(new YamlSnakeYamlConfigurer(), new SerdesCommons());
        opt.bindFile("config.yml");
    });
    it.saveDefaults();
    it.load(true); // load and save to update comments/new fields
});
```

### Multiple Extensions

You can use multiple extensions together:

```java
it.configure(opt -> {
    opt.configurer(
        new YamlSnakeYamlConfigurer(),
        new SerdesCommons(),  // For Duration, Instant, etc.
        new SerdesBukkit()    // For Location, ItemStack, etc.
    );
});
```

## Available Extensions

| Extension | Purpose | Common Types |
|-----------|---------|--------------|
| [SerdesCommons](#serdescommons) | Common Java types | `Instant`, `Duration`, `Locale`, `Pattern` |
| [SerdesBukkit](#serdesbukkit) | Bukkit/Spigot types | `Location`, `ItemStack`, `PotionEffect` |
| [SerdesBungee](#serdesbungee) | BungeeCord types | `ChatColor` |
| [SerdesAdventure](#serdesadventure) | Kyori Adventure | `Component`, `TextColor` |
| [SerdesBucket4j](#serdesbucket4j) | Rate limiting | `LocalBucket`, `Bandwidth` |
| [SerdesOkaeri](#serdesokaeri) | Okaeri Commons | `IndexedSet`, `RomanNumeral` |
| [SerdesOkaeriBukkit](#serdesokaeri-bukkit) | Okaeri Commons Bukkit | `TagMaterialSet` |

## SerdesCommons

Common Java types not included in the core library.

### Installation

**Maven:**
```xml
<dependency>
    <groupId>eu.okaeri</groupId>
    <artifactId>okaeri-configs-serdes-commons</artifactId>
    <version>{VERSION}</version>
</dependency>
```

**Gradle (Kotlin DSL):**
```kotlin
implementation("eu.okaeri:okaeri-configs-serdes-commons:{VERSION}")
```

### Usage

```java
it.configure(opt -> {
    opt.configurer(new YamlSnakeYamlConfigurer(), new SerdesCommons());
});
```

### Supported Types

| Type | Description | Example |
|------|-------------|---------|
| `java.time.Instant` | Timestamp | `2024-01-01T12:00:00Z` |
| `java.time.Duration` | Time duration | `PT1H30M` (1 hour 30 minutes) |
| `java.util.Locale` | Locale/language | `en_US`, `pl_PL` |
| `java.util.regex.Pattern` | Regular expression | `[a-z]+` |

### Example

```java
import java.time.Instant;
import java.time.Duration;
import java.util.Locale;
import java.util.regex.Pattern;

@Getter
@Setter
public class TimedConfig extends OkaeriConfig {

    @Comment("Server start time")
    private Instant startTime = Instant.now();

    @Comment("Session timeout")
    private Duration sessionTimeout = Duration.ofMinutes(30);

    @Comment("Default locale")
    private Locale defaultLocale = Locale.US;

    @Comment("Username pattern")
    private Pattern usernamePattern = Pattern.compile("[a-zA-Z0-9_]{3,16}");
}
```

**Output (YAML):**
```yaml
# Server start time
startTime: '2024-01-01T12:00:00Z'

# Session timeout
sessionTimeout: PT30M

# Default locale
defaultLocale: en_US

# Username pattern
usernamePattern: '[a-zA-Z0-9_]{3,16}'
```

## SerdesBukkit

Serializers for Minecraft Bukkit/Spigot/Paper types including `ItemStack`, `Location`, `PotionEffect`, `Vector`, and more.

**See the dedicated guide:** **[Platform Integration - Bukkit](Platform-Bukkit)** for:
- Complete installation and setup instructions
- Detailed ItemStack serialization (@ItemStackSpec, formats, failsafe modes)
- Location, Vector, and PotionEffect usage
- Bukkit-specific transformers (Enchantment, World, Biome, Tag)
- Complete plugin examples and best practices

### Quick Example

```java
import eu.okaeri.configs.yaml.bukkit.YamlBukkitConfigurer;
import eu.okaeri.configs.yaml.bukkit.serdes.SerdesBukkit;

config = ConfigManager.create(PluginConfig.class, (it) -> {
    it.configure(opt -> {
        opt.configurer(new YamlBukkitConfigurer(), new SerdesBukkit());
        opt.bindFile(new File(getDataFolder(), "config.yml"));
    });
    it.load(true); // load and save to update comments/new fields
});
```

## SerdesBungee

Serializers for BungeeCord/Waterfall types.

### Installation

**Maven:**
```xml
<dependency>
    <groupId>eu.okaeri</groupId>
    <artifactId>okaeri-configs-serdes-bungee</artifactId>
    <version>{VERSION}</version>
</dependency>
```

### Usage

```java
it.configure(opt -> {
    opt.configurer(new YamlBungeeConfigurer(), new SerdesBungee());
});
```

### Supported Types

| Type | Description |
|------|-------------|
| `net.md_5.bungee.api.ChatColor` | Chat color codes |

### Example

```java
import net.md_5.bungee.api.ChatColor;

@Getter
@Setter
public class ProxyConfig extends OkaeriConfig {

    @Comment("Prefix color")
    private ChatColor prefixColor = ChatColor.GOLD;

    @Comment("Error color")
    private ChatColor errorColor = ChatColor.RED;
}
```

## SerdesAdventure

Serializers for Kyori Adventure text components.

### Installation

**Maven:**
```xml
<dependency>
    <groupId>eu.okaeri</groupId>
    <artifactId>okaeri-configs-serdes-adventure</artifactId>
    <version>{VERSION}</version>
</dependency>
```

### Usage

```java
it.configure(opt -> {
    opt.configurer(new YamlSnakeYamlConfigurer(), new SerdesAdventure());
});
```

### Supported Types

| Type | Description |
|------|-------------|
| `net.kyori.adventure.text.Component` | Rich text components |
| `net.kyori.adventure.text.format.TextColor` | RGB colors |
| `net.kyori.adventure.text.format.NamedTextColor` | Named colors |

### Example

```java
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

@Getter
@Setter
public class MessagesConfig extends OkaeriConfig {

    @Comment("Welcome message")
    private Component welcomeMessage = Component.text("Welcome!", NamedTextColor.GOLD);

    @Comment("Prefix color")
    private TextColor prefixColor = TextColor.color(0xFF5555);
}
```

## SerdesBucket4j

Serializers for Bucket4j rate limiting.

### Installation

**Maven:**
```xml
<dependency>
    <groupId>eu.okaeri</groupId>
    <artifactId>okaeri-configs-serdes-bucket4j</artifactId>
    <version>{VERSION}</version>
</dependency>
```

> ⚠️ **Requires**: SerdesCommons (or another `Duration` transformer provider)

### Usage

```java
it.configure(opt -> {
    opt.configurer(
        new YamlSnakeYamlConfigurer(),
        new SerdesCommons(),   // Required for Duration support
        new SerdesBucket4j()
    );
});
```

### Supported Types

| Type | Properties | Description |
|------|------------|-------------|
| `LocalBucket` | bandwidths | Rate limit bucket |
| `Bandwidth` | capacity, refill-period, refill-tokens | Bandwidth configuration |
| `SingleBandwidthBucket` | capacity, refill-period, refill-tokens | Simplified wrapper |

### Example

```java
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.local.LocalBucket;
import java.time.Duration;

@Getter
@Setter
public class RateLimitConfig extends OkaeriConfig {

    @Comment("API rate limit (10 requests per minute)")
    private Bandwidth apiLimit = Bandwidth.simple(10, Duration.ofMinutes(1));
}
```

**Output (YAML):**
```yaml
# API rate limit (10 requests per minute)
apiLimit:
  capacity: 10
  refill-period: PT1M
  refill-tokens: 10
```

## SerdesOkaeri

Serializers for Okaeri Commons types.

### Installation

**Maven:**
```xml
<dependency>
    <groupId>eu.okaeri</groupId>
    <artifactId>okaeri-configs-serdes-okaeri</artifactId>
    <version>{VERSION}</version>
</dependency>
```

### Usage

```java
it.configure(opt -> {
    opt.configurer(new YamlSnakeYamlConfigurer(), new SerdesOkaeri());
});
```

### Supported Types

| Type | Description |
|------|-------------|
| `eu.okaeri.commons.indexedset.IndexedSet` | Indexed set collection |
| `eu.okaeri.commons.RomanNumeral` | Roman numeral conversion |

## SerdesOkaeriBukkit

Serializers for Okaeri Commons Bukkit types.

### Installation

**Maven:**
```xml
<dependency>
    <groupId>eu.okaeri</groupId>
    <artifactId>okaeri-configs-serdes-okaeri-bukkit</artifactId>
    <version>{VERSION}</version>
</dependency>
```

### Usage

```java
it.configure(opt -> {
    opt.configurer(new YamlBukkitConfigurer(), new SerdesOkaeriBukkit());
});
```

### Supported Types

| Type | Properties | Description |
|------|------------|-------------|
| `eu.okaeri.commons.bukkit.material.TagMaterialSet` | tags, materials | Material sets with tag support |

## Next Steps

- **[Advanced Topics](Advanced-Topics)** - Creating custom serializers
- **[Supported Types](Supported-Types)** - Built-in types reference
- **[Platform Integration](Platform-Bukkit)** - Platform-specific guides

## See Also

- **[Subconfigs & Serialization](Subconfigs-and-Serialization)** - Custom serializable objects
- **[Examples & Recipes](Examples-and-Recipes)** - Real-world examples
