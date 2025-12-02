# Subconfigs and Serialization

This guide explains how to organize complex configurations using subconfigs (nested `OkaeriConfig` classes) and serializable objects. Both approaches allow you to break down large configurations into manageable, reusable components.

## Table of Contents

- [Subconfigs (OkaeriConfig)](#subconfigs-okaericonfig)
- [Serializable Objects](#serializable-objects)
- [Comparison: Subconfigs vs Serializable](#comparison-subconfigs-vs-serializable)
- [Collections of Subconfigs/Serializables](#collections-of-subconfigsserializables)
- [Advanced: ConfigSerializable](#advanced-configserializable)
- [Best Practices](#best-practices)

## Subconfigs (OkaeriConfig)

Subconfigs are classes that extend `OkaeriConfig` and are used as fields in other configurations. They provide full config features including nested comments, validation, and all annotations.

### Basic Subconfig

```java
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppConfig extends OkaeriConfig {

    @Comment("Application settings")
    private String appName = "MyApp";

    @Comment("Database configuration")
    private DatabaseConfig database = new DatabaseConfig();

    @Comment("Server configuration")
    private ServerConfig server = new ServerConfig();

    @Getter
    @Setter
    public static class DatabaseConfig extends OkaeriConfig {
        @Comment("Database host address")
        private String host = "localhost";

        @Comment("Database port")
        private Integer port = 5432;

        @Comment("Database name")
        private String name = "mydb";

        @Comment("Connection timeout in seconds")
        private Integer timeout = 30;
    }

    @Getter
    @Setter
    public static class ServerConfig extends OkaeriConfig {
        @Comment("Server host")
        private String host = "0.0.0.0";

        @Comment("Server port")
        private Integer port = 8080;

        @Comment("Enable SSL")
        private Boolean ssl = false;
    }
}
```

**Output (YAML):**
```yaml
# Application settings
appName: MyApp

# Database configuration
database:
  # Database host address
  host: localhost
  # Database port
  port: 5432
  # Database name
  name: mydb
  # Connection timeout in seconds
  timeout: 30

# Server configuration
server:
  # Server host
  host: 0.0.0.0
  # Server port
  port: 8080
  # Enable SSL
  ssl: false
```

### Accessing Subconfig Values

```java
AppConfig config = ConfigManager.create(AppConfig.class, (it) -> {
    it.configure(opt -> {
        opt.configurer(new YamlSnakeYamlConfigurer());
        opt.bindFile("config.yml");
    });
    it.saveDefaults();
    it.load(true); // load and save to update comments/new fields
});

// Read values
String dbHost = config.getDatabase().getHost();
Integer serverPort = config.getServer().getPort();

// Modify values
config.getDatabase().setHost("db.example.com");
config.getDatabase().setPort(3306);
config.save();
```

### Multi-Level Nesting

Subconfigs can be nested multiple levels deep:

```java
@Getter
@Setter
public class GameConfig extends OkaeriConfig {

    @Comment("World settings")
    private WorldConfig world = new WorldConfig();

    @Getter
    @Setter
    public static class WorldConfig extends OkaeriConfig {
        @Comment("World name")
        private String name = "world";

        @Comment("Spawn configuration")
        private SpawnConfig spawn = new SpawnConfig();

        @Getter
        @Setter
        public static class SpawnConfig extends OkaeriConfig {
            @Comment("X coordinate")
            private Double x = 0.0;

            @Comment("Y coordinate")
            private Double y = 64.0;

            @Comment("Z coordinate")
            private Double z = 0.0;
        }
    }
}

// Access deeply nested values
Double spawnX = config.getWorld().getSpawn().getX();
config.getWorld().getSpawn().setY(100.0);
```

**Output (YAML):**
```yaml
# World settings
world:
  # World name
  name: world
  # Spawn configuration
  spawn:
    # X coordinate
    x: 0.0
    # Y coordinate
    y: 64.0
    # Z coordinate
    z: 0.0
```

### Reusable Subconfig Classes

Subconfigs can be defined as standalone classes and reused:

```java
// Reusable connection config
@Getter
@Setter
public class ConnectionConfig extends OkaeriConfig {
    @Comment("Host address")
    private String host = "localhost";

    @Comment("Port number")
    private Integer port;

    @Comment("Connection timeout (seconds)")
    private Integer timeout = 30;
}

// Use in multiple places
@Getter
@Setter
public class ServiceConfig extends OkaeriConfig {

    @Comment("Primary database connection")
    private ConnectionConfig primaryDb = new ConnectionConfig() {{
        setPort(5432);
    }};

    @Comment("Cache server connection")
    private ConnectionConfig cache = new ConnectionConfig() {{
        setPort(6379);
    }};

    @Comment("Message queue connection")
    private ConnectionConfig messageQueue = new ConnectionConfig() {{
        setHost("mq.example.com");
        setPort(5672);
    }};
}
```

### Subconfig Features

**✅ Advantages:**
- Full `OkaeriConfig` features (comments, validation, all annotations)
- Nested comments are preserved
- Type-safe access via getters/setters
- Can have their own subconfigs (unlimited nesting)
- Support for `@Variable`, `@Exclude`, etc.
- Can override parent methods

**❌ Limitations:**
- Slightly larger footprint than Serializable
- Must extend `OkaeriConfig`

## Serializable Objects

Serializable objects are plain Java classes implementing `Serializable`. They offer a lightweight alternative to subconfigs for simple data structures.

### Basic Serializable

```java
import lombok.*;
import java.io.Serializable;

@Getter
@Setter
public class PlayerConfig extends OkaeriConfig {

    @Comment("Player data")
    private Player player = new Player();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Player implements Serializable {
        private static final long serialVersionUID = 1L;

        private String username = "player";
        private String email = "player@example.com";
        private Integer level = 1;
        private Double health = 100.0;
    }
}
```

**Output (YAML):**
```yaml
# Player data
player:
  username: player
  email: player@example.com
  level: 1
  health: 100.0
```

> ⚠️ **Important**: Serializable classes MUST have a **no-args constructor**. Always initialize Serializable fields using the no-args constructor, not parameterized constructors.

**Why this matters:**

```java
// ✅ CORRECT - Uses no-args constructor
private Player player = new Player();

// ❌ WRONG - New fields won't get defaults on reload!
private Player player = new Player("admin", "admin@example.com", 99, 1000.0);
```

When you add new fields to the Serializable class later, configs saved with parameterized constructors won't properly initialize those new fields with their defaults.

### Complex Serializable

Serializable classes can contain collections and other supported types:

```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Quest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String questId;
    private String name;
    private String description;
    private List<String> objectives;
    private Map<String, Integer> rewards;
    private Boolean completed;
}

@Getter
@Setter
public class QuestConfig extends OkaeriConfig {

    @Comment("Active quests")
    private List<Quest> activeQuests = new ArrayList<>();

    @Comment("Completed quest IDs")
    private Set<String> completedQuestIds = new LinkedHashSet<>();
}
```

### Nested Serializable

Serializable objects can contain other Serializable objects:

```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Address implements Serializable {
    private static final long serialVersionUID = 1L;

    private String street;
    private String city;
    private String country;
}

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private String username;
    private String email;
    private Address address;  // Nested Serializable
    private List<String> roles;
}

@Getter
@Setter
public class UserConfig extends OkaeriConfig {

    @Comment("User profile")
    private User user = new User();
}
```

**Output (YAML):**
```yaml
# User profile
user:
  username: null
  email: null
  address:
    street: null
    city: null
    country: null
  roles: null
```

### Serializable Features

**✅ Advantages:**
- Lightweight (no `OkaeriConfig` overhead)
- Simple POJO structure
- Can be used with any serialization framework
- Faster initialization
- Can implement other interfaces

**❌ Limitations:**
- **No nested comments** (only top-level `@Comment` on the config field)
- Must have getters, setters, and **no-args constructor**
- No support for `@Variable`, `@Header`, etc. inside the class
- Cannot extend `OkaeriConfig` features

## Comparison: Subconfigs vs Serializable

| Feature | Subconfigs (OkaeriConfig) | Serializable |
|---------|--------------------------|--------------|
| **Nested Comments** | ✅ Yes | ❌ No |
| **Annotations Support** | ✅ All annotations | ❌ None (only on field) |
| **Validation** | ✅ Yes | ❌ Not directly |
| **Type Safety** | ✅ Yes | ✅ Yes |
| **Nesting** | ✅ Unlimited | ✅ Yes |
| **Size** | Moderate | Lightweight |
| **Complexity** | More features | Simpler |
| **Best For** | Complex configs | Simple data objects |

### When to Use Subconfigs

Use `OkaeriConfig` subconfigs when:
- You need nested comments for better documentation
- Configuration section is complex and needs validation
- You want to use `@Variable`, `@Header`, or other annotations
- You need methods or computed properties in the config

**Example:**
```java
@Getter
@Setter
public class DatabaseConfig extends OkaeriConfig {

    @Variable("DB_HOST")
    @Comment("Database host (can be set via DB_HOST env variable)")
    private String host = "localhost";

    @Variable("DB_PORT")
    @Comment("Database port (can be set via DB_PORT env variable)")
    private Integer port = 5432;

    @Comment("Full connection string (computed)")
    public String getConnectionString() {
        return "jdbc:postgresql://" + host + ":" + port;
    }
}
```

### When to Use Serializable

Use `Serializable` objects when:
- Data structure is simple and doesn't need comments
- You want minimal overhead
- Object is reused outside config contexts
- No validation or special features needed

**Example:**
```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Coordinate implements Serializable {
    private static final long serialVersionUID = 1L;

    private Double x;
    private Double y;
    private Double z;
}

// Clean, simple, lightweight
private Coordinate spawn = new Coordinate(0.0, 64.0, 0.0);
```

## Collections of Subconfigs/Serializables

Both subconfigs and serializable objects can be used in collections:

### List of Subconfigs

```java
@Getter
@Setter
public class ServersConfig extends OkaeriConfig {

    @Comment("Server definitions")
    private List<ServerDef> servers = new ArrayList<>();

    @Getter
    @Setter
    public static class ServerDef extends OkaeriConfig {
        @Comment("Server name")
        private String name;

        @Comment("Server address")
        private String address;

        @Comment("Server port")
        private Integer port;
    }
}
```

**Usage:**
```java
ServerDef server1 = new ServerDef();
server1.setName("Primary");
server1.setAddress("primary.example.com");
server1.setPort(25565);

config.getServers().add(server1);
```

### Map of Subconfigs

```java
@Getter
@Setter
public class WorldsConfig extends OkaeriConfig {

    @Comment("World configurations by name")
    private Map<String, WorldSettings> worlds = new LinkedHashMap<>();

    @Getter
    @Setter
    public static class WorldSettings extends OkaeriConfig {
        @Comment("World difficulty")
        private String difficulty = "normal";

        @Comment("PvP enabled")
        private Boolean pvp = false;

        @Comment("Spawn coordinates")
        private List<Double> spawn = List.of(0.0, 64.0, 0.0);
    }
}
```

**Output (YAML):**
```yaml
# World configurations by name
worlds:
  world:
    # World difficulty
    difficulty: normal
    # PvP enabled
    pvp: false
    # Spawn coordinates
    spawn: [0.0, 64.0, 0.0]
  world_nether:
    difficulty: hard
    pvp: true
    spawn: [0.0, 64.0, 0.0]
```

### List of Serializable

```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Item implements Serializable {
    private static final long serialVersionUID = 1L;

    private String itemId;
    private Integer quantity;
    private Map<String, String> metadata;
}

@Getter
@Setter
public class InventoryConfig extends OkaeriConfig {

    @Comment("Player inventory items")
    private List<Item> items = new ArrayList<>();
}
```

### Map of Serializable

```java
@Getter
@Setter
public class PlayersConfig extends OkaeriConfig {

    @Comment("Player data by UUID")
    private Map<String, PlayerData> players = new LinkedHashMap<>();
}

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlayerData implements Serializable {
    private static final long serialVersionUID = 1L;

    private String username;
    private Integer level;
    private Double experience;
    private List<String> achievements;
}
```

## Advanced: ConfigSerializable

For advanced use cases, you can implement `ConfigSerializable` to have full control over serialization:

```java
import eu.okaeri.configs.serdes.serializable.ConfigSerializable;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.SerializationData;
import eu.okaeri.configs.serdes.DeserializationData;
import lombok.NonNull;

public class CustomObject implements ConfigSerializable {

    private String data;
    private Integer version;

    // Custom serialization
    @Override
    public void serialize(@NonNull SerializationData data, @NonNull GenericsDeclaration generics) {
        data.add("encoded_data", Base64.getEncoder().encodeToString(this.data.getBytes()));
        data.add("v", this.version);
    }

    // Custom deserialization (static method)
    public static CustomObject deserialize(@NonNull DeserializationData data, @NonNull GenericsDeclaration generics) {
        CustomObject obj = new CustomObject();
        String encoded = data.get("encoded_data", String.class);
        obj.data = new String(Base64.getDecoder().decode(encoded));
        obj.version = data.get("v", Integer.class);
        return obj;
    }
}
```

> 📖 **Learn More**: See **[Advanced Topics](Advanced-Topics)** for detailed `ConfigSerializable` usage.

## Best Practices

### ✅ Do's

1. **Use subconfigs for complex sections:**
   ```java
   @Getter
   @Setter
   public static class DatabaseConfig extends OkaeriConfig {
       @Comment("Connection settings")
       // ... complex config with comments
   }
   ```

2. **Use Serializable for simple data objects:**
   ```java
   @Getter
   @Setter
   @NoArgsConstructor
   @AllArgsConstructor
   public static class Point implements Serializable {
       private Double x;
       private Double y;
   }
   ```

3. **Always provide no-args constructor for Serializable:**
   ```java
   @NoArgsConstructor  // ✅ Required!
   @AllArgsConstructor // Optional
   public static class Data implements Serializable {
       // ...
   }
   ```

4. **Initialize with no-args constructor:**
   ```java
   private Player player = new Player();  // ✅ Correct
   ```

5. **Include `serialVersionUID` for Serializable:**
   ```java
   private static final long serialVersionUID = 1L;  // ✅ Good practice
   ```

6. **Organize related settings into subconfigs:**
   ```java
   private DatabaseConfig database = new DatabaseConfig();
   private CacheConfig cache = new CacheConfig();
   private ServerConfig server = new ServerConfig();
   ```

### ❌ Don'ts

1. **Don't use parameterized constructor for initialization:**
   ```java
   // ❌ WRONG - New fields won't get defaults!
   private Player player = new Player("admin", "admin@example.com");

   // ✅ CORRECT
   private Player player = new Player();
   ```

2. **Don't expect nested comments in Serializable:**
   ```java
   public static class Data implements Serializable {
       @Comment("This won't appear")  // ❌ Won't work
       private String field;
   }
   ```

3. **Don't forget getters and setters:**
   ```java
   // ❌ Missing getters/setters
   public static class Data implements Serializable {
       private String field;
   }

   // ✅ With Lombok
   @Getter
   @Setter
   public static class Data implements Serializable {
       private String field;
   }
   ```

4. **Don't make Serializable classes without no-args constructor:**
   ```java
   // ❌ WRONG - No no-args constructor
   @AllArgsConstructor
   public static class Data implements Serializable {
       private String field;
   }

   // ✅ CORRECT - Has no-args constructor
   @NoArgsConstructor
   @AllArgsConstructor
   public static class Data implements Serializable {
       private String field;
   }
   ```

5. **Don't use subconfigs when Serializable is sufficient:**
   ```java
   // ❌ Overkill for simple coordinate
   public static class Coordinate extends OkaeriConfig {
       private Double x, y, z;
   }

   // ✅ Serializable is lighter
   @NoArgsConstructor
   @AllArgsConstructor
   public static class Coordinate implements Serializable {
       private Double x, y, z;
   }
   ```

### Initialization Patterns

**Subconfig initialization:**
```java
// ✅ Simple default
private DatabaseConfig database = new DatabaseConfig();

// ✅ With custom defaults using instance initializer
private DatabaseConfig database = new DatabaseConfig() {{
    setHost("db.example.com");
    setPort(3306);
}};
```

**Serializable initialization:**
```java
// ✅ CORRECT - No-args constructor
private Player player = new Player();

// ✅ CORRECT - Set values after
private Player player = new Player();
{
    player.setUsername("admin");
    player.setLevel(99);
}

// ❌ WRONG - Parameterized constructor
private Player player = new Player("admin", 99);
```

## Next Steps

- **[Validation](Validation)** - Add validation to subconfigs and serializable objects
- **[Collections & Maps](Collections-and-Maps)** - Working with lists and maps of subconfigs
- **[Advanced Topics](Advanced-Topics)** - Custom serialization with ConfigSerializable
- **[Annotations Guide](Annotations-Guide)** - Using annotations in subconfigs

## See Also

- **[Supported Types](Supported-Types)** - Complete type reference
- **[Configuration Basics](Configuration-Basics)** - Understanding OkaeriConfig
- **[Examples & Recipes](Examples-and-Recipes)** - Real-world configuration examples
