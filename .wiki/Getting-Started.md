# Getting Started

This guide will help you set up Okaeri Configs in your project and create your first configuration.

## Installation

### Prerequisites

- Java 8 or higher
- Maven or Gradle build system

### Choose Your Format

Okaeri Configs supports multiple configuration formats. Choose the one that best fits your needs:

| Format            | Module                                              | Best For                                    |
|-------------------|-----------------------------------------------------|---------------------------------------------|
| **YAML**          | `okaeri-configs-yaml-snakeyaml`                     | General purpose, widely recognized          |
| **XML/INI**       | `okaeri-configs-xml` or `okaeri-configs-properties` | Standalone apps, zero external dependencies |
| **JSON**          | `okaeri-configs-json-gson`                          | In-app storage, GSON users                  |
| **YAML (Bukkit)** | `okaeri-configs-yaml-bukkit`                        | Minecraft Bukkit/Spigot/Paper plugins       |
| **YAML (Bungee)** | `okaeri-configs-yaml-bungee`                        | Minecraft BungeeCord/Waterfall plugins      |

### Maven Setup

1. **Add the repository:**

```xml
<repositories>
    <repository>
        <id>okaeri-releases</id>
        <url>https://repo.okaeri.cloud/releases</url>
    </repository>
</repositories>
```

2. **Add the dependency** (example with YAML SnakeYAML):

```xml
<dependencies>
    <dependency>
        <groupId>eu.okaeri</groupId>
        <artifactId>okaeri-configs-yaml-snakeyaml</artifactId>
        <version>{VERSION}</version>
    </dependency>
</dependencies>
```

### Gradle Setup (Kotlin DSL)

1. **Add the repository:**

```kotlin
repositories {
    maven("https://repo.okaeri.cloud/releases")
}
```

2. **Add the dependency** (example with YAML SnakeYAML):

```kotlin
dependencies {
    implementation("eu.okaeri:okaeri-configs-yaml-snakeyaml:{VERSION}")
}
```

### Gradle Setup (Groovy DSL)

1. **Add the repository:**

```groovy
repositories {
    maven { url "https://repo.okaeri.cloud/releases" }
}
```

2. **Add the dependency** (example with YAML SnakeYAML):

```groovy
dependencies {
    implementation 'eu.okaeri:okaeri-configs-yaml-snakeyaml:{VERSION}'
}
```

## Your First Config

### Step 1: Create a Config Class

Create a class that extends `OkaeriConfig`:

```java
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.Header;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Header("My Application Configuration")
public class MyConfig extends OkaeriConfig {

    @Comment("The name of your application")
    private String applicationName = "MyApp";

    @Comment("Maximum number of concurrent users")
    private int maxUsers = 100;

    @Comment("Enable debug mode")
    private boolean debug = false;
}
```

> 💡 **Tip**: Using Lombok with `@Getter` and `@Setter` annotations can reduce boilerplate significantly! All examples in this wiki use Lombok. See **[Using Lombok](Using-Lombok)** for installation and setup guide.

### Step 2: Initialize and Use the Config

#### Method 1: Using ConfigManager.create with Initializer (Recommended)

```java
import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;
import java.io.File;

public class Main {
    public static void main(String[] args) {

        MyConfig config = ConfigManager.create(MyConfig.class, (it) -> {
            it.withConfigurer(new YamlSnakeYamlConfigurer()); // Set format
            it.withBindFile(new File("config.yml"));           // Set file location
            it.saveDefaults();                                  // Create file if it doesn't exist
            it.load(true);                                      // Load and update with comments
        });

        // Use the config
        System.out.println("App Name: " + config.getApplicationName());
        System.out.println("Max Users: " + config.getMaxUsers());

        // Modify and save
        config.setMaxUsers(200);
        config.save();
    }
}
```

#### Method 2: Using ConfigManager.create with Chaining

```java
MyConfig config = (MyConfig) ConfigManager.create(MyConfig.class)
    .withConfigurer(new YamlSnakeYamlConfigurer())
    .withBindFile(new File("config.yml"))
    .saveDefaults()
    .load(true);

// Use the config
String appName = config.getApplicationName();
```

### Step 3: Check the Generated File

After running your code, you'll find a `config.yml` file with this content:

```yaml
# My Application Configuration

# The name of your application
applicationName: MyApp

# Maximum number of concurrent users
maxUsers: 100

# Enable debug mode
debug: false
```

## Configuration Lifecycle

Understanding the config lifecycle is important:

1. **Create**: `ConfigManager.create()` instantiates your config class
2. **Configure**: `withConfigurer()` sets the file format handler
3. **Bind**: `withBindFile()` associates a file with the config
4. **Save Defaults**: `saveDefaults()` creates the file with default values if it doesn't exist
5. **Load**: `load()` reads the file and populates your config object
6. **Update**: `load(true)` also saves after loading to update comments and new fields

