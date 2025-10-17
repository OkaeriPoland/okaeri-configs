# Supported Types

This page provides a complete reference of all types supported by Okaeri Configs out of the box.

## Type Categories

Okaeri Configs organizes supported types into four main categories:

1. **[Simple Types](#simple-types)** - Primitives, wrappers, strings, math types
2. **[Composite Types](#composite-types)** - Subconfigs, serializable objects, enums
3. **[Collection Types](#collection-types)** - Lists, sets, maps
4. **[Extension Types](#extension-types)** - Types from serdes modules

## Simple Types

### Primitives

All Java primitive types are supported:

| Type | Default Value | Example |
|------|---------------|---------|
| `boolean` | `false` | `true`, `false` |
| `byte` | `0` | `127`, `-128` |
| `short` | `0` | `32767`, `-32768` |
| `int` | `0` | `42`, `-1000` |
| `long` | `0L` | `9223372036854775807L` |
| `float` | `0.0f` | `3.14f`, `-2.5f` |
| `double` | `0.0` | `3.141592653589793` |
| `char` | `'\u0000'` | `'A'`, `'ż'` |

```java
public class PrimitivesConfig extends OkaeriConfig {
    private boolean enabled = true;
    private int port = 8080;
    private double version = 1.0;
    private char separator = ',';
}
```

**Output (YAML):**
```yaml
enabled: true
port: 8080
version: 1.0
separator: ','
```

> ⚠️ **Note**: Primitives cannot be `null`. Use wrapper classes if you need nullable values.

### Wrapper Classes

Wrapper classes provide nullable alternatives to primitives:

| Wrapper Type | Primitive Equivalent | Can be null? |
|--------------|---------------------|--------------|
| `Boolean` | `boolean` | ✅ Yes |
| `Byte` | `byte` | ✅ Yes |
| `Short` | `short` | ✅ Yes |
| `Integer` | `int` | ✅ Yes |
| `Long` | `long` | ✅ Yes |
| `Float` | `float` | ✅ Yes |
| `Double` | `double` | ✅ Yes |
| `Character` | `char` | ✅ Yes |

```java
public class WrappersConfig extends OkaeriConfig {
    private Integer optionalPort;  // Can be null
    private Boolean optionalFlag = null;  // Explicitly null
    private Double price = 19.99;
}
```

**Output (YAML):**
```yaml
optionalPort: null
optionalFlag: null
price: 19.99
```

### String

The `String` type is fully supported:

```java
public class StringsConfig extends OkaeriConfig {
    private String name = "My Application";
    private String multiline = "Line 1\nLine 2\nLine 3";
    private String empty = "";
    private String nullValue = null;
}
```

**Output (YAML):**
```yaml
name: My Application
multiline: |-
  Line 1
  Line 2
  Line 3
empty: ''
nullValue: null
```

### Math Types

Large number support via `java.math` package:

#### BigInteger

For arbitrarily large integers:

```java
import java.math.BigInteger;

public class BigIntConfig extends OkaeriConfig {
    private BigInteger largeNumber = new BigInteger("99999999999999999999999999");
    private BigInteger factorial = new BigInteger("93326215443944152681699238856266700490715968264381621468592963895217599993229915608941463976156518286253697920827223758251185210916864000000000000000000000000");
}
```

**Output (YAML):**
```yaml
largeNumber: '99999999999999999999999999'
factorial: '93326215443944152681699238856266700490715968264381621468592963895217599993229915608941463976156518286253697920827223758251185210916864000000000000000000000000'
```

#### BigDecimal

For precise decimal arithmetic:

```java
import java.math.BigDecimal;

public class BigDecimalConfig extends OkaeriConfig {
    private BigDecimal price = new BigDecimal("19.99");
    private BigDecimal preciseValue = new BigDecimal("0.123456789012345678901234567890");
}
```

**Output (YAML):**
```yaml
price: '19.99'
preciseValue: '0.123456789012345678901234567890'
```

### UUID

Java's `UUID` type is supported:

```java
import java.util.UUID;

public class UuidConfig extends OkaeriConfig {
    private UUID serverId = UUID.randomUUID();
    private UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
}
```

**Output (YAML):**
```yaml
serverId: 3f2504e0-4f89-41d3-9a0c-0305e82c3301
userId: 550e8400-e29b-41d4-a716-446655440000
```

## Composite Types

### Subconfigs

Classes extending `OkaeriConfig` can be nested:

```java
public class ServerConfig extends OkaeriConfig {

    @Comment("Database configuration")
    private DatabaseConfig database = new DatabaseConfig();

    @Comment("Cache configuration")
    private CacheConfig cache = new CacheConfig();

    public static class DatabaseConfig extends OkaeriConfig {
        @Comment("Database host")
        private String host = "localhost";
        private Integer port = 5432;
    }

    public static class CacheConfig extends OkaeriConfig {
        private Boolean enabled = true;
        private Integer ttl = 3600;
    }
}
```

**Output (YAML):**
```yaml
# Database configuration
database:
  # Database host
  host: localhost
  port: 5432

# Cache configuration
cache:
  enabled: true
  ttl: 3600
```

**Features:**
- ✅ Supports nested comments
- ✅ Full OkaeriConfig features
- ✅ Can be multiple levels deep
- ✅ Hot reload support

See **[Subconfigs & Serialization](Subconfigs-and-Serialization)** for more details.

### Serializable Objects

Plain Java classes implementing `Serializable`:

```java
import lombok.*;
import java.io.Serializable;

public class UserConfig extends OkaeriConfig {

    private User admin = new User();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class User implements Serializable {
        private String username = "admin";
        private String email = "admin@example.com";
    }
}
```

**Output (YAML):**
```yaml
admin:
  username: admin
  email: admin@example.com
```

**Features:**
- ✅ Lighter weight than subconfigs
- ❌ No nested comments support
- ✅ Requires getters/setters and **no-args constructor**

> ⚠️ **Important**: Always initialize Serializable objects using their **no-args constructor**, not parameterized constructors! This ensures that when new fields are added to the Serializable class, they will be properly initialized with their default values on deserialization. Using parameterized constructors will prevent new fields from getting their defaults.
>
> ```java
> // ✅ CORRECT - Uses no-args constructor
> private User admin = new User();
>
> // ❌ WRONG - Won't get new field defaults on reload!
> private User admin = new User("admin", "admin@example.com");
> ```

See **[Subconfigs & Serialization](Subconfigs-and-Serialization)** for comparison.

### Enums

All Java enum types are automatically supported:

```java
public class GameConfig extends OkaeriConfig {

    private Difficulty difficulty = Difficulty.NORMAL;
    private GameMode mode = GameMode.SURVIVAL;

    public enum Difficulty {
        PEACEFUL, EASY, NORMAL, HARD
    }

    public enum GameMode {
        SURVIVAL, CREATIVE, ADVENTURE, SPECTATOR
    }
}
```

**Output (YAML):**
```yaml
difficulty: NORMAL
mode: SURVIVAL
```

**Features:**
- ✅ Serialized using `name()`
- ✅ Deserialized using `valueOf()` with case-insensitive fallback
- ✅ Works with any enum type

**Case-insensitive loading:**
```yaml
difficulty: normal  # Works!
difficulty: NORMAL  # Also works!
difficulty: Normal  # Also works!
```

## Collection Types

### Lists

`List<T>` is supported with any supported element type:

```java
import java.util.*;

public class ListConfig extends OkaeriConfig {

    // Simple lists
    private List<String> admins = List.of("admin1", "admin2");
    private List<Integer> ports = List.of(8080, 8081, 8082);
    private List<Boolean> flags = List.of(true, false, true);

    // Empty lists (use mutable list if you need to modify)
    private List<String> empty = new ArrayList<>();

    // Null list
    private List<String> nullList = null;

    // Complex element types
    private List<UUID> playerIds = List.of(
        UUID.fromString("3f2504e0-4f89-41d3-9a0c-0305e82c3301"),
        UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8")
    );
}
```

**Output (YAML):**
```yaml
admins:
  - admin1
  - admin2
ports:
  - 8080
  - 8081
  - 8082
flags:
  - true
  - false
  - true
empty: []
nullList: null
playerIds:
  - 3f2504e0-4f89-41d3-9a0c-0305e82c3301
  - 6ba7b810-9dad-11d1-80b4-00c04fd430c8
```

**Implementation:** Deserializes to `ArrayList<T>` by default.

See **[Collections & Maps](Collections-and-Maps)** for advanced usage.

### Sets

`Set<T>` is supported with any supported element type:

```java
import java.util.*;

public class SetConfig extends OkaeriConfig {

    private Set<String> uniqueNames = Set.of(
        "Alice", "Bob", "Charlie"
    );

    private Set<Integer> uniqueNumbers = Set.of(1, 2, 3, 4, 5);

    private Set<GameMode> allowedModes = EnumSet.of(
        GameMode.SURVIVAL,
        GameMode.CREATIVE
    );
}
```

**Output (YAML):**
```yaml
uniqueNames:
  - Alice
  - Bob
  - Charlie
uniqueNumbers:
  - 1
  - 2
  - 3
  - 4
  - 5
allowedModes:
  - SURVIVAL
  - CREATIVE
```

**Implementation:** Deserializes to `LinkedHashSet<T>` by default (preserves order).

### Maps

`Map<K, V>` is supported with various key and value types:

#### String Keys

```java
import java.util.*;

public class MapConfig extends OkaeriConfig {

    // String -> Simple types
    private Map<String, String> messages = Map.of(
        "welcome", "Welcome to the server!",
        "goodbye", "See you later!"
    );

    private Map<String, Integer> limits = Map.of(
        "maxPlayers", 100,
        "maxConnections", 1000
    );

    // String -> Complex types
    private Map<String, List<String>> groups = Map.of(
        "admins", Arrays.asList("Alice", "Bob"),
        "moderators", Arrays.asList("Charlie", "David")
    );
}
```

**Output (YAML):**
```yaml
messages:
  welcome: Welcome to the server!
  goodbye: See you later!
limits:
  maxPlayers: 100
  maxConnections: 1000
groups:
  admins:
    - Alice
    - Bob
  moderators:
    - Charlie
    - David
```

#### Non-String Keys

```java
public class NonStringKeyMap extends OkaeriConfig {

    // Integer keys
    private Map<Integer, String> levelNames = Map.of(
        1, "Tutorial",
        2, "Easy Mode",
        3, "Normal Mode"
    );

    // Enum keys
    private Map<Difficulty, Double> damageMultipliers = Map.of(
        Difficulty.EASY, 0.5,
        Difficulty.NORMAL, 1.0,
        Difficulty.HARD, 2.0
    );

    // UUID keys
    private Map<UUID, String> playerNames = new HashMap<>();
}
```

**Output (YAML):**
```yaml
levelNames:
  1: Tutorial
  2: Easy Mode
  3: Normal Mode
damageMultipliers:
  EASY: 0.5
  NORMAL: 1.0
  HARD: 2.0
playerNames: {}
```

**Implementation:** Deserializes to `LinkedHashMap<K, V>` by default (preserves insertion order).

> ⚠️ **Format Limitations**: Some formats (like JSON) may not fully support non-string keys. Keys are serialized to strings and converted back on deserialization.

### Nested Collections

Complex nested structures are supported:

```java
public class NestedCollections extends OkaeriConfig {

    // List of Lists
    private List<List<Integer>> matrix = List.of(
        List.of(1, 2, 3),
        List.of(4, 5, 6),
        List.of(7, 8, 9)
    );

    // List of Maps
    private List<Map<String, String>> users = List.of(
        Map.of("name", "Alice", "role", "admin"),
        Map.of("name", "Bob", "role", "user")
    );

    // Map of Lists
    private Map<String, List<Integer>> scores = Map.of(
        "player1", List.of(10, 20, 30),
        "player2", List.of(15, 25, 35)
    );

    // Map of Maps
    private Map<String, Map<String, String>> config = Map.of(
        "database", Map.of("host", "localhost", "port", "5432"),
        "cache", Map.of("host", "localhost", "port", "6379")
    );
}
```

See **[Collections & Maps](Collections-and-Maps)** for more complex examples.

### Custom Collection Implementations

You can use custom collection implementations if they have a default constructor:

```java
import java.util.*;
import java.util.concurrent.*;

public class CustomCollections extends OkaeriConfig {

    // These work because they have default constructors
    private ArrayList<String> arrayList = new ArrayList<>();
    private LinkedList<String> linkedList = new LinkedList<>();
    private TreeSet<Integer> treeSet = new TreeSet<>();
    private TreeMap<String, String> treeMap = new TreeMap<>();
    private ConcurrentHashMap<String, String> concurrentMap = new ConcurrentHashMap<>();
}
```

## Extension Types

Additional types are available through serdes extension modules:

### serdes-commons

Common Java types:

- `java.time.Instant` - Timestamps
- `java.time.Duration` - Time durations
- `java.util.Locale` - Locales
- `java.util.regex.Pattern` - Regular expressions

**Installation:**
```xml
<dependency>
    <groupId>eu.okaeri</groupId>
    <artifactId>okaeri-configs-serdes-commons</artifactId>
    <version>5.0.13</version>
</dependency>
```

**Usage:**
```java
import eu.okaeri.configs.serdes.commons.SerdesCommons;

config.withConfigurer(new YamlSnakeYamlConfigurer(), new SerdesCommons());
```

See **[Serdes Extensions](Serdes-Extensions)** for details.

### serdes-bukkit

Minecraft Bukkit types:

- `org.bukkit.Location` - World locations
- `org.bukkit.inventory.ItemStack` - Items
- `org.bukkit.potion.PotionEffect` - Potion effects
- And more...

**Installation:**
```xml
<dependency>
    <groupId>eu.okaeri</groupId>
    <artifactId>okaeri-configs-serdes-bukkit</artifactId>
    <version>5.0.13</version>
</dependency>
```

See **[Platform Integration - Bukkit](Platform-Bukkit)** for details.

### Other Serdes Modules

- **serdes-bungee** - BungeeCord types
- **serdes-adventure** - Kyori Adventure components
- **serdes-bucket4j** - Rate limiting buckets
- **serdes-okaeri** - Okaeri Commons types
- **serdes-okaeri-bukkit** - Okaeri Bukkit Commons types

See **[Serdes Extensions](Serdes-Extensions)** for the complete list.

## Type Conversion

Okaeri Configs automatically converts between compatible types:

### String to Number

```yaml
port: "8080"  # String in file
```

```java
private Integer port;  // Converted to Integer
```

### Number to String

```java
private String version = "1.0";
```

```yaml
version: '1.0'  # Quoted in file
```

### Conservative vs Non-Conservative

When converting configs to maps, you can choose:

- **Conservative**: Preserves types (numbers stay numbers)
- **Non-Conservative**: Everything becomes strings

```java
Map<String, Object> conservative = config.asMap(configurer, true);
Map<String, Object> strings = config.asMap(configurer, false);
```

## Unsupported Types

These types are **not** supported out of the box:

- ❌ **Interfaces** (without concrete implementation)
- ❌ **Abstract classes** (without concrete implementation)
- ❌ **Generic wildcards** (`List<?>`, `Map<?, ?>`)
- ❌ **Raw types** (`List` without generics)
- ❌ **Multidimensional arrays** (`int[][]`, `String[][]`)
- ❌ **Functional interfaces** (lambdas, method references)
- ❌ **Records** (Java 14+) - Not yet supported

**Workarounds:**

1. **Use concrete types** instead of interfaces
2. **Create custom serializers** for special types
3. **Use Serializable wrapper classes**

See **[Serdes Extensions](Serdes-Extensions)** to learn how to add support for custom types.

## Next Steps

- **[Collections & Maps](Collections-and-Maps)** - Deep dive into collection usage
- **[Subconfigs & Serialization](Subconfigs-and-Serialization)** - Organize complex configs
- **[Serdes Extensions](Serdes-Extensions)** - Add support for more types
- **[Annotations Guide](Annotations-Guide)** - Document your configurations

## See Also

- **[Configuration Basics](Configuration-Basics)** - Understanding OkaeriConfig
- **[Advanced Topics](Advanced-Topics)** - Custom transformers and serializers
