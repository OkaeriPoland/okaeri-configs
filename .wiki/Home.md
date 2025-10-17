# Okaeri Configs Wiki

> ⚠️ **Note**: This wiki has been primarily generated and maintained using AI assistance. While we strive for accuracy, please report any errors or inconsistencies in the [GitHub Issues](https://github.com/OkaeriPoland/okaeri-configs/issues).

Welcome to the comprehensive documentation for **Okaeri Configs** - a powerful, lightweight Java configuration library that makes working with configuration files simple and type-safe.

## What is Okaeri Configs?

Okaeri Configs is a modern Java configuration library that allows you to use Java classes as configuration adapters. It provides a clean, annotation-based approach to managing application settings across multiple formats and platforms.

### Key Features

- 🎯 **Type-Safe Configurations**: Use Java classes with getters/setters for compile-time safety
- 💬 **Comment Support**: Add persistent comments and headers to your config files
- 🔄 **Multiple Formats**: YAML, JSON, HJSON, HOCON support out of the box
- 🎮 **Platform Support**: Special integrations for Bukkit, Bungee, and more
- 📦 **Lightweight**: Core library is only ~129kB
- ✅ **Validation**: Built-in validation support (Okaeri Validator, Jakarta EE)
- 🔌 **Extensible**: Custom serializers, transformers, and format support
- 🌍 **Environment Variables**: Built-in support for environment variable substitution

### Quick Example

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

    @Comment("Server configuration")
    private ServerConfig server = new ServerConfig();

    public static class ServerConfig extends OkaeriConfig {
        private String host = "localhost";
        private Integer port = 8080;
    }
}
```

**Usage:**

```java
AppConfig config = ConfigManager.create(AppConfig.class, (it) -> {
    it.withConfigurer(new YamlSnakeYamlConfigurer());
    it.withBindFile(new File("config.yml"));
    it.saveDefaults();
    it.load(true);
});

// Access with type safety
String appName = config.getAppName();
config.setMaxConnections(200);
config.save();
```

## Documentation Structure

### Getting Started
- **[Getting Started](Getting-Started)** - Installation, first config, basic concepts
- **[Configuration Basics](Configuration-Basics)** - Understanding OkaeriConfig fundamentals
- **[Quick Start Examples](Examples-and-Recipes)** - Common use cases and patterns

### Core Concepts
- **[Annotations Guide](Annotations-Guide)** - @Header, @Comment, @Variable, and more
- **[Supported Types](Supported-Types)** - What types can be used in configs
- **[Subconfigs & Serialization](Subconfigs-and-Serialization)** - Nested configurations
- **[Collections & Maps](Collections-and-Maps)** - Working with complex data structures

### Advanced Features
- **[Validation](Validation)** - Config validation with annotations
- **[Serdes Extensions](Serdes-Extensions)** - Custom serialization/deserialization
- **[Migrations](Migrations)** - Updating configs between versions
- **[Advanced Topics](Advanced-Topics)** - Custom serializers, transformers, post-processing

### Help & Reference
- **[Troubleshooting](Troubleshooting)** - Common issues and solutions
- **[Examples & Recipes](Examples-and-Recipes)** - Real-world usage patterns

## Quick Links

### Installation

**Maven:**
```xml
<repository>
    <id>okaeri-repo</id>
    <url>https://storehouse.okaeri.eu/repository/maven-public/</url>
</repository>

<dependency>
    <groupId>eu.okaeri</groupId>
    <artifactId>okaeri-configs-yaml-snakeyaml</artifactId>
    <version>5.0.13</version>
</dependency>
```

**Gradle (Kotlin DSL):**
```kotlin
maven("https://storehouse.okaeri.eu/repository/maven-public/")
implementation("eu.okaeri:okaeri-configs-yaml-snakeyaml:5.0.13")
```

See **[Getting Started](Getting-Started)** for detailed installation instructions.

### Common Configurations

| Use Case | Format | Additional Dependencies |
|----------|--------|------------------------|
| **Standalone Apps** | HJSON (~193kB total) | `okaeri-configs-hjson` + `okaeri-configs-validator-okaeri` |
| **Bukkit Plugins** | YAML (Bukkit) | `okaeri-configs-yaml-bukkit` + `okaeri-configs-serdes-bukkit` |
| **Bungee Plugins** | YAML (Bungee) | `okaeri-configs-yaml-bungee` |
| **General Purpose** | YAML (SnakeYAML) | `okaeri-configs-yaml-snakeyaml` |

## Community & Support

- 🐛 **Bug Reports**: [GitHub Issues](https://github.com/OkaeriPoland/okaeri-configs/issues)
- 💬 **Discord**: [Join our Discord](https://discord.gg/hASN5eX)
- 📖 **Source Code**: [GitHub Repository](https://github.com/OkaeriPoland/okaeri-configs)
- 📦 **Maven Repository**: [Okaeri Storehouse](https://storehouse.okaeri.eu/repository/maven-public/)

## Contributing to the Wiki

Found an error or want to improve the documentation?

1. Visit the [GitHub repository](https://github.com/OkaeriPoland/okaeri-configs)
2. Navigate to the `.wiki/` directory
3. Submit a pull request with your improvements

---

**Ready to get started?** Head over to **[Getting Started](Getting-Started)** to create your first config!
