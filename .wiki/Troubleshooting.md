# Troubleshooting

Common issues and solutions when working with okaeri-configs.

## Table of Contents

- [Serialization Issues](#serialization-issues)
- [Type Conversion Errors](#type-conversion-errors)
- [Loading & Saving Problems](#loading--saving-problems)
- [Validation Errors](#validation-errors)
- [Annotation Issues](#annotation-issues)
- [Performance Issues](#performance-issues)
- [Migration Problems](#migration-problems)

## Serialization Issues

### Type not supported

**Error:**
```
No serializer found for type: java.time.Instant
```

**Cause:** No serializer/transformer registered for this type.

**Solution:** Add the appropriate serdes extension:

```java
// Add SerdesCommons for Instant, Duration, Locale, Pattern
it.withConfigurer(
    new YamlSnakeYamlConfigurer(),
    new SerdesCommons()  // ← Add this
);
```

See [Serdes Extensions](Serdes-Extensions) for available extensions.

---

### ConfigSerializable missing deserialize method

**Error:**
```
NoSuchMethodException: MyClass.deserialize(DeserializationData, GenericsDeclaration)
```

**Cause:** ConfigSerializable requires a static `deserialize` method.

**Solution:** Add the static deserialize method:

```java
public class MyClass implements ConfigSerializable {

    @Override
    public void serialize(SerializationData data, GenericsDeclaration generics) {
        // Your serialization logic
    }

    // ✅ Add this method
    public static MyClass deserialize(DeserializationData data, GenericsDeclaration generics) {
        // Your deserialization logic
        return new MyClass(...);
    }
}
```

---

### Serializable object not getting new fields

**Problem:** Added new field to Serializable class, but existing configs don't get the default value.

**Cause:** Constructor initialization bypasses no-args constructor on reload.

**Solution:** Always use no-args constructor for default initialization:

```java
// ❌ WRONG - New fields won't get defaults
private Player player = new Player("admin", "admin@example.com", 99);

// ✅ CORRECT - Uses no-args constructor
private Player player = new Player();
```

See [Subconfigs & Serialization](Subconfigs-and-Serialization#serializable-vs-subconfig) for details.

---

### Enum serialization fails

**Error:**
```
IllegalArgumentException: No enum constant MyEnum.lowercase_value
```

**Cause:** Enum deserialization uses case-insensitive `valueOf()`, but the value doesn't match any enum constant.

**Solution:** Ensure enum constant names match config values (case-insensitive):

```java
public enum GameMode {
    SURVIVAL,    // Matches: "survival", "SURVIVAL", "SuRvIvAl"
    CREATIVE,    // Matches: "creative", "CREATIVE", etc.
    ADVENTURE
}
```

If you need different names in config:

```java
// Use ObjectTransformer for custom mapping
public class GameModeTransformer extends ObjectTransformer<String, GameMode> {
    @Override
    public GameMode transform(String data, SerdesContext context) {
        return switch (data.toLowerCase()) {
            case "s", "survival" -> GameMode.SURVIVAL;
            case "c", "creative" -> GameMode.CREATIVE;
            case "a", "adventure" -> GameMode.ADVENTURE;
            default -> throw new IllegalArgumentException("Unknown game mode: " + data);
        };
    }
}
```

## Type Conversion Errors

### NumberFormatException

**Error:**
```
NumberFormatException: For input string: "abc"
```

**Cause:** Config value cannot be converted to the expected numeric type.

**Config file:**
```yaml
port: abc  # ❌ Not a number
```

**Solution:** Fix the config value:

```yaml
port: 8080  # ✅ Valid integer
```

Or add validation:

```java
@Min(1) @Max(65535)
private Integer port = 8080;
```

---

### ClassCastException

**Error:**
```
ClassCastException: java.lang.Integer cannot be cast to java.lang.String
```

**Cause:** Field type doesn't match config value type.

**Problem:**
```java
private String port = "8080";  // Field is String
```

```yaml
port: 8080  # Config has Integer (no quotes)
```

**Solutions:**

1. Change field type to match config:
```java
private Integer port = 8080;
```

2. Or ensure config has string (quoted):
```yaml
port: "8080"
```

---

### Cannot deserialize Map with non-String keys

**Error:**
```
Cannot deserialize map with key type: java.lang.Integer
```

**Cause:** YAML/JSON configurers only support String keys in maps.

**Problem:**
```java
private Map<Integer, String> scores;  // ❌ Integer keys
```

**Solution:** Use String keys and convert if needed:

```java
private Map<String, String> scores;  // ✅ String keys

// Access with conversion
int score = Integer.parseInt(scores.get("123"));
```

Or use a List of entries:

```java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ScoreEntry implements Serializable {
    private int playerId;
    private String playerName;
}

private List<ScoreEntry> scores;
```

## Loading & Saving Problems

### FileNotFoundException

**Error:**
```
FileNotFoundException: config.yml (No such file or directory)
```

**Cause:** Config file doesn't exist and `saveDefaults()` wasn't called.

**Solution:** Call `saveDefaults()` before `load()`:

```java
MyConfig config = ConfigManager.create(MyConfig.class, (it) -> {
    it.withConfigurer(new YamlSnakeYamlConfigurer());
    it.withBindFile("config.yml");
    it.saveDefaults();  // ← Creates file if missing
    it.load();
});
```

---

### Config values not persisting

**Problem:** Changes made to config object don't save to file.

**Cause:** Forgot to call `save()` after modifying values.

**Solution:**

```java
config.setPort(9090);
config.save();  // ← Must call save()
```

Or use `load(true)` which auto-saves:

```java
config.load(true);  // Loads and saves (updates comments/new fields)
```

---

### Comments disappear after save

**Problem:** Comments removed from config file after loading.

**Cause:** Using wrong configurer or not calling `load(true)`.

**Solution:**

1. Use configurer that supports comments (YAML, HJSON):
```java
// ✅ Supports comments
new YamlSnakeYamlConfigurer()
new HjsonConfigurer()
new YamlBukkitConfigurer()

// ❌ No comment support
new JsonGsonConfigurer()
```

2. Use `load(true)` to preserve/update comments:
```java
config.load(true);  // Saves after loading to update comments
```

---

### Old config keys not removed

**Problem:** Deleted field from config class, but key remains in config file.

**Cause:** Orphan removal not enabled.

**Solution:** Enable orphan removal:

```java
it.withRemoveOrphans(true);  // Remove undeclared keys
it.load(true);  // Must save to apply changes
```

**Warning:** This removes any keys not declared in your config class.

## Validation Errors

### Validation fails with NullPointerException

**Error:**
```
NullPointerException during validation
```

**Cause:** Field is null but validation annotation doesn't allow nulls.

**Solution:**

1. Provide default value:
```java
@NotBlank
private String name = "default";  // ✅ Not null
```

2. Or allow null explicitly:
```java
@Nullable
private String optionalField;  // ✅ Can be null
```

3. Or use strict mode with @Nullable:
```java
// Strict mode - all fields @NotNull by default
new OkaeriValidator(configurer, true)

// Allow specific fields to be null
@Nullable
private String optional;
```

---

### Validation error messages unclear

**Problem:** Generic validation error without field context.

**Solution:** Add `@Comment` to document constraints:

```java
@Comment("Server port (1-65535)")
@Min(1) @Max(65535)
private Integer port = 8080;
```

Error will show:
```
ValidationException: port (-1) is invalid: must be greater than or equal to 1
```

---

### Custom validator not working

**Problem:** Created custom validator but it's not being used.

**Cause:** Validator not registered or configurer not wrapped.

**Solution:**

```java
// ✅ Wrap configurer with validator
it.withConfigurer(new OkaeriValidator(new YamlSnakeYamlConfigurer()));
```

Not:
```java
// ❌ Validator not applied
it.withConfigurer(new YamlSnakeYamlConfigurer());
```

## Annotation Issues

### @CustomKey not working

**Error:** Key name not changed in config file.

**Cause:** Using dots or trying to change nesting level.

**Problem:**
```java
@CustomKey("server.port")  // ❌ Dots not supported
private Integer serverPort;
```

**Solution:** Use camelCase without dots:

```java
@CustomKey("serverPort")  // ✅ Simple rename only
private Integer port;
```

**Note:** @CustomKey only renames keys, it cannot move fields to different nesting levels.

---

### @Include throws IllegalArgumentException

**Error:**
```
IllegalArgumentException: Class MyConfig does not extend BaseConfig
```

**Cause:** @Include requires extending the included class.

**Problem:**
```java
@Include(BaseConfig.class)
public class MyConfig extends OkaeriConfig {  // ❌ Doesn't extend BaseConfig
}
```

**Solution:** Extend the included class:

```java
@Include(BaseConfig.class)
public class MyConfig extends BaseConfig {  // ✅ Extends BaseConfig
}
```

---

### @Variable not replacing value

**Problem:** Environment variable not being used.

**Cause:** Environment variable not set.

**Solution:** Set environment variable before running:

```bash
# Linux/Mac
export API_KEY="your-key-here"
java -jar app.jar

# Windows
set API_KEY=your-key-here
java -jar app.jar

# Or via Java system property
java -DAPI_KEY=your-key-here -jar app.jar
```

Check if variable is set:

```java
@Variable("API_KEY")
private String apiKey = "default-key";

// Log the value
System.out.println("API_KEY: " + apiKey);
```

---

### @TargetType not changing collection type

**Problem:** @TargetType doesn't change the collection implementation.

**Cause:** Field is already a concrete type.

**Problem:**
```java
@TargetType(HashSet.class)
private LinkedHashSet<String> items;  // ❌ Already concrete, @TargetType ignored
```

**Solution:** Use interface type for field:

```java
@TargetType(HashSet.class)
private Set<String> items;  // ✅ Interface type, @TargetType applied
```

Or just declare the concrete type directly:

```java
private HashSet<String> items;  // ✅ Direct type, no annotation needed
```

## Performance Issues

### Slow config loading

**Problem:** Config takes several seconds to load.

**Causes:**

1. **Large config file** (10,000+ lines)
2. **Many complex serializers**
3. **Excessive validation**

**Solutions:**

1. Split into multiple config files:
```java
// Instead of one huge config
public class MainConfig extends OkaeriConfig {
    private DatabaseConfig database = new DatabaseConfig();
    private ServerConfig server = new ServerConfig();
    // ... 100 more fields
}

// Use separate config files
DatabaseConfig dbConfig = ConfigManager.create(DatabaseConfig.class, ...);
ServerConfig serverConfig = ConfigManager.create(ServerConfig.class, ...);
```

2. Disable validation for non-critical configs:
```java
// Only validate critical configs
it.withConfigurer(new OkaeriValidator(configurer));  // Critical

// Skip validation for non-critical
it.withConfigurer(configurer);  // Non-critical, faster
```

3. Use HJSON instead of SnakeYAML for better performance:
```java
// HJSON is faster for large files
new HjsonConfigurer()
```

---

### High memory usage

**Problem:** Config uses too much memory.

**Causes:**

1. Large collections in memory
2. Too many config instances
3. Duplicate data structures

**Solutions:**

1. Use lazy loading for large data:
```java
// ❌ Loads entire player database into memory
private Map<UUID, PlayerData> allPlayers = new HashMap<>();

// ✅ Load on-demand
public PlayerData getPlayer(UUID uuid) {
    // Load from separate file or database
}
```

2. Reuse config instances:
```java
// ❌ Creates new config every time
public MyConfig getConfig() {
    return ConfigManager.create(MyConfig.class, ...);
}

// ✅ Reuse singleton
private static final MyConfig CONFIG = ConfigManager.create(MyConfig.class, ...);
public MyConfig getConfig() {
    return CONFIG;
}
```

## Migration Problems

### Migrating from @Names to field names

**Problem:** Old configs use @Names annotation which is deprecated.

**Old code:**
```java
@Names({"server-ip", "serverIp"})
private String serverIp;
```

**Solution:** Use @CustomKey for backwards compatibility:

```java
// Support old key during migration
@CustomKey("server-ip")
private String serverIp = "localhost";
```

**Migration steps:**

1. Add @CustomKey to read old configs
2. Load and save to update files
3. After migration complete, remove @CustomKey

---

### Changing field types

**Problem:** Changed field type, old configs fail to load.

**Old:**
```java
private Integer port = 8080;
```

**New:**
```java
private String port = "8080";  // Changed to String
```

**Error:** `ClassCastException` when loading old configs.

**Solutions:**

1. **Use custom transformer:**
```java
public class FlexiblePortTransformer extends ObjectTransformer<Object, String> {
    @Override
    public String transform(Object data, SerdesContext context) {
        return data.toString();  // Convert anything to String
    }
}
```

2. **Use version field:**
```java
@Exclude  // Don't save to file
private final int configVersion = 2;

public void load() {
    super.load();
    if (configVersion < 2) {
        // Migrate old data
        migrateFromV1();
    }
}
```

3. **Keep old field temporarily:**
```java
@CustomKey("port")
private Integer portOld;  // Read old value

private String port;  // New field

@Override
public void load() {
    super.load();
    if (portOld != null) {
        port = portOld.toString();  // Migrate
        portOld = null;
    }
}
```

---

### Moving fields to subconfigs

**Problem:** Want to organize flat config into nested structure.

**Old:**
```yaml
serverHost: localhost
serverPort: 8080
serverTimeout: 30
```

**New (desired):**
```yaml
server:
  host: localhost
  port: 8080
  timeout: 30
```

**Solution:** Create migration logic:

```java
public class MyConfig extends OkaeriConfig {

    // Old fields (read but don't write)
    @CustomKey("serverHost")
    @Exclude
    private String oldHost;

    @CustomKey("serverPort")
    @Exclude
    private Integer oldPort;

    // New structure
    private ServerConfig server = new ServerConfig();

    @Override
    public void load() {
        super.load();

        // Migrate old values
        if (oldHost != null) {
            server.setHost(oldHost);
            oldHost = null;
        }
        if (oldPort != null) {
            server.setPort(oldPort);
            oldPort = null;
        }

        // Save with new structure
        this.save();
    }

    public static class ServerConfig extends OkaeriConfig {
        private String host = "localhost";
        private Integer port = 8080;
        private Integer timeout = 30;
    }
}
```

## Debugging Tips

### Enable detailed logging

Use Java logging to see what's happening:

```java
import java.util.logging.*;

Logger.getLogger("eu.okaeri.configs").setLevel(Level.FINE);
```

### Inspect serialization output

Check what's being written:

```java
MyConfig config = new MyConfig();
config.withConfigurer(new YamlSnakeYamlConfigurer());

// Print to console instead of file
String yaml = config.saveToString();
System.out.println(yaml);
```

### Test serialization roundtrip

Verify serialize → deserialize works:

```java
// Original
MyConfig original = new MyConfig();
original.setPort(8080);

// Save and reload
String yaml = original.saveToString();
MyConfig reloaded = new MyConfig();
reloaded.withConfigurer(new YamlSnakeYamlConfigurer());
reloaded.load(yaml);

// Compare
assert original.getPort().equals(reloaded.getPort());
```

### Check SerdesRegistry

See what's registered:

```java
YamlSnakeYamlConfigurer configurer = new YamlSnakeYamlConfigurer();
SerdesRegistry registry = configurer.getRegistry();

// Check if type is supported
ObjectSerializer<?> serializer = registry.getSerializer(MyType.class);
if (serializer == null) {
    System.out.println("No serializer found for MyType!");
}

// Check if transformation is possible
boolean canTransform = registry.canTransform(
    GenericsDeclaration.of(String.class),
    GenericsDeclaration.of(MyType.class)
);
```

## Getting Help

If your issue isn't covered here:

1. **Check the wiki:** [Home](Home) - Comprehensive guides and references
2. **Review examples:** [Examples & Recipes](Examples-and-Recipes) - Real-world usage patterns
3. **Search GitHub issues:** [okaeri-configs issues](https://github.com/OkaeriPoland/okaeri-configs/issues)
4. **Ask on Discord:** [OkaeriPoland Discord](https://discord.gg/hASN5eX)

## See Also

- **[Configuration Basics](Configuration-Basics)** - Understanding core concepts
- **[Advanced Topics](Advanced-Topics)** - Custom serializers and transformers
- **[Validation](Validation)** - Adding validation to configs
