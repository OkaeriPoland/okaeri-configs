# Configuration Basics

This guide covers the fundamental concepts of working with Okaeri Configs.

## Understanding OkaeriConfig

Every configuration class must extend `OkaeriConfig`. This base class provides all the functionality for loading, saving, and managing your configuration.

```java
import eu.okaeri.configs.OkaeriConfig;

public class MyConfig extends OkaeriConfig {
    // Your configuration fields
}
```

### Key Characteristics

- **Must have a no-argument constructor** (default constructor is fine)
- **Non-transient fields** are automatically included in the configuration
- **Field values serve as defaults** when creating new config files
- **Supports getters and setters** for accessing configuration values

## Field Declarations

### Basic Field Types

Okaeri Configs supports a wide variety of field types out of the box:

```java
public class ExampleConfig extends OkaeriConfig {

    // Wrapper types (recommended)
    private String text = "Hello World";
    private Integer number = 42;
    private Double decimal = 3.14;
    private Boolean flag = true;

    // Primitives (use when null is not needed)
    private int count = 0;
    private double ratio = 0.5;
    private boolean enabled = false;

    // Math types
    private BigInteger bigNumber = new BigInteger("999999999999999");
    private BigDecimal preciseMoney = new BigDecimal("19.99");

    // UUID
    private UUID uniqueId = UUID.randomUUID();
}
```

### Field Visibility

Fields can have any visibility modifier:

```java
public class VisibilityExample extends OkaeriConfig {

    public String publicField = "visible";
    protected String protectedField = "also works";
    private String privateField = "works too";  // Most common
    String packageField = "package-private works";
}
```

> 💡 **Best Practice**: Use `private` fields with getters/setters for proper encapsulation.

## Excluded Fields

### Using transient

Fields marked as `transient` are not saved or loaded:

```java
public class RuntimeConfig extends OkaeriConfig {

    private String configValue = "saved";

    // This field exists only in memory
    private transient Instant startTime = Instant.now();
    private transient Connection databaseConnection;
}
```

**Use Cases for transient:**
- Runtime-only data (connections, timestamps)
- Cached values computed from config
- Temporary state that shouldn't persist

### Using @Exclude

The `@Exclude` annotation also prevents serialization:

```java
import eu.okaeri.configs.annotation.Exclude;

public class ExcludeExample extends OkaeriConfig {

    private String normalField = "saved";

    @Exclude
    private String excludedField = "not saved";
}
```

### Special Excluded Fields

These fields are automatically excluded:

- `serialVersionUID` - Used for Java serialization version control
- Fields from `java.base` module that aren't accessible

## Default Values

Field initializers define the default values used when creating a new configuration file:

```java
public class DefaultsConfig extends OkaeriConfig {

    private String serverName = "My Server";      // Default: "My Server"
    private int maxPlayers = 20;                  // Default: 20
    private boolean pvpEnabled = true;            // Default: true
    private List<String> admins = Arrays.asList(  // Default: list with one admin
        "admin@example.com"
    );
}
```

### Understanding Null Defaults

```java
public class NullableConfig extends OkaeriConfig {

    // Will be null if not set in config file
    private String optionalValue;
    private Integer optionalNumber;

    // Will have default if not set in config file
    private String requiredValue = "default";
    private Integer requiredNumber = 0;
}
```

## Getters and Setters

### Manual Getters/Setters

```java
public class ManualAccessors extends OkaeriConfig {

    private String value;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
```

### Using Lombok (Recommended)

```java
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LombokConfig extends OkaeriConfig {

    private String value;
    private Integer number;
    private Boolean flag;

    // Lombok generates all getters and setters
}
```

### No Accessors (Direct Access)

While not recommended for production, you can access fields directly in simple cases:

```java
public class SimpleConfig extends OkaeriConfig {
    public String value = "directly accessible";
}

// Usage
SimpleConfig config = ConfigManager.create(SimpleConfig.class, ...);
String val = config.value;  // Direct access
```

## Configuration Lifecycle Methods

### save()

Saves the current configuration state to the bound file:

```java
config.setValue("newValue");
config.save();  // Writes to disk
```

### load()

Loads configuration from the bound file:

```java
config.load();  // Reads from disk, updates object
```

### load(boolean update)

Loads and optionally saves afterwards:

```java
config.load(true);   // Load, then save (updates comments/new fields)
config.load(false);  // Just load
```

### saveDefaults()

Saves only if the file doesn't exist:

```java
config.saveDefaults();  // Creates file with defaults if missing
```

### saveToString()

Returns configuration as a string without saving to file:

```java
String yaml = config.saveToString();
System.out.println(yaml);
```

## Direct Key Access

You can access configuration values by string keys:

### get(String key)

```java
Object value = config.get("serverName");
String name = (String) value;
```

### get(String key, Class<T> type)

