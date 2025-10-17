# Advanced Topics

This guide covers advanced features for extending okaeri-configs with custom serialization logic, transformers, and post-processors.

## Table of Contents

- [Overview](#overview)
- [Custom Serializers](#custom-serializers)
- [Custom Transformers](#custom-transformers)
- [ConfigSerializable Interface](#configserializable-interface)
- [Creating Serdes Packs](#creating-serdes-packs)
- [Post-processors](#post-processors)
- [SerdesRegistry Operations](#serdesregistry-operations)

## Overview

Okaeri Configs provides multiple extension points for handling custom types:

| Extension Type | Use Case | Complexity |
|---------------|----------|------------|
| **ConfigSerializable** | Class-local serdes, auto-registered | Low |
| **ObjectTransformer** | Simple type conversions (A → B, faster) | Low |
| **ObjectSerializer** | Multi-field types, custom logic | Medium |
| **OkaeriSerdesPack** | Grouping multiple serializers/transformers | Medium |
| **ConfigPostprocessor** | Custom file format manipulation | High |

## Custom Serializers

### When to Use

Use `ObjectSerializer` for types that need custom serialization logic.

**Use serializers for:**
- Multi-field types (Location with x, y, z, world, yaw, pitch)
- Platform-specific types (Bukkit ItemStack, PotionEffect)
- Complex types requiring custom logic
- Simple types (can use `setValue()` for single-value serialization)

**Prefer transformers for simple conversions:**
- Transformers use **hashed matching** (faster lookup)
- Serializers use **iterative matching** (slower for many types)
- For basic type conversions (String ↔ Integer, String ↔ Locale), transformers are more efficient

**Prefer Serializable/subconfigs for:**
- Your own POJOs → use `Serializable` or subconfigs

### Creating a Serializer

Implement `ObjectSerializer<T>`:

```java
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.*;
import lombok.NonNull;

public class CoordinateSerializer implements ObjectSerializer<Coordinate> {

    @Override
    public boolean supports(@NonNull Class<?> type) {
        return Coordinate.class.isAssignableFrom(type);
    }

    @Override
    public void serialize(@NonNull Coordinate object, @NonNull SerializationData data, @NonNull GenericsDeclaration generics) {
        data.add("x", object.getX());
        data.add("y", object.getY());
        data.add("z", object.getZ());
    }

    @Override
    public Coordinate deserialize(@NonNull DeserializationData data, @NonNull GenericsDeclaration generics) {
        double x = data.get("x", Double.class);
        double y = data.get("y", Double.class);
        double z = data.get("z", Double.class);
        return new Coordinate(x, y, z);
    }
}
```

### Registering a Serializer

```java
MyConfig config = ConfigManager.create(MyConfig.class, (it) -> {
    it.withConfigurer(new YamlSnakeYamlConfigurer(), registry -> {
        registry.register(new CoordinateSerializer());
    });
    it.withBindFile("config.yml");
    it.load();
});
```

**Alternative** (if you already have a configurer instance):

```java
MyConfig config = new MyConfig();
config.withConfigurer(new YamlSnakeYamlConfigurer());
config.withSerdesPack(registry -> {
    registry.register(new CoordinateSerializer());
});
config.withBindFile("config.yml");
config.load();
```

### SerializationData Methods

| Method | Description | Example |
|--------|-------------|---------|
| `add(key, value)` | Add simple value | `data.add("x", 10.5)` |
| `add(key, value, type)` | Add typed value | `data.add("world", world, World.class)` |
| `setValue(value)` | Set single value (no key) | `data.setValue("value")` |
| `addCollection(key, list, type)` | Add collection | `data.addCollection("coords", list, Coordinate.class)` |

### DeserializationData Methods

| Method | Description | Example |
|--------|-------------|---------|
| `get(key, type)` | Get typed value | `data.get("x", Double.class)` |
| `getValue(type)` | Get single value | `data.getValue(String.class)` |
| `getAsList(key, type)` | Get typed list | `data.getAsList("coords", Coordinate.class)` |
| `containsKey(key)` | Check if key exists | `data.containsKey("optional")` |

### Complex Serializer Example

Real-world example from serdes-bukkit:

```java
public class LocationSerializer implements ObjectSerializer<Location> {

    @Override
    public boolean supports(@NonNull Class<?> type) {
        return Location.class.isAssignableFrom(type);
    }

    @Override
    public void serialize(@NonNull Location location, @NonNull SerializationData data, @NonNull GenericsDeclaration generics) {
        data.add("world", location.getWorld(), World.class);
        data.add("x", location.getX());
        data.add("y", location.getY());
        data.add("z", location.getZ());
        data.add("yaw", location.getYaw());
        data.add("pitch", location.getPitch());
    }

    @Override
    public Location deserialize(@NonNull DeserializationData data, @NonNull GenericsDeclaration generics) {
        World world = data.get("world", World.class);
        double x = data.get("x", Double.class);
        double y = data.get("y", Double.class);
        double z = data.get("z", Double.class);
        float yaw = data.get("yaw", Float.class);
        float pitch = data.get("pitch", Float.class);

        return new Location(world, x, y, z, yaw, pitch);
    }
}
```

**Output (YAML):**
```yaml
location:
  world: world
  x: 100.5
  y: 64.0
  z: -50.25
  yaw: 90.0
  pitch: 0.0
```

## Custom Transformers

### When to Use

Use `ObjectTransformer` for **simple type conversions** (A → B) with no intermediate structure.

**Use transformers for:**
- Basic type conversions (String ↔ Integer, String ↔ Enum)
- String ↔ Locale, UUID, Pattern
- Custom ↔ String conversions for simple types

**Why transformers for simple types?**
- **Performance**: Transformers use hashed matching (fast lookup by type pair)
- **Simplicity**: Direct A → B conversion without intermediate data structures
- Serializers use iterative matching (slower when many serializers registered)

**When to use serializers instead:**
- Multi-field types → use `ObjectSerializer`
- Complex serialization logic → use `ObjectSerializer`

### Creating a Transformer

Implement `ObjectTransformer<From, To>`:

```java
import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.*;
import lombok.NonNull;
import java.util.Locale;

public class LocaleTransformer extends ObjectTransformer<String, Locale> {

    @Override
    public GenericsPair<String, Locale> getPair() {
        return this.genericsPair(String.class, Locale.class);
    }

    @Override
    public Locale transform(@NonNull String data, @NonNull SerdesContext context) {
        return Locale.forLanguageTag(data.replace("_", "-"));
    }
}
```

### Bidirectional Transformer

For two-way conversions, use `BidirectionalTransformer`:

```java
import eu.okaeri.configs.serdes.BidirectionalTransformer;

public class ColorTransformer extends BidirectionalTransformer<String, Color> {

    @Override
    public GenericsPair<String, Color> getPair() {
        return this.genericsPair(String.class, Color.class);
    }

    @Override
    public Color leftToRight(@NonNull String data, @NonNull SerdesContext context) {
        // String → Color
        return Color.decode(data);
    }

    @Override
    public String rightToLeft(@NonNull Color data, @NonNull SerdesContext context) {
        // Color → String
        return String.format("#%06X", data.getRGB() & 0xFFFFFF);
    }
}
```

### Registering Transformers

```java
MyConfig config = ConfigManager.create(MyConfig.class, (it) -> {
    it.withConfigurer(new YamlSnakeYamlConfigurer(), registry -> {
        // One-way transformer
        registry.register(new LocaleTransformer());

        // Bidirectional transformer (registers both directions)
        registry.register(new ColorTransformer());
    });
    it.withBindFile("config.yml");
    it.load();
});
```

### Transformer with Reverse toString

For transformers that serialize via `.toString()`:

```java
config.withSerdesPack(registry -> {
    registry.registerWithReversedToString(new StringToIntegerTransformer());
    // Registers: String → Integer AND Integer → String (via toString)
});
```

## ConfigSerializable Interface

### When to Use

Use `ConfigSerializable` for **class-local serdes** - serialization logic lives in the class itself.

**Advantages:**
- No external serializer needed
- Self-contained serialization logic
- Works automatically (no registration)

**Use for:**
- Application-specific types you control
- Types where serialization is part of the domain logic

**Don't use for:**
- External library types (can't modify source)
- Types needing multiple serialization formats

### Implementation

Implement the interface and provide a static `deserialize` method:

```java
import eu.okaeri.configs.serdes.serializable.ConfigSerializable;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Player implements ConfigSerializable {
    private String name;
    private int level;
    private double experience;

    @Override
    public void serialize(@NonNull SerializationData data, @NonNull GenericsDeclaration generics) {
        data.add("name", this.name);
        data.add("level", this.level);
        data.add("experience", this.experience);
    }

    public static Player deserialize(@NonNull DeserializationData data, @NonNull GenericsDeclaration generics) {
        String name = data.get("name", String.class);
        int level = data.get("level", Integer.class);
        double experience = data.get("experience", Double.class);
        return new Player(name, level, experience);
    }
}
```

### Usage in Config

```java
@Getter
@Setter
public class GameConfig extends OkaeriConfig {

    private Player player = new Player();  // Works automatically!

    private List<Player> topPlayers = List.of(
        new Player("Alice", 50, 1250.0),
        new Player("Bob", 45, 980.5)
    );
}
```

**Output (YAML):**
```yaml
player:
  name: ''
  level: 0
  experience: 0.0

topPlayers:
  - name: Alice
    level: 50
    experience: 1250.0
  - name: Bob
    level: 45
    experience: 980.5
```

### Nested ConfigSerializable

ConfigSerializable objects can contain other ConfigSerializable objects:

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Guild implements ConfigSerializable {
    private String name;
    private Player leader;  // Nested ConfigSerializable

    @Override
    public void serialize(@NonNull SerializationData data, @NonNull GenericsDeclaration generics) {
        data.add("name", this.name);
        data.add("leader", this.leader, Player.class);
    }

    public static Guild deserialize(@NonNull DeserializationData data, @NonNull GenericsDeclaration generics) {
        String name = data.get("name", String.class);
        Player leader = data.get("leader", Player.class);
        return new Guild(name, leader);
    }
}
```

### Important Notes

**Required deserialize signature:**
```java
public static YourType deserialize(@NonNull DeserializationData data, @NonNull GenericsDeclaration generics)
```

**Common mistakes:**
```java
// ❌ WRONG - not static
public YourType deserialize(...)

// ❌ WRONG - wrong return type
public static Object deserialize(...)

// ❌ WRONG - missing parameters
public static YourType deserialize(DeserializationData data)
```

## Creating Serdes Packs

### When to Use

Use `OkaeriSerdesPack` to **group multiple serializers and transformers** into a reusable module.

**Use for:**
- Platform-specific type bundles (Bukkit, Bungee)
- Library integrations (Bucket4j, Adventure)
- Organizational purposes (group related serializers)

### Creating a Pack

Implement `OkaeriSerdesPack`:

```java
import eu.okaeri.configs.serdes.*;
import lombok.NonNull;

public class MyCustomSerdes implements OkaeriSerdesPack {

    @Override
    public void register(@NonNull SerdesRegistry registry) {
        // Register transformers
        registry.register(new LocaleTransformer());
        registry.register(new ColorTransformer());

        // Register serializers
        registry.register(new CoordinateSerializer());
        registry.register(new PlayerSerializer());
    }
}
```

### Using a Pack

```java
MyConfig config = ConfigManager.create(MyConfig.class, (it) -> {
    it.withConfigurer(
        new YamlSnakeYamlConfigurer(),
        new SerdesCommons(),     // Built-in pack
        new MyCustomSerdes()     // Your custom pack
    );
    it.withBindFile("config.yml");
    it.load();
});
```

### Real Example: SerdesCommons

From the library source:

```java
public class SerdesCommons implements OkaeriSerdesPack {

    @Override
    public void register(@NonNull SerdesRegistry registry) {
        registry.register(new DurationTransformer());
        registry.register(new DurationAttachmentResolver());

        registry.register(new InstantSerializer(false));

        registry.register(new LocaleTransformer());
        registry.register(new PatternTransformer());
    }
}
```

## Post-processors

### What are Post-processors?

`ConfigPostprocessor` allows **low-level manipulation** of config file content after it's written or before it's read.

**Use cases:**
- Custom comment formatting
- Line filtering/removal
- Key manipulation
- Custom file structure modifications

### Basic Usage

```java
import eu.okaeri.configs.postprocessor.ConfigPostprocessor;
import java.io.*;

// Read from file
ConfigPostprocessor processor = ConfigPostprocessor.of(new FileInputStream("config.yml"));

// Modify content
processor
    .removeLines(line -> line.trim().isEmpty())  // Remove empty lines
    .updateLines(line -> line.replace("old", "new"))  // Replace text
    .prependContextComment("# ", new String[]{"Custom header", "Generated by MyApp"})
    .write(new FileOutputStream("config.yml"));
```

### Line Manipulation

```java
// Remove lines matching condition
processor.removeLines(line -> line.contains("DEBUG"));

// Remove lines until condition met
processor.removeLinesUntil(line -> line.startsWith("main:"));

// Update each line
processor.updateLines(line -> {
    if (line.startsWith("password:")) {
        return "password: [REDACTED]";
    }
    return line;
});
```

### Context Manipulation

```java
// Modify entire file content
processor.updateContext(content -> {
    return content
        .replace("old_version", "new_version")
        .replaceAll("(?m)^#.*$", "");  // Remove all comments
});
```

### Comment Management

```java
// Prepend header
processor.prependContextComment("# ", new String[]{
    "================================",
    "  Application Configuration",
    "================================"
});

// Append footer
processor.appendContextComment("# ", new String[]{
    "End of configuration"
});
```

### Key Walking (Advanced)

Use `ConfigSectionWalker` to manipulate config keys:

```java
import eu.okaeri.configs.postprocessor.format.YamlSectionWalker;

processor.updateLinesKeys(new YamlSectionWalker() {
    @Override
    public String update(String line, ConfigLineInfo current, List<ConfigLineInfo> path) {
        // Rename specific keys
        if (current.getName().equals("oldKey")) {
            return line.replace("oldKey:", "newKey:");
        }
        return line;
    }
});
```

## SerdesRegistry Operations

### Registration Methods

**Preferred approach** (using withConfigurer/withSerdesPack):

```java
config.withConfigurer(new YamlSnakeYamlConfigurer(), registry -> {
    // Standard registration (last wins)
    registry.register(new MySerializer());

    // Register first (high priority)
    registry.registerFirst(new HighPrioritySerializer());

    // Exclusive registration (removes others for this type)
    registry.registerExclusive(MyType.class, new ExclusiveSerializer());
});
```

**Direct access** (when needed):

```java
SerdesRegistry registry = config.getConfigurer().getRegistry();
registry.register(new MySerializer());
```

### Checking for Support

```java
// Get serializer for type
ObjectSerializer<?> serializer = registry.getSerializer(MyType.class);

// Check if transformation is possible
boolean canTransform = registry.canTransform(
    GenericsDeclaration.of(String.class),
    GenericsDeclaration.of(Integer.class)
);

// Get transformer
ObjectTransformer transformer = registry.getTransformer(
    GenericsDeclaration.of(String.class),
    GenericsDeclaration.of(Locale.class)
);
```

### Serializer Precedence

**Last registered wins:**
```java
registry.register(new MySerializer1());  // ← Will be checked second
registry.register(new MySerializer2());  // ← Will be checked first
```

**First registration (highest priority):**
```java
registry.registerFirst(new HighPrioritySerializer());  // ← Checked first
registry.register(new NormalSerializer());             // ← Checked second
```

**Exclusive registration:**
```java
// Remove all serializers for ItemStack, register only this one
registry.registerExclusive(ItemStack.class, new CustomItemStackSerializer());
```

## Best Practices

### ✅ Do's

1. **Use the right abstraction:**
   ```java
   // ✅ Simple conversion
   new StringToLocaleTransformer()

   // ✅ Complex multi-field type
   new LocationSerializer()

   // ✅ Your own POJOs
   implements ConfigSerializable
   ```

2. **Group related serializers:**
   ```java
   public class MyGameSerdes implements OkaeriSerdesPack {
       // All game-related serializers in one place
   }
   ```

3. **Check for null/optional values:**
   ```java
   @Override
   public void serialize(Location loc, SerializationData data, GenericsDeclaration generics) {
       if (loc.getWorld() != null) {
           data.add("world", loc.getWorld(), World.class);
       }
   }
   ```

### ❌ Don'ts

1. **Prefer transformers for basic type conversions (performance):**
   ```java
   // ⚠️ Works but slower (iterative matching)
   public class LocaleSerializer implements ObjectSerializer<Locale> {
       public void serialize(Locale locale, SerializationData data, GenericsDeclaration generics) {
           data.setValue(locale.toLanguageTag());
       }
       public Locale deserialize(DeserializationData data, GenericsDeclaration generics) {
           return Locale.forLanguageTag(data.getValue(String.class));
       }
   }

   // ✅ Better - faster (hashed matching, shown earlier in guide)
   public class LocaleTransformer extends ObjectTransformer<String, Locale>
   ```

2. **Don't forget the static deserialize method:**
   ```java
   // ❌ Will fail at runtime
   public class Player implements ConfigSerializable {
       public void serialize(...) { }
       // Missing: public static Player deserialize(...)
   }
   ```

3. **Don't register serdes after config is created:**
   ```java
   // ❌ Too late - config already initialized
   MyConfig config = ConfigManager.create(...);
   config.withSerdesPack(registry -> {
       registry.register(new MySerializer());
   });

   // ✅ Register during config creation
   MyConfig config = ConfigManager.create(MyConfig.class, (it) -> {
       it.withConfigurer(new YamlSnakeYamlConfigurer(), registry -> {
           registry.register(new MySerializer());
       });
       it.withBindFile("config.yml");
       it.load();
   });
   ```

## Next Steps

- **[Serdes Extensions](Serdes-Extensions)** - Pre-built serializers for common types
- **[Subconfigs & Serialization](Subconfigs-and-Serialization)** - Understanding serialization basics
- **[Examples & Recipes](Examples-and-Recipes)** - Real-world examples

## See Also

- **[Configuration Basics](Configuration-Basics)** - Understanding core concepts
- **[Validation](Validation)** - Adding validation to custom types