## Common Patterns

### Pattern 1: Config with File Creation

```java
// Creates file with defaults if missing, then loads it
MyConfig config = ConfigManager.create(MyConfig.class, (it) -> {
    it.withConfigurer(new YamlSnakeYamlConfigurer());
    it.withBindFile("config.yml");
    it.saveDefaults();
    it.load();
});
```

### Pattern 2: Config with Auto-Update

```java
// Creates file, loads it, and updates with any new fields/comments
MyConfig config = ConfigManager.create(MyConfig.class, (it) -> {
    it.withConfigurer(new YamlSnakeYamlConfigurer());
    it.withBindFile("config.yml");
    it.saveDefaults();
    it.load(true);  // true = save after loading
});
```

### Pattern 3: Config with Orphan Removal

```java
// Removes keys from file that don't exist in your config class
MyConfig config = ConfigManager.create(MyConfig.class, (it) -> {
    it.withConfigurer(new YamlSnakeYamlConfigurer());
    it.withBindFile("config.yml");
    it.withRemoveOrphans(true);  // Remove undeclared keys
    it.saveDefaults();
    it.load(true);
});
```

## Format-Specific Configurers

Depending on your chosen format, use the appropriate configurer:

```java
// YAML
new YamlSnakeYamlConfigurer()      // General purpose
new YamlJacksonConfigurer()        // Via Jackson
new YamlBukkitConfigurer()         // Bukkit/Spigot/Paper
new YamlBungeeConfigurer()         // BungeeCord/Waterfall

// TOML
new TomlJacksonConfigurer()        // TOML 1.0 via Jackson

// JSON
new JsonGsonConfigurer()           // Via Google GSON
new JsonJacksonConfigurer()        // Via Jackson
new JsonSimpleConfigurer()         // Via json-simple

// HJSON
new HjsonConfigurer()              // Human-friendly JSON

// XML
new XmlSimpleConfigurer()          // Human-readable XML

// Properties/INI (zero dependencies)
new PropertiesConfigurer()         // key=value format
new IniConfigurer()                // [section] format
```

## Using Path Instead of File

You can use `java.nio.file.Path` instead of `File`:

```java
import java.nio.file.Path;
import java.nio.file.Paths;

Path configPath = Paths.get("config.yml");

MyConfig config = ConfigManager.create(MyConfig.class, (it) -> {
    it.withConfigurer(new YamlSnakeYamlConfigurer());
    it.withBindFile(configPath);
    it.saveDefaults();
    it.load(true);
});
```

Or even use a string pathname directly:

```java
MyConfig config = ConfigManager.create(MyConfig.class, (it) -> {
    it.withConfigurer(new YamlSnakeYamlConfigurer());
    it.withBindFile("configs/myapp.yml");  // String pathname
    it.saveDefaults();
    it.load(true);
});
```

## Common Pitfalls

### ❌ No Default Constructor

```java
// This will fail!
public class MyConfig extends OkaeriConfig {
    public MyConfig(String name) {  // Custom constructor
        // ...
    }
}
```

**Solution**: Always provide a no-args constructor (or don't define any constructors):

```java
public class MyConfig extends OkaeriConfig {
    // No constructor = default constructor is implicit
}
```

### ❌ Using Primitives for Nullable Values

```java
private int port;  // Will default to 0, can't be null
```

**Solution**: Use wrapper classes when null is a valid value:

```java
private Integer port;  // Can be null
```

### ❌ Forgetting to Call load()

```java
MyConfig config = ConfigManager.create(MyConfig.class, (it) -> {
    it.withConfigurer(new YamlSnakeYamlConfigurer());
    it.withBindFile("config.yml");
    // Forgot to load!
});
// Config has only default values, file contents are ignored
```

**Solution**: Always call `load()` or `load(true)`:

```java
it.load(true);
```

### ❌ Using transient Fields for Config Values

```java
private transient String apiKey;  // Will NOT be saved/loaded!
```

**Solution**: Only use `transient` for fields you don't want in the config file (runtime data only).

## Next Steps

Now that you have a basic config working, explore these topics:

- **[Configuration Basics](Configuration-Basics)** - Learn about all supported types and features
- **[Annotations Guide](Annotations-Guide)** - Master @Comment, @Header, @Variable, and more
- **[Subconfigs & Serialization](Subconfigs-and-Serialization)** - Organize complex configurations
- **[Collections & Maps](Collections-and-Maps)** - Work with lists, sets, and maps
- **[Validation](Validation)** - Add validation to your configs
- **[Examples & Recipes](Examples-and-Recipes)** - See more complete examples

## Troubleshooting

Having issues? Check the **[Troubleshooting](Troubleshooting)** guide or ask for help on our [Discord](https://discord.gg/hASN5eX).