```java
String name = config.get("serverName", String.class);
Integer port = config.get("port", Integer.class);
```

### set(String key, Object value)

```java
config.set("serverName", "New Name");
config.set("port", 8080);
config.save();
```

**When to use direct access:**
- Dynamic configuration keys
- Programmatic config manipulation
- Migration scripts
- Config editors/GUIs

**When to use fields:**
- Type-safe access (recommended)
- IDE autocomplete support
- Refactoring safety

## Configuration Declaration

Every config has a declaration that describes its structure:

```java
ConfigDeclaration declaration = config.getDeclaration();

// Get all fields
for (FieldDeclaration field : declaration.getFields()) {
    System.out.println("Field: " + field.getName());
    System.out.println("Type: " + field.getType());
    System.out.println("Value: " + field.getValue());
}
```

This is useful for:
- Building config editors
- Generating documentation
- Config validation tools
- Migration utilities

## Working with Files

### Binding Files

Multiple ways to bind a configuration file:

```java
config.configure(opt -> {
    // Using File
    opt.bindFile(new File("config.yml"));

    // Or using Path
    opt.bindFile(Paths.get("config.yml"));

    // Or using String pathname
    opt.bindFile("config.yml");
});
```

### File Locations

```java
config.configure(opt -> {
    // Relative to working directory
    opt.bindFile("config.yml");

    // Absolute path
    opt.bindFile("/etc/myapp/config.yml");

    // In subdirectory (creates parent directories automatically)
    opt.bindFile("configs/database/mysql.yml");

    // User home directory
    Path home = Paths.get(System.getProperty("user.home"));
    opt.bindFile(home.resolve("myapp/config.yml"));
});
```

### Getting Current File

```java
Path boundFile = config.getBindFile();
System.out.println("Config at: " + boundFile.toAbsolutePath());
```

## Orphan Keys

**Orphans** are keys in the configuration file that don't correspond to any field in your config class.

### Understanding Orphans

```java
// Your config class
public class MyConfig extends OkaeriConfig {
    private String newField = "value";
    // old oldField was removed from code
}
```

**Config file might have:**
```yaml
newField: value
oldField: old value  # This is an orphan!
```

### Removing Orphans

```java
config.configure(opt -> {
    opt.removeOrphans(true); // enable orphan removal
});
config.load(true); // load and save, orphans will be deleted on save
```

**Console output:**
```
WARNING: Removed orphaned (undeclared) keys: [oldField]
```

### When to Remove Orphans

- ✅ **Enable when**: Cleaning up old config files
- ✅ **Enable when**: You want strict config validation
- ❌ **Disable when**: Users may add custom keys
- ❌ **Disable when**: Using dynamic configuration

## Configurer Basics

The **Configurer** handles the actual file format (YAML, JSON, etc.):

```java
// Set configurer
config.configure(opt -> {
    opt.configurer(new YamlSnakeYamlConfigurer());
});

// Get current configurer
Configurer configurer = config.getConfigurer();
```

### Changing Configurers

You can change the format of an existing config:

```java
// Load from YAML
config.configure(opt -> {
    opt.configurer(new YamlSnakeYamlConfigurer());
    opt.bindFile("config.yml");
});
config.load(); // load only, don't save after

// Switch to JSON
config.configure(opt -> {
    opt.configurer(new JsonGsonConfigurer());
    opt.bindFile("config.json");
});
config.save();  // Now saved as JSON!
```

## Best Practices

### ✅ DO

- **Use wrapper types** (Integer, Boolean) for nullable values
- **Provide sensible defaults** in field initializers
- **Use Lombok** to reduce boilerplate
- **Use private fields** with getters/setters
- **Call load(true)** to keep config files updated
- **Use @Comment** to document configuration options

### ❌ DON'T

- **Don't remove default constructor**
- **Don't use primitives** when null is meaningful
- **Don't forget to call load()** after setup
- **Don't use final fields** (they can't be updated on load)

### Final Fields Warning

```java
public class BadConfig extends OkaeriConfig {
    // This will cause warnings!
    private final String value = "immutable";
}
```

**Console warning:**
```
WARNING: Final field 'value' cannot be updated on load
```

**Solution**: Remove `final` keyword from config fields.

## Next Steps

Now that you understand the basics:

- **[Supported Types](Supported-Types)** - Complete reference of supported field types
- **[Annotations Guide](Annotations-Guide)** - Learn about @Comment, @Header, @Variable, etc.
- **[Subconfigs & Serialization](Subconfigs-and-Serialization)** - Organize complex configs
- **[Collections & Maps](Collections-and-Maps)** - Work with lists, sets, and maps

## See Also

- **[Getting Started](Getting-Started)** - First-time setup
- **[Advanced Topics](Advanced-Topics)** - ConfigManager deep dive, custom configurers
- **[Troubleshooting](Troubleshooting)** - Common issues and solutions
