# Collections and Maps

This guide covers working with collections (Lists, Sets) and maps in Okaeri Configs. Collections are essential for storing multiple values, groups of items, and complex data structures in your configurations.

## Table of Contents

- [Lists](#lists)
- [Sets](#sets)
- [Maps](#maps)
- [Nested Collections](#nested-collections)
- [Default Implementations](#default-implementations)
- [Custom Implementations](#custom-implementations)
- [Common Patterns](#common-patterns)
- [Best Practices](#best-practices)

## Lists

Lists are ordered collections that allow duplicate elements. They are perfect for sequences, ordered data, and when you need to maintain insertion order.

### Basic Lists

```java
import eu.okaeri.configs.OkaeriConfig;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ListConfig extends OkaeriConfig {

    // String lists
    private List<String> playerNames = List.of("Alice", "Bob", "Charlie");

    // Number lists
    private List<Integer> ports = List.of(8080, 8081, 8082);

    // Boolean lists
    private List<Boolean> features = List.of(true, false, true);

    // Empty list (use mutable ArrayList if you need to modify)
    private List<String> emptyList = new ArrayList<>();
}
```

**Output (YAML):**
```yaml
playerNames:
  - Alice
  - Bob
  - Charlie
ports:
  - 8080
  - 8081
  - 8082
features:
  - true
  - false
  - true
emptyList: []
```

### List Characteristics

**✅ Advantages:**
- Maintains insertion order
- Allows duplicate elements
- Supports indexed access
- Fast iteration

**Example with duplicates:**
```java
private List<String> tags = List.of("admin", "vip", "admin", "moderator");
// Duplicates are preserved: ["admin", "vip", "admin", "moderator"]
```

### Mutable vs Immutable Lists

```java
public class MutableListConfig extends OkaeriConfig {

    // ✅ Immutable - good for defaults (List.of creates unmodifiable list)
    private List<String> defaultPermissions = List.of("basic.access", "basic.chat");

    // ✅ Mutable - use when you need to modify at runtime
    private List<String> activeUsers = new ArrayList<>();

    // ✅ Mutable with initial values
    private List<String> allowedCommands = new ArrayList<>(List.of("help", "info"));
}
```

> 💡 **Tip**: Use `List.of()` for immutable defaults, `new ArrayList<>()` when you need to modify the list at runtime.

## Sets

Sets are unordered collections that do **not** allow duplicate elements. They're ideal for unique values, membership testing, and eliminating duplicates.

### Basic Sets

```java
@Getter
@Setter
public class SetConfig extends OkaeriConfig {

    // String sets
    private Set<String> uniqueUsernames = Set.of("alice", "bob", "charlie");

    // Number sets
    private Set<Integer> allowedLevels = Set.of(1, 5, 10, 25, 50);

    // Enum sets
    private Set<GameMode> enabledModes = Set.of(
        GameMode.SURVIVAL,
        GameMode.CREATIVE
    );

    // Empty set
    private Set<String> emptySet = new LinkedHashSet<>();
}

public enum GameMode {
    SURVIVAL, CREATIVE, ADVENTURE, SPECTATOR
}
```

**Output (YAML):**
```yaml
uniqueUsernames:
  - alice
  - bob
  - charlie
allowedLevels:
  - 1
  - 5
  - 10
  - 25
  - 50
enabledModes:
  - SURVIVAL
  - CREATIVE
emptySet: []
```

### Set Characteristics

**✅ Advantages:**
- Automatically removes duplicates
- Fast membership testing
- Order preserved with LinkedHashSet (default)

**Example with duplicates removed:**
```java
// In file:
# tags:
#   - admin
#   - vip
#   - admin  # Duplicate will be removed on load
#   - moderator

private Set<String> tags = new LinkedHashSet<>();
// After loading: ["admin", "vip", "moderator"] - duplicates removed
```

### Order Preservation

The default implementation (`LinkedHashSet`) **preserves insertion order**:

```java
@Getter
@Setter
public class OrderedSetConfig extends OkaeriConfig {

    private Set<String> orderedTags = Set.of("first", "second", "third");

    // Will be saved and loaded in the same order
}
```

## Maps

Maps store key-value pairs, perfect for lookups, associations, and structured data.

### String Keys (Most Common)

```java
@Getter
@Setter
public class MapConfig extends OkaeriConfig {

    // String -> String
    private Map<String, String> messages = Map.of(
        "welcome", "Welcome to the server!",
        "goodbye", "See you later!"
    );

    // String -> Number
    private Map<String, Integer> limits = Map.of(
        "maxPlayers", 100,
        "maxConnections", 1000
    );

    // String -> List
    private Map<String, List<String>> permissions = Map.of(
        "admin", List.of("*"),
        "moderator", List.of("kick", "ban", "mute")
    );

    // Empty map
    private Map<String, String> emptyMap = new LinkedHashMap<>();
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
permissions:
  admin:
    - '*'
  moderator:
    - kick
    - ban
    - mute
emptyMap: {}
```

### Non-String Keys

Maps support various key types:

```java
@Getter
@Setter
public class NonStringKeyConfig extends OkaeriConfig {

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
    private Map<UUID, String> playerData = new LinkedHashMap<>();
}

public enum Difficulty {
    EASY, NORMAL, HARD
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
playerData: {}
```

> ⚠️ **Format Note**: Some formats (like JSON) serialize all keys to strings internally. They're converted back to the correct type on deserialization.

### Map Characteristics

**✅ Advantages:**
- Fast key-based lookup
- Insertion order preserved with LinkedHashMap (default)
- Supports null values (but not null keys)

**Null values example:**
```java
private Map<String, String> config = new LinkedHashMap<>();
// config.put("key1", "value");
// config.put("key2", null);  // null value is allowed
```

**Output (YAML):**
```yaml
config:
  key1: value
  key2: null
```

## Nested Collections

Okaeri Configs supports complex nested structures:

### List of Lists

```java
@Getter
@Setter
public class NestedListConfig extends OkaeriConfig {

    // 2D matrix
    private List<List<Integer>> matrix = List.of(
        List.of(1, 2, 3),
        List.of(4, 5, 6),
        List.of(7, 8, 9)
    );

    // Grouped items
    private List<List<String>> teams = List.of(
        List.of("Alice", "Bob"),
        List.of("Charlie", "David"),
        List.of("Eve", "Frank")
    );
}
```

**Output (YAML):**
```yaml
matrix:
  - [1, 2, 3]
  - [4, 5, 6]
  - [7, 8, 9]
teams:
  - [Alice, Bob]
  - [Charlie, David]
  - [Eve, Frank]
```

### List of Maps

```java
@Getter
@Setter
public class ListOfMapsConfig extends OkaeriConfig {

    private List<Map<String, String>> users = List.of(
        Map.of("name", "Alice", "role", "admin"),
        Map.of("name", "Bob", "role", "moderator"),
        Map.of("name", "Charlie", "role", "user")
    );
}
```

**Output (YAML):**
```yaml
users:
  - name: Alice
    role: admin
  - name: Bob
    role: moderator
  - name: Charlie
    role: user
```

### Map of Lists

```java
@Getter
@Setter
public class MapOfListsConfig extends OkaeriConfig {

    private Map<String, List<String>> groupPermissions = Map.of(
        "admin", List.of("*"),
        "moderator", List.of("kick", "ban", "mute"),
        "user", List.of("chat", "build")
    );

    private Map<String, List<Integer>> playerScores = Map.of(
        "player1", List.of(100, 200, 300),
        "player2", List.of(150, 250, 350)
    );
}
```

**Output (YAML):**
```yaml
groupPermissions:
  admin:
    - '*'
  moderator:
    - kick
    - ban
    - mute
  user:
    - chat
    - build
playerScores:
  player1:
    - 100
    - 200
    - 300
  player2:
    - 150
    - 250
    - 350
```

### Map of Maps

```java
@Getter
@Setter
public class MapOfMapsConfig extends OkaeriConfig {

    private Map<String, Map<String, String>> serverConfig = Map.of(
        "database", Map.of(
            "host", "localhost",
            "port", "5432",
            "name", "mydb"
        ),
        "cache", Map.of(
            "host", "localhost",
            "port", "6379"
        )
    );
}
```

**Output (YAML):**
```yaml
serverConfig:
  database:
    host: localhost
    port: '5432'
    name: mydb
  cache:
    host: localhost
    port: '6379'
```

> 💡 **Tip**: For complex nested structures, consider using [subconfigs](Subconfigs-and-Serialization) for better organization and nested comments.

## Default Implementations

Okaeri Configs automatically chooses collection implementations when you use interface types:

| Interface Type | Default Implementation | Characteristics |
|----------------|----------------------|-----------------|
| `List<T>` | `ArrayList<T>` | Fast indexed access, ordered |
| `Set<T>` | `LinkedHashSet<T>` | Unique elements, **order preserved** |
| `Map<K, V>` | `LinkedHashMap<K, V>` | Key-value pairs, **order preserved** |

```java
@Getter
@Setter
public class DefaultsConfig extends OkaeriConfig {

    // These will use default implementations:
    private List<String> names;       // → ArrayList<String>
    private Set<Integer> numbers;      // → LinkedHashSet<Integer>
    private Map<String, String> data;  // → LinkedHashMap<String, String>
}
```

> 📖 **Learn More**: See **[Supported Types](Supported-Types#collection-types)** for complete details on default implementations.

## Custom Implementations

### Using Concrete Types (Recommended)

The preferred way to use a specific implementation is to declare the field with a concrete type:

```java
@Getter
@Setter
public class ConcreteTypesConfig extends OkaeriConfig {

    // ✅ PREFERRED: Concrete type in field declaration
    private HashSet<String> fastLookupSet = new HashSet<>();
    private TreeSet<Integer> sortedNumbers = new TreeSet<>();
    private ArrayList<String> indexedList = new ArrayList<>();
    private TreeMap<String, Integer> sortedMap = new TreeMap<>();
}
```

This automatically uses the specified implementation without needing `@TargetType`.

### Using @TargetType (When Needed)

Use `@TargetType` only when you **cannot** change the field type (e.g., API compatibility):

```java
import eu.okaeri.configs.annotation.TargetType;

@Getter
@Setter
public class TargetTypeConfig extends OkaeriConfig {

    // ✅ Use @TargetType when field type must be interface
    @TargetType(HashSet.class)
    private Set<String> fastSet = new HashSet<>();  // API requires Set interface

    @TargetType(TreeMap.class)
    private Map<String, Integer> sortedMap = new TreeMap<>();
}
```

> 📖 **Learn More**: See **[Annotations Guide - @TargetType](Annotations-Guide#targettype)** for detailed usage.

### Available Implementations

**List implementations:**
- `ArrayList` - Fast indexed access (default)
- `LinkedList` - Fast insertions/deletions
- `Vector` - Thread-safe (legacy)
- `Stack` - LIFO stack (legacy)

**Set implementations:**
- `LinkedHashSet` - Order preserved (default)
- `HashSet` - Fastest performance, no order
- `TreeSet` - Sorted order

**Map implementations:**
- `LinkedHashMap` - Order preserved (default)
- `HashMap` - Fastest performance, no order
- `TreeMap` - Sorted by keys
- `ConcurrentHashMap` - Thread-safe

**Example with different implementations:**

```java
@Getter
@Setter
public class ImplementationsConfig extends OkaeriConfig {

    // Fast unordered set (no duplicates, no order guarantee)
    private HashSet<String> tags = new HashSet<>();

    // Sorted set (automatically sorted)
    private TreeSet<Integer> levels = new TreeSet<>();

    // Sorted map (keys sorted alphabetically)
    private TreeMap<String, Integer> scores = new TreeMap<>();

    // Thread-safe map (for concurrent access)
    private ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();
}
```

## Common Patterns

### Pattern 1: Whitelists and Blacklists

```java
@Getter
@Setter
public class AccessConfig extends OkaeriConfig {

    @Comment("Allowed IP addresses")
    private Set<String> allowedIps = Set.of(
        "127.0.0.1",
        "192.168.1.0/24"
    );

    @Comment("Blocked users")
    private Set<String> blockedUsers = new LinkedHashSet<>();

    @Comment("Command aliases")
    private Map<String, String> commandAliases = Map.of(
        "?", "help",
        "tpa", "teleport",
        "msg", "message"
    );
}
```

### Pattern 2: Grouped Configuration

```java
@Getter
@Setter
public class GroupedConfig extends OkaeriConfig {

    @Comment("Permissions by role")
    private Map<String, List<String>> rolePermissions = Map.of(
        "admin", List.of("*"),
        "moderator", List.of("kick", "ban", "mute"),
        "vip", List.of("fly", "kit.vip"),
        "default", List.of("chat", "build")
    );

    @Comment("Spawn locations by world")
    private Map<String, List<Double>> worldSpawns = Map.of(
        "world", List.of(0.0, 64.0, 0.0),
        "world_nether", List.of(0.0, 64.0, 0.0),
        "world_the_end", List.of(100.0, 48.0, 0.0)
    );
}
```

### Pattern 3: Translations/Messages

```java
@Getter
@Setter
public class MessagesConfig extends OkaeriConfig {

    @Comment("Player messages")
    private Map<String, String> messages = Map.of(
        "join", "Welcome {player}!",
        "quit", "{player} left the game",
        "death", "{player} died"
    );

    @Comment("Error messages")
    private Map<String, String> errors = Map.of(
        "no-permission", "You don't have permission",
        "invalid-command", "Unknown command",
        "player-not-found", "Player not found"
    );
}
```

### Pattern 4: Feature Flags

```java
@Getter
@Setter
public class FeaturesConfig extends OkaeriConfig {

    @Comment("Enable/disable features")
    private Map<String, Boolean> features = Map.of(
        "economy", true,
        "pvp", true,
        "explosions", false,
        "mobSpawning", true
    );

    @Comment("Feature-specific settings")
    private Map<String, Map<String, Object>> featureSettings = Map.of(
        "economy", Map.of(
            "startingBalance", 1000,
            "currency", "$"
        ),
        "pvp", Map.of(
            "enabled", true,
            "allowedWorlds", List.of("world", "arena")
        )
    );
}
```

### Pattern 5: Item/Reward Lists

```java
@Getter
@Setter
public class RewardsConfig extends OkaeriConfig {

    @Comment("Daily rewards by level")
    private Map<Integer, List<String>> dailyRewards = Map.of(
        1, List.of("DIAMOND:1", "GOLD_INGOT:5"),
        5, List.of("DIAMOND:3", "GOLD_INGOT:10", "EMERALD:1"),
        10, List.of("DIAMOND:5", "EMERALD:3", "NETHERITE_INGOT:1")
    );

    @Comment("Achievement rewards")
    private Map<String, Map<String, Object>> achievements = Map.of(
        "first_kill", Map.of(
            "title", "First Blood",
            "rewards", List.of("DIAMOND:1"),
            "experience", 100
        )
    );
}
```

## Best Practices

### ✅ Do's

1. **Use concrete types for specific implementations:**
   ```java
   private HashSet<String> uniqueIds = new HashSet<>();  // ✅ Clear and direct
   ```

2. **Use `List.of()`, `Set.of()`, `Map.of()` for immutable defaults:**
   ```java
   private List<String> defaults = List.of("a", "b", "c");  // ✅ Immutable
   ```

3. **Use `ArrayList`, `LinkedHashSet`, `LinkedHashMap` for mutable collections:**
   ```java
   private List<String> items = new ArrayList<>();  // ✅ Can be modified
   ```

4. **Choose the right collection type:**
   - **List**: When order matters or duplicates are needed
   - **Set**: When uniqueness is required
   - **Map**: For key-value associations

5. **Use appropriate key types:**
   ```java
   private Map<String, String> configs = Map.of();  // ✅ String keys (most compatible)
   private Map<Integer, String> levels = Map.of();  // ✅ Integer keys work fine
   ```

### ❌ Don'ts

1. **Don't use raw types (without generics):**
   ```java
   private List items = new ArrayList();  // ❌ Raw type - NOT supported
   private List<String> items = new ArrayList<>();  // ✅ Generic type
   ```

2. **Don't use wildcard generics:**
   ```java
   private List<?> items = new ArrayList<>();  // ❌ Wildcard - NOT supported
   private List<String> items = new ArrayList<>();  // ✅ Concrete generic
   ```

3. **Don't use `List.of()` when you need to modify the collection:**
   ```java
   private List<String> items = List.of("a", "b");
   items.add("c");  // ❌ Runtime error: UnsupportedOperationException

   private List<String> items = new ArrayList<>(List.of("a", "b"));
   items.add("c");  // ✅ Works fine
   ```

4. **Don't use arrays instead of collections:**
   ```java
   private String[] items = {"a", "b"};  // ❌ Arrays not well supported
   private List<String> items = List.of("a", "b");  // ✅ Use List
   ```

5. **Don't use null for empty collections:**
   ```java
   private List<String> items = null;  // ❌ Null - can cause NPE
   private List<String> items = new ArrayList<>();  // ✅ Empty list
   ```

### Performance Tips

1. **Choose LinkedHashSet over HashSet if order matters:**
   ```java
   // Order matters (e.g., display order)
   private Set<String> orderedTags = new LinkedHashSet<>();

   // Order doesn't matter (fastest)
   private Set<String> uniqueIds = new HashSet<>();
   ```

2. **Use TreeSet/TreeMap for sorted collections:**
   ```java
   // Automatically sorted
   private TreeSet<Integer> levels = new TreeSet<>();
   private TreeMap<String, String> sortedConfig = new TreeMap<>();
   ```

3. **For large collections, consider the right implementation:**
   - **ArrayList**: Best for indexed access
   - **LinkedList**: Best for frequent insertions/deletions
   - **HashSet**: Fastest lookups
   - **TreeSet**: Sorted, slower than HashSet

## Next Steps

- **[Subconfigs & Serialization](Subconfigs-and-Serialization)** - Organize complex configurations with nested configs
- **[Supported Types](Supported-Types)** - Complete type reference including collections
- **[Annotations Guide](Annotations-Guide)** - Learn about @TargetType and other annotations
- **[Examples & Recipes](Examples-and-Recipes)** - More real-world examples

## See Also

- **[Configuration Basics](Configuration-Basics)** - Understanding OkaeriConfig fundamentals
- **[Advanced Topics](Advanced-Topics)** - Custom serializers for special collection types
