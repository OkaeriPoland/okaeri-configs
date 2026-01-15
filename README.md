# Okaeri Configs

![License](https://img.shields.io/github/license/OkaeriPoland/okaeri-configs)
[![Codecov](https://codecov.io/gh/OkaeriPoland/okaeri-configs/branch/master/graph/badge.svg)](https://codecov.io/gh/OkaeriPoland/okaeri-configs)
![Contributors](https://img.shields.io/github/contributors/OkaeriPoland/okaeri-configs)
[![Discord](https://img.shields.io/discord/589089838200913930)](https://discord.gg/hASN5eX)

## Supported formats

### Zero external dependencies

Uses only Java built-in APIs. Great for minimal footprint.

| Format         | Module                                                                              | Comments | Errors | Notes                            |
|----------------|-------------------------------------------------------------------------------------|----------|--------|----------------------------------|
| **XML**        | 🌟 [xml](https://github.com/OkaeriPoland/okaeri-configs/tree/master/xml)            | ✅        | ✅      | Uses Java built-in XML APIs      |
| **Properties** | [properties](https://github.com/OkaeriPoland/okaeri-configs/tree/master/properties) | ✅        | ✅      | Flat `key=value` format          |
| **INI**        | [properties](https://github.com/OkaeriPoland/okaeri-configs/tree/master/properties) | ✅        | ✅      | Section-based `[section]` format |
| **Binary**     | [binary](https://github.com/OkaeriPoland/okaeri-configs/tree/master/binary)         | ❌        | ❌      | Java ObjectStream serialization  |

### With external dependencies

| Format    | Module                                                                                         | Comments | Errors | Notes                              |
|-----------|------------------------------------------------------------------------------------------------|----------|--------|------------------------------------|
| **YAML**  | 🌟 [yaml-snakeyaml](https://github.com/OkaeriPoland/okaeri-configs/tree/master/yaml-snakeyaml) | ✅        | ✅      | Via SnakeYAML                      |
| **YAML**  | [yaml-jackson](https://github.com/OkaeriPoland/okaeri-configs/tree/master/yaml-jackson)        | ✅        | ✅      | Via Jackson (SnakeYAML underneath) |
| **TOML**  | [toml-jackson](https://github.com/OkaeriPoland/okaeri-configs/tree/master/toml-jackson)        | ✅        | ✅      | TOML 1.0 via Jackson               |
| **HJSON** | [hjson](https://github.com/OkaeriPoland/okaeri-configs/tree/master/hjson)                      | ✅        | ❌      | Human JSON via hjson-java          |
| **JSON**  | [json-gson](https://github.com/OkaeriPoland/okaeri-configs/tree/master/json-gson)              | ❌        | ❌      | Via Google GSON                    |
| **JSON**  | [json-jackson](https://github.com/OkaeriPoland/okaeri-configs/tree/master/json-jackson)        | ❌        | ❌      | Via Jackson                        |
| **JSON**  | [json-simple](https://github.com/OkaeriPoland/okaeri-configs/tree/master/json-simple)          | ❌        | ❌      | Via json-simple, no pretty print   |

### Environment dependent

Special implementations for safe use in specific environments, e.g., game servers.

| Platform                 | Module                                                                                   | Comments | Errors | Notes                                                                                                |
|--------------------------|------------------------------------------------------------------------------------------|----------|--------|------------------------------------------------------------------------------------------------------|
| **Bukkit/Spigot/Paper**  | 🌟 [yaml-bukkit](https://github.com/OkaeriPoland/okaeri-configs/tree/master/yaml-bukkit) | ✅        | ✅      | No extra dependencies (best with [okaeri-platform](https://github.com/OkaeriPoland/okaeri-platform)) |
| **BungeeCord/Waterfall** | 🌟 [yaml-bungee](https://github.com/OkaeriPoland/okaeri-configs/tree/master/yaml-bungee) | ✅        | ✅      | No extra dependencies needed                                                                         |
| **Velocity/Sponge**      | yaml-snakeyaml or json-gson                                                              | Varies   | Varies | Exclude format deps when shading (provided by environment)                                           |

**Legend:**
- Comments: `@Comment`/`@Header` support
- Errors: Rust-style error markers (see below)

### Rust-style error messages

Supported formats provide precise error messages that pinpoint exactly where serdes failed:

```
error[DurationTransformer]: Cannot transform 'scoreboard.dummy.update-rate' to Duration from String
 --> config.yml:1118:18
     |
1114 |     # How often should all players' dummy be refreshed (regardless of other triggers)
1115 |     # Format: <value><unit><value><unit><...>
1116 |     # Units: s - seconds, m - minutes, h - hours
1117 |     # Example: 1m30s
1118 |     update-rate: hello
     |                  ^^^^^ Expected duration (e.g. 30s, 5m, 1h30m, 1d)
```

```
error[EnvironmentPlaceholderProcessor]: Cannot pre-process 'database.password' to String from String
 --> config.yml:14:13
   |
12 |   host: ${DB_HOST:localhost}
13 |   port: ${DB_PORT:5432}
14 |   password: ${DB_PASSWORD}
   |             ^^^^^^^^^^^^^^ Unresolved property or env
```

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
- Core library is just ~177kB in size, most of the adapters require only ~100 lines of code

## Example

```java
@Header("################################")
@Header("#   My Application Config      #")
@Header("################################")
public class AppConfig extends OkaeriConfig {

    @Comment("Application settings")
    private String appName = "MyApp";
    private Integer maxConnections = 100;

    @Comment("API key from properties (-DAPI_KEY=1234) or environment")
    private String apiKey = "${API_KEY}";
    // use ${API_KEY:default} for fallback value
    // or @Variable("API_KEY") annotation with field value as fallback

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
        opt.resolvePlaceholders(); // resolve ${VAR} and ${VAR:default} from environment
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
        opt.resolvePlaceholders(); // resolve ${VAR} and ${VAR:default} from environment
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
    opt.resolvePlaceholders(); // resolve ${VAR} and ${VAR:default} from properties/environment
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
