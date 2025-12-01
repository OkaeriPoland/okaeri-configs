# Okaeri Configs

![License](https://img.shields.io/github/license/OkaeriPoland/okaeri-configs)
[![Codecov](https://codecov.io/gh/OkaeriPoland/okaeri-configs/branch/master/graph/badge.svg)](https://codecov.io/gh/OkaeriPoland/okaeri-configs)
![Contributors](https://img.shields.io/github/contributors/OkaeriPoland/okaeri-configs)
[![Discord](https://img.shields.io/discord/589089838200913930)](https://discord.gg/hASN5eX)

## Supported formats

### Zero external dependencies

Uses only Java built-in APIs. Great for minimal footprint.

| Format         | Module                                                                              | Comments | Notes                            |
|----------------|-------------------------------------------------------------------------------------|----------|----------------------------------|
| **XML**        | 🌟 [xml](https://github.com/OkaeriPoland/okaeri-configs/tree/master/xml)            | ✅ Full   | Uses Java built-in XML APIs      |
| **Properties** | [properties](https://github.com/OkaeriPoland/okaeri-configs/tree/master/properties) | ✅ Full   | Flat `key=value` format          |
| **INI**        | [properties](https://github.com/OkaeriPoland/okaeri-configs/tree/master/properties) | ✅ Full   | Section-based `[section]` format |

### With external dependencies

| Format    | Module                                                                                         | Comments     | Notes                                |
|-----------|------------------------------------------------------------------------------------------------|--------------|--------------------------------------|
| **YAML**  | 🌟 [yaml-snakeyaml](https://github.com/OkaeriPoland/okaeri-configs/tree/master/yaml-snakeyaml) | ⚠️ Partial¹  | Via SnakeYAML                        |
| **YAML**  | [yaml-jackson](https://github.com/OkaeriPoland/okaeri-configs/tree/master/yaml-jackson)        | ⚠️ Partial¹  | Via Jackson (SnakeYAML underneath)   |
| **TOML**  | [toml-jackson](https://github.com/OkaeriPoland/okaeri-configs/tree/master/toml-jackson)        | ✅ Full       | TOML 1.0 via Jackson                 |
| **HJSON** | [hjson](https://github.com/OkaeriPoland/okaeri-configs/tree/master/hjson)                      | ✅ Full       | Human JSON via hjson-java            |
| **JSON**  | [json-gson](https://github.com/OkaeriPoland/okaeri-configs/tree/master/json-gson)              | ❌ None       | Via Google GSON                      |
| **JSON**  | [json-jackson](https://github.com/OkaeriPoland/okaeri-configs/tree/master/json-jackson)        | ❌ None       | Via Jackson                          |
| **JSON**  | [json-simple](https://github.com/OkaeriPoland/okaeri-configs/tree/master/json-simple)          | ❌ None       | Via json-simple, no pretty print     |
| **HOCON** | [hocon-lightbend](https://github.com/OkaeriPoland/okaeri-configs/tree/master/hocon-lightbend)  | ⚠️ Top-level | Severely limited (see module README) |

### Environment dependent

Special implementations for safe use in specific environments, e.g., game servers.

| Platform                 | Module                                                                                   | Comments    | Notes                                                                                                |
|--------------------------|------------------------------------------------------------------------------------------|-------------|------------------------------------------------------------------------------------------------------|
| **Bukkit/Spigot/Paper**  | 🌟 [yaml-bukkit](https://github.com/OkaeriPoland/okaeri-configs/tree/master/yaml-bukkit) | ⚠️ Partial¹ | No extra dependencies (best with [okaeri-platform](https://github.com/OkaeriPoland/okaeri-platform)) |
| **BungeeCord/Waterfall** | 🌟 [yaml-bungee](https://github.com/OkaeriPoland/okaeri-configs/tree/master/yaml-bungee) | ⚠️ Partial¹ | No extra dependencies needed                                                                         |
| **Velocity/Sponge**      | json-gson, hocon-lightbend, or yaml-snakeyaml                                            | Varies      | Exclude format deps when shading (provided by environment)                                           |

¹ Comments not supported on Collection/Map elements<br>
² Only header and top-level field comments (see [module README](https://github.com/OkaeriPoland/okaeri-configs/tree/master/hocon-lightbend))

## Validation extensions

- [Jakarta EE](https://github.com/OkaeriPoland/okaeri-configs/tree/master/validator-jakartaee): Jakarta EE based with full Jakarta Bean Validation support
- [Okaeri Validator](https://github.com/OkaeriPoland/okaeri-configs/tree/master/validator-okaeri): simple validator with Jakarta EE-like annotations but much less code (and features)

## Serialization extensions

- 🌟 [serdes-commons](https://github.com/OkaeriPoland/okaeri-configs/tree/master/serdes-commons): for common but not mandatory types, e.g. Instant, Pattern, Duration
- [serdes-bukkit](https://github.com/OkaeriPoland/okaeri-configs/tree/master/serdes-bukkit): for Minecraft (Bukkit) types
- [serdes-bucket4j](https://github.com/OkaeriPoland/okaeri-configs/tree/master/serdes-bucket4j): for [vladimir-bukhtoyarov/bucket4j](https://github.com/vladimir-bukhtoyarov/bucket4j) types

## Recommendations

For `standalone platforms`, the [xml](https://github.com/OkaeriPoland/okaeri-configs/tree/master/xml) module is a great choice with zero external dependencies (uses Java built-in APIs) and full comment support.
Alternatively, the [hjson](https://github.com/OkaeriPoland/okaeri-configs/tree/master/hjson) module offers a good balance of readability and small footprint, also including full comment support.
Combine either with [Okaeri Validator](https://github.com/OkaeriPoland/okaeri-configs/tree/master/validator-okaeri) for a complete config solution.

For `any platform`, if some form of config validation is applicable (e.g., requiring that an integer is positive), it is recommended to use [Okaeri Validator](https://github.com/OkaeriPoland/okaeri-configs/tree/master/validator-okaeri) when possible.
Only a few kilobytes but makes for a much better experience for both end-users and developers.

For `any platform`, if some form of i18n/translation is needed, you may be interested in [okaeri-i18n](https://github.com/OkaeriPoland/okaeri-i18n), which can use okaeri-configs as a translation source.

## Genesis

Okaeri's configuration library is an easy way to use Java classes as config adapters:

- Supports different environments with minimal hassle and relatively small footprint
- Allows for even complex types to be serialized/deserialized
- Enhances your configs with durable comments and strongly-typed fields
- Provides the ability to access typed fields with classic getters and setters
- Core library is just ~133kB in size, most of the adapters require only ~100 lines of code

## Example

```java
@Header("################################")
@Header("#   My Application Config      #")
@Header("################################")
public class AppConfig extends OkaeriConfig {

    @Comment("Application settings")
    private String appName = "MyApp";
    private Integer maxConnections = 100;

    @Variable("API_KEY")
    @Comment("API key (can be set via environment variable)")
    private String apiKey = "your-key-here";

    @Comment("Server configuration (subconfig)")
    private ServerConfig server = new ServerConfig();

    @Comment("Database settings (serializable)")
    private DatabaseConfig database = new DatabaseConfig("localhost", 5432);

    @Comment("Feature flags")
    private Map<String, Boolean> features = Map.of(
        "experimental", false,
        "logging", true
    );

    @Comment("Allowed IP addresses")
    private List<String> allowedIps = List.of("127.0.0.1", "192.168.1.1");

    // Subconfig - extends OkaeriConfig, supports nested comments
    public static class ServerConfig extends OkaeriConfig {
        private String host = "localhost";
        private Integer port = 8080;

        @Comment("Connection timeout in seconds")
        private Integer timeout = 30;
    }

    // Serializable - lighter weight, no nested comments
    @Getter @Setter @AllArgsConstructor @NoArgsConstructor
    public static class DatabaseConfig implements Serializable {
        private String host;
        private Integer port;
    }
}
```

## Usage

Pick your poison.

### With create(clazz, initializer)

```java
// recommended
TestConfig config = ConfigManager.create(TestConfig.class, it -> {
    it.configure(opt -> {
        opt.configurer(new YamlBukkitConfigurer(), new SerdesBukkit()); // specify configurer implementation, optionally additional serdes packages
        opt.bindFile(new File(this.getDataFolder(), "config.yml")); // specify Path, File or pathname
        opt.removeOrphans(true); // automatic removal of undeclared keys
    });
    it.saveDefaults(); // save file if it does not exist
    it.load(true); // load and save to update comments/new fields
});
```

### With create(clazz)

```java
TestConfig config = (TestConfig) ConfigManager.create(TestConfig.class)
    .configure(opt -> {
        opt.configurer(new YamlBukkitConfigurer(), new SerdesBukkit()); // specify configurer implementation, optionally additional serdes packages
        opt.bindFile(new File(this.getDataFolder(), "config.yml")); // specify Path, File or pathname
        opt.removeOrphans(true); // automatic removal of undeclared keys
    })
    .saveDefaults() // save file if it does not exist
    .load(true); // load and save to update comments/new fields
```

### With direct instantiation

```java
TestConfig config = new TestConfig();
config.configure(opt -> {
    opt.configurer(new YamlBukkitConfigurer(), new SerdesBukkit()); // specify configurer implementation, optionally additional serdes packages
    opt.bindFile(new File(this.getDataFolder(), "config.yml")); // specify Path, File or pathname
    opt.removeOrphans(true); // automatic removal of undeclared keys
});
config.saveDefaults(); // save file if it does not exist
config.load(true); // load and save to update comments/new fields
```

## Supported types

### Composite Types
- **Subconfigs**: Classes extending `OkaeriConfig` - supports nested comments and full config features
- **Serializable**: Classes implementing `Serializable` - lighter weight alternative to subconfigs

### Simple Types
- **Primitives**: `boolean`, `byte`, `char`, `double`, `float`, `int`, `long`, `short`
- **Wrappers**: `Boolean`, `Byte`, `Character`, `Double`, `Float`, `Integer`, `Long`, `Short`, `String`
- **Math**: `BigInteger`, `BigDecimal`
- **Enums**: Automatically serialized via `name()` with case-insensitive `valueOf()` deserialization

### Collections
- **List**: `List<T>` → `ArrayList<T>`
- **Set**: `Set<T>` → `LinkedHashSet<T>`
- **Map**: `Map<K, V>` → `LinkedHashMap<K, V>`
- Custom implementations supported if non-interface type with default constructor

### Custom Types
- Use `ObjectSerializer` for complex serialization logic
- Use `ObjectTransformer` for simple type conversions
- See platform-specific serdes modules for examples (bukkit, commons, etc.)
