# Examples & Recipes

Practical examples and real-world usage patterns for okaeri-configs.

## Table of Contents

- [Complete Application Config](#complete-application-config)
- [Database Configuration](#database-configuration)
- [Multi-Server Setup](#multi-server-setup)
- [Feature Flags System](#feature-flags-system)
- [Bukkit Plugin Config](#bukkit-plugin-config)
- [BungeeCord Plugin Config](#bungeecord-plugin-config)
- [API Client Configuration](#api-client-configuration)
- [i18n/Translation Config](#i18ntranslation-config)
- [Complex Nested Structures](#complex-nested-structures)
- [Migration Recipe](#migration-recipe)

## Complete Application Config

Full-featured application configuration with validation, environment variables, and subconfigs.

```java
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.*;
import eu.okaeri.validator.annotation.*;
import lombok.*;

@Header("################################")
@Header("#   Application Configuration  #")
@Header("################################")
@Getter
@Setter
public class AppConfig extends OkaeriConfig {

    @Comment("Application metadata")
    private AppMetadata app = new AppMetadata();

    @Comment("Server configuration")
    private ServerConfig server = new ServerConfig();

    @Comment("Database settings")
    private DatabaseConfig database = new DatabaseConfig();

    @Comment("Security settings")
    private SecurityConfig security = new SecurityConfig();

    @Comment("Feature flags")
    private Map<String, Boolean> features = Map.of(
        "experimentalFeatures", false,
        "advancedLogging", true,
        "metricsCollection", true
    );

    @Getter
    @Setter
    public static class AppMetadata extends OkaeriConfig {
        private String name = "MyApp";
        private String version = "1.0.0";

        @Comment("Environment: development, staging, production")
        @Pattern("development|staging|production")
        private String environment = "development";
    }

    @Getter
    @Setter
    public static class ServerConfig extends OkaeriConfig {
        @NotBlank
        private String host = "0.0.0.0";

        @Min(1) @Max(65535)
        private Integer port = 8080;

        @Min(1) @Max(3600)
        private Integer timeout = 30;

        @Min(1) @Max(10000)
        private Integer maxConnections = 100;
    }

    @Getter
    @Setter
    public static class DatabaseConfig extends OkaeriConfig {
        @NotBlank
        private String url = "jdbc:postgresql://localhost:5432/myapp";

        @NotBlank
        private String username = "postgres";

        @Variable("DB_PASSWORD")
        @Comment("Database password (can be set via DB_PASSWORD env var)")
        private String password = "changeme";

        @Min(1) @Max(100)
        private Integer poolSize = 10;

        @Min(1) @Max(60)
        private Integer connectionTimeout = 10;
    }

    @Getter
    @Setter
    public static class SecurityConfig extends OkaeriConfig {
        @Variable("API_KEY")
        @NotBlank
        private String apiKey = "your-api-key-here";

        @Variable("JWT_SECRET")
        @Size(min = 32)
        private String jwtSecret = "change-this-to-a-secure-random-secret";

        @Min(300) @Max(86400)
        private Integer sessionTimeout = 3600;

        private List<String> allowedOrigins = List.of(
            "http://localhost:3000",
            "https://app.example.com"
        );
    }
}
```

**Usage:**

```java
import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;
import eu.okaeri.configs.validator.okaeri.OkaeriValidator;

public class Application {
    public static void main(String[] args) {
        AppConfig config = ConfigManager.create(AppConfig.class, (it) -> {
            it.withConfigurer(new OkaeriValidator(new YamlSnakeYamlConfigurer()));
            it.withBindFile("config.yml");
            it.withRemoveOrphans(true);
            it.saveDefaults();
            it.load(true);
        });

        // Use configuration
        System.out.println("Starting " + config.getApp().getName());
        System.out.println("Server: " + config.getServer().getHost() + ":" + config.getServer().getPort());
        System.out.println("Environment: " + config.getApp().getEnvironment());
    }
}
```

## Database Configuration

Comprehensive database configuration with connection pooling and multiple database support.

```java
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.*;
import eu.okaeri.validator.annotation.*;
import lombok.*;

@Getter
@Setter
public class DatabaseConfig extends OkaeriConfig {

    @Comment("Database type: mysql, postgresql, sqlite, h2")
    @Pattern("mysql|postgresql|sqlite|h2")
    private String type = "mysql";

    @Comment("Connection settings")
    private ConnectionSettings connection = new ConnectionSettings();

    @Comment("Connection pool settings")
    private PoolSettings pool = new PoolSettings();

    @Comment("Query settings")
    private QuerySettings query = new QuerySettings();

    @Getter
    @Setter
    public static class ConnectionSettings extends OkaeriConfig {
        @NotBlank
        private String host = "localhost";

        @Min(1) @Max(65535)
        private Integer port = 3306;

        @NotBlank
        private String database = "myapp";

        @NotBlank
        private String username = "root";

        @Variable("DB_PASSWORD")
        private String password = "";

        @Comment("Additional JDBC parameters")
        private Map<String, String> parameters = Map.of(
            "useSSL", "false",
            "autoReconnect", "true",
            "characterEncoding", "utf8"
        );
    }

    @Getter
    @Setter
    public static class PoolSettings extends OkaeriConfig {
        @Min(1) @Max(100)
        private Integer minimumIdle = 5;

        @Min(1) @Max(100)
        private Integer maximumPoolSize = 10;

        @Min(1000) @Max(600000)
        private Integer connectionTimeout = 30000;

        @Min(1000) @Max(1800000)
        private Integer idleTimeout = 600000;

        @Min(1000) @Max(1800000)
        private Integer maxLifetime = 1800000;
    }

    @Getter
    @Setter
    public static class QuerySettings extends OkaeriConfig {
        @Min(1) @Max(3600)
        private Integer queryTimeout = 30;

        private Boolean logSlowQueries = true;

        @Min(100) @Max(60000)
        private Integer slowQueryThreshold = 1000;
    }

    // Helper method to build JDBC URL
    public String buildJdbcUrl() {
        String baseUrl = switch (type) {
            case "mysql" -> "jdbc:mysql://" + connection.getHost() + ":" + connection.getPort() + "/" + connection.getDatabase();
            case "postgresql" -> "jdbc:postgresql://" + connection.getHost() + ":" + connection.getPort() + "/" + connection.getDatabase();
            case "sqlite" -> "jdbc:sqlite:" + connection.getDatabase() + ".db";
            case "h2" -> "jdbc:h2:./" + connection.getDatabase();
            default -> throw new IllegalStateException("Unknown database type: " + type);
        };

        if (connection.getParameters().isEmpty()) {
            return baseUrl;
        }

        String params = connection.getParameters().entrySet().stream()
            .map(e -> e.getKey() + "=" + e.getValue())
            .collect(Collectors.joining("&"));

        return baseUrl + "?" + params;
    }
}
```

## Multi-Server Setup

Configuration for managing multiple servers or environments.

```java
@Getter
@Setter
public class MultiServerConfig extends OkaeriConfig {

    @Comment("Active server configuration")
    private String activeServer = "primary";

    @Comment("Server definitions")
    private Map<String, ServerDef> servers = Map.of(
        "primary", new ServerDef("primary.example.com", 8080, "us-east-1"),
        "secondary", new ServerDef("secondary.example.com", 8080, "us-west-2"),
        "development", new ServerDef("localhost", 8080, "local")
    );

    @Comment("Load balancing settings")
    private LoadBalancerConfig loadBalancer = new LoadBalancerConfig();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServerDef implements Serializable {
        private String host;
        private Integer port;
        private String region;
    }

    @Getter
    @Setter
    public static class LoadBalancerConfig extends OkaeriConfig {
        @Comment("Strategy: round-robin, least-connections, ip-hash")
        @Pattern("round-robin|least-connections|ip-hash")
        private String strategy = "round-robin";

        @Min(1) @Max(10)
        private Integer maxRetries = 3;

        @Min(100) @Max(10000)
        private Integer retryDelay = 1000;

        private Boolean healthCheckEnabled = true;

        @Min(1) @Max(300)
        private Integer healthCheckInterval = 30;
    }

    // Helper method to get active server
    public ServerDef getActiveServerDef() {
        ServerDef server = servers.get(activeServer);
        if (server == null) {
            throw new IllegalStateException("Active server '" + activeServer + "' not found in configuration");
        }
        return server;
    }
}
```

## Feature Flags System

Advanced feature flags with user/group targeting.

```java
@Getter
@Setter
public class FeatureFlagsConfig extends OkaeriConfig {

    @Comment("Feature flag definitions")
    private Map<String, FeatureFlag> features = new LinkedHashMap<>();

    public FeatureFlagsConfig() {
        // Initialize with some default flags
        features.put("newUI", new FeatureFlag(
            "New UI Design",
            false,
            List.of("beta-testers"),
            null
        ));

        features.put("experimentalAPI", new FeatureFlag(
            "Experimental API Endpoints",
            false,
            null,
            List.of("admin@example.com")
        ));

        features.put("darkMode", new FeatureFlag(
            "Dark Mode Theme",
            true,
            null,
            null
        ));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeatureFlag implements Serializable {
        @Comment("Feature description")
        private String description;

        @Comment("Is feature enabled globally?")
        private Boolean enabled;

        @Comment("Enabled for these user groups (null = all groups)")
        private List<String> enabledForGroups;

        @Comment("Enabled for these specific users (null = all users)")
        private List<String> enabledForUsers;
    }

    // Helper methods
    public boolean isEnabled(String featureName) {
        FeatureFlag flag = features.get(featureName);
        return flag != null && flag.getEnabled();
    }

    public boolean isEnabledForUser(String featureName, String userId, List<String> userGroups) {
        FeatureFlag flag = features.get(featureName);
        if (flag == null || !flag.getEnabled()) {
            return false;
        }

        // Check user-specific access
        if (flag.getEnabledForUsers() != null) {
            return flag.getEnabledForUsers().contains(userId);
        }

        // Check group-based access
        if (flag.getEnabledForGroups() != null) {
            return userGroups.stream()
                .anyMatch(group -> flag.getEnabledForGroups().contains(group));
        }

        // Enabled for all
        return true;
    }
}
```

## Bukkit Plugin Config

Complete Minecraft Bukkit/Spigot plugin configuration.

```java
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.*;
import eu.okaeri.configs.yaml.bukkit.YamlBukkitConfigurer;
import eu.okaeri.configs.yaml.bukkit.serdes.SerdesBukkit;
import eu.okaeri.validator.annotation.*;
import lombok.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@Header("################################")
@Header("#  MyPlugin Configuration      #")
@Header("################################")
@Getter
@Setter
public class PluginConfig extends OkaeriConfig {

    @Comment("Plugin settings")
    private PluginSettings plugin = new PluginSettings();

    @Comment("Spawn settings")
    private SpawnSettings spawn = new SpawnSettings();

    @Comment("Economy settings")
    private EconomySettings economy = new EconomySettings();

    @Comment("Messages")
    private Messages messages = new Messages();

    @Getter
    @Setter
    public static class PluginSettings extends OkaeriConfig {
        @Comment("Enable debug mode")
        private Boolean debug = false;

        @Comment("Auto-save interval (minutes)")
        @Min(1) @Max(60)
        private Integer autoSaveInterval = 5;

        @Comment("Language: en, es, fr, de, pl")
        @Pattern("en|es|fr|de|pl")
        private String language = "en";
    }

    @Getter
    @Setter
    public static class SpawnSettings extends OkaeriConfig {
        @Comment("Spawn location")
        private Location location;

        @Comment("Teleport to spawn on join")
        private Boolean teleportOnJoin = true;

        @Comment("Teleport to spawn on death")
        private Boolean teleportOnDeath = false;

        @Comment("Effects applied at spawn")
        private List<PotionEffect> effects = List.of(
            new PotionEffect(PotionEffectType.REGENERATION, 100, 1),
            new PotionEffect(PotionEffectType.SATURATION, 100, 0)
        );

        @Comment("Starter items")
        private List<ItemStack> starterItems = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class EconomySettings extends OkaeriConfig {
        @Comment("Enable economy features")
        private Boolean enabled = true;

        @Comment("Starting balance")
        @Min(0) @Max(1000000)
        private Double startingBalance = 1000.0;

        @Comment("Currency symbol")
        @NotBlank
        private String currencySymbol = "$";

        @Comment("Currency name (singular)")
        private String currencyName = "dollar";

        @Comment("Currency name (plural)")
        private String currencyNamePlural = "dollars";
    }

    @Getter
    @Setter
    public static class Messages extends OkaeriConfig {
        private String prefix = "&7[&6MyPlugin&7]&r";
        private String noPermission = "{prefix} &cYou don't have permission!";
        private String playerNotFound = "{prefix} &cPlayer not found: {player}";
        private String welcomeMessage = "{prefix} &aWelcome to the server, {player}!";
        private String spawned = "{prefix} &aTeleported to spawn!";
    }
}
```

**Plugin initialization:**

```java
import org.bukkit.plugin.java.JavaPlugin;
import eu.okaeri.configs.ConfigManager;

public class MyPlugin extends JavaPlugin {

    private PluginConfig config;

    @Override
    public void onEnable() {
        this.config = ConfigManager.create(PluginConfig.class, (it) -> {
            it.withConfigurer(new YamlBukkitConfigurer(), new SerdesBukkit());
            it.withBindFile(new File(getDataFolder(), "config.yml"));
            it.withRemoveOrphans(true);
            it.saveDefaults();
            it.load(true);
        });

        getLogger().info("Loaded configuration for " + config.getPlugin().getLanguage());
    }
}
```

## BungeeCord Plugin Config

BungeeCord/Waterfall proxy plugin configuration.

```java
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.*;
import eu.okaeri.configs.yaml.bungee.YamlBungeeConfigurer;
import eu.okaeri.configs.yaml.bukkit.serdes.SerdesBungee;
import lombok.*;
import net.md_5.bungee.api.ChatColor;

@Getter
@Setter
public class ProxyConfig extends OkaeriConfig {

    @Comment("Proxy settings")
    private ProxySettings proxy = new ProxySettings();

    @Comment("Server definitions")
    private Map<String, ServerDef> servers = new LinkedHashMap<>();

    @Comment("Messages")
    private Messages messages = new Messages();

    public ProxyConfig() {
        // Initialize default servers
        servers.put("lobby", new ServerDef("Lobby", "lobby.example.com", 25565, 100));
        servers.put("survival", new ServerDef("Survival", "survival.example.com", 25565, 50));
        servers.put("creative", new ServerDef("Creative", "creative.example.com", 25565, 30));
    }

    @Getter
    @Setter
    public static class ProxySettings extends OkaeriConfig {
        @Comment("Default server for new connections")
        private String defaultServer = "lobby";

        @Comment("Enable server list ping")
        private Boolean serverListPing = true;

        @Comment("MOTD (Message of the Day)")
        private List<String> motd = List.of(
            "&6&lMy Network",
            "&7Welcome to our server!"
        );

        @Min(1) @Max(1000)
        private Integer maxPlayers = 100;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServerDef implements Serializable {
        private String displayName;
        private String address;
        private Integer port;
        private Integer maxPlayers;
    }

    @Getter
    @Setter
    public static class Messages extends OkaeriConfig {
        private ChatColor primaryColor = ChatColor.GOLD;
        private ChatColor secondaryColor = ChatColor.GRAY;
        private ChatColor errorColor = ChatColor.RED;

        private String joinMessage = "{primary}[+] {secondary}{player} joined the network";
        private String leaveMessage = "{primary}[-] {secondary}{player} left the network";
        private String switchServer = "{primary}Connecting to {secondary}{server}{primary}...";
        private String serverOffline = "{error}The server is currently offline!";
    }
}
```

## API Client Configuration

Configuration for external API integrations.

```java
@Getter
@Setter
public class ApiConfig extends OkaeriConfig {

    @Comment("API endpoints")
    private Map<String, ApiEndpoint> endpoints = new LinkedHashMap<>();

    @Comment("Global settings")
    private GlobalSettings global = new GlobalSettings();

    public ApiConfig() {
        // Initialize default endpoints
        endpoints.put("users", new ApiEndpoint(
            "https://api.example.com/v1/users",
            "GET",
            Map.of("Content-Type", "application/json"),
            null,
            30
        ));

        endpoints.put("createUser", new ApiEndpoint(
            "https://api.example.com/v1/users",
            "POST",
            Map.of(
                "Content-Type", "application/json",
                "Authorization", "Bearer {apiKey}"
            ),
            "{\"name\": \"{name}\", \"email\": \"{email}\"}",
            30
        ));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApiEndpoint implements Serializable {
        private String url;

        @Comment("HTTP method: GET, POST, PUT, DELETE, PATCH")
        private String method;

        @Comment("Request headers")
        private Map<String, String> headers;

        @Comment("Request body template (null for GET)")
        private String bodyTemplate;

        @Comment("Timeout in seconds")
        private Integer timeout;
    }

    @Getter
    @Setter
    public static class GlobalSettings extends OkaeriConfig {
        @Variable("API_KEY")
        @NotBlank
        private String apiKey = "your-api-key-here";

        @Min(1) @Max(10)
        private Integer maxRetries = 3;

        @Min(100) @Max(30000)
        private Integer retryDelay = 1000;

        @Comment("Enable request/response logging")
        private Boolean loggingEnabled = false;

        @Comment("Rate limiting (requests per minute)")
        @Min(1) @Max(10000)
        private Integer rateLimit = 60;
    }
}
```

## i18n/Translation Config

Multi-language support configuration.

```java
@Getter
@Setter
public class TranslationsConfig extends OkaeriConfig {

    @Comment("Default language")
    private String defaultLanguage = "en";

    @Comment("Available languages")
    private List<String> availableLanguages = List.of("en", "es", "fr", "de", "pl");

    @Comment("Translations")
    private Map<String, Map<String, String>> translations = new LinkedHashMap<>();

    public TranslationsConfig() {
        // Initialize with default translations
        translations.put("en", Map.of(
            "welcome", "Welcome, {player}!",
            "goodbye", "Goodbye, {player}!",
            "error.notFound", "Not found: {item}",
            "error.noPermission", "You don't have permission!",
            "success.saved", "Successfully saved!"
        ));

        translations.put("es", Map.of(
            "welcome", "¡Bienvenido, {player}!",
            "goodbye", "¡Adiós, {player}!",
            "error.notFound", "No encontrado: {item}",
            "error.noPermission", "¡No tienes permiso!",
            "success.saved", "¡Guardado exitosamente!"
        ));

        translations.put("fr", Map.of(
            "welcome", "Bienvenue, {player}!",
            "goodbye", "Au revoir, {player}!",
            "error.notFound", "Introuvable: {item}",
            "error.noPermission", "Vous n'avez pas la permission!",
            "success.saved", "Enregistré avec succès!"
        ));
    }

    // Helper method to get translation
    public String get(String language, String key, Object... replacements) {
        Map<String, String> langMap = translations.get(language);
        if (langMap == null) {
            langMap = translations.get(defaultLanguage);
        }

        String message = langMap.get(key);
        if (message == null) {
            return key;  // Return key if translation not found
        }

        // Simple placeholder replacement
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                String placeholder = "{" + replacements[i] + "}";
                String value = String.valueOf(replacements[i + 1]);
                message = message.replace(placeholder, value);
            }
        }

        return message;
    }
}
```

**Usage:**

```java
TranslationsConfig i18n = ConfigManager.create(TranslationsConfig.class, ...);

// Get translation
String welcome = i18n.get("es", "welcome", "player", "Steve");
// → "¡Bienvenido, Steve!"

String error = i18n.get("fr", "error.notFound", "item", "Diamond Sword");
// → "Introuvable: Diamond Sword"
```

## Complex Nested Structures

Example of deeply nested configuration structures.

```java
@Getter
@Setter
public class GameConfig extends OkaeriConfig {

    @Comment("Game world definitions")
    private Map<String, World> worlds = new LinkedHashMap<>();

    public GameConfig() {
        // Initialize default world
        World overworld = new World();
        overworld.setName("Overworld");
        overworld.setDifficulty("normal");

        // Add biomes
        Biome plains = new Biome();
        plains.setTemperature(0.8);
        plains.setHumidity(0.4);
        plains.getMobs().put("passive", List.of("Cow", "Sheep", "Pig"));
        plains.getMobs().put("hostile", List.of("Zombie", "Skeleton", "Creeper"));

        overworld.getBiomes().put("plains", plains);
        worlds.put("overworld", overworld);
    }

    @Getter
    @Setter
    public static class World extends OkaeriConfig {
        private String name;

        @Pattern("peaceful|easy|normal|hard")
        private String difficulty;

        private Map<String, Biome> biomes = new LinkedHashMap<>();

        private WorldSettings settings = new WorldSettings();
    }

    @Getter
    @Setter
    public static class Biome extends OkaeriConfig {
        @Min(-1.0) @Max(2.0)
        private Double temperature;

        @Min(0.0) @Max(1.0)
        private Double humidity;

        private Map<String, List<String>> mobs = new LinkedHashMap<>();

        private Map<String, StructureSettings> structures = new LinkedHashMap<>();
    }

    @Getter
    @Setter
    public static class WorldSettings extends OkaeriConfig {
        private Boolean pvp = true;
        private Boolean mobSpawning = true;
        private Boolean mobGriefing = false;
        private Integer spawnRadius = 10;
    }

    @Getter
    @Setter
    public static class StructureSettings extends OkaeriConfig {
        private Boolean enabled = true;

        @Min(1) @Max(1000)
        private Integer spacing = 32;

        @Min(1) @Max(1000)
        private Integer separation = 8;
    }
}
```

## Migration Recipe

Simple recipe for migrating from old config structure to new one.

> 💡 **See also**: **[Migrations](Migrations)** - Complete guide to config migrations with DSL helpers, named migrations, and best practices.

```java
@Getter
@Setter
public class MigratingConfig extends OkaeriConfig {

    // Version tracking
    @Comment("Config version (DO NOT EDIT)")
    private Integer configVersion = 2;

    // New structure (v2)
    @Comment("Server settings")
    private ServerSettings server = new ServerSettings();

    // Old fields (v1) - marked for removal
    @Exclude  // Don't write to file
    @CustomKey("server-host")
    private String oldServerHost;

    @Exclude
    @CustomKey("server-port")
    private Integer oldServerPort;

    @Exclude
    @CustomKey("max-players")
    private Integer oldMaxPlayers;

    @Getter
    @Setter
    public static class ServerSettings extends OkaeriConfig {
        private String host = "localhost";

        @Min(1) @Max(65535)
        private Integer port = 25565;

        @Min(1) @Max(1000)
        private Integer maxPlayers = 100;
    }

    @Override
    public void load() {
        super.load();

        // Detect and migrate from v1
        if (configVersion < 2) {
            migrateFromV1();
            configVersion = 2;
            this.save();
        }
    }

    private void migrateFromV1() {
        System.out.println("Migrating config from v1 to v2...");

        // Migrate old values to new structure
        if (oldServerHost != null) {
            server.setHost(oldServerHost);
            oldServerHost = null;
        }

        if (oldServerPort != null) {
            server.setPort(oldServerPort);
            oldServerPort = null;
        }

        if (oldMaxPlayers != null) {
            server.setMaxPlayers(oldMaxPlayers);
            oldMaxPlayers = null;
        }

        System.out.println("Migration complete!");
    }
}
```

**Old config (v1):**
```yaml
server-host: example.com
server-port: 8080
max-players: 50
```

**After migration (v2):**
```yaml
# Config version (DO NOT EDIT)
configVersion: 2

# Server settings
server:
  host: example.com
  port: 8080
  maxPlayers: 50
```

## Best Practices from Examples

### ✅ Configuration Organization

1. **Use subconfigs for logical grouping:**
```java
private ServerConfig server = new ServerConfig();
private DatabaseConfig database = new DatabaseConfig();
```

2. **Provide sensible defaults:**
```java
private Integer port = 8080;  // ✅ Default value
```

3. **Add validation:**
```java
@Min(1) @Max(65535)
private Integer port = 8080;
```

4. **Document with comments:**
```java
@Comment("Server port (1-65535)")
private Integer port = 8080;
```

### ✅ Sensitive Data

Use environment variables for secrets:

```java
@Variable("API_KEY")
@Comment("API key (can be set via API_KEY env var)")
private String apiKey = "changeme";
```

### ✅ Helper Methods

Add convenience methods to your config:

```java
public String buildJdbcUrl() {
    return "jdbc:mysql://" + host + ":" + port + "/" + database;
}

public boolean isFeatureEnabled(String name) {
    return features.getOrDefault(name, false);
}
```

### ✅ Version Tracking

Track config version for migrations:

```java
@Comment("Config version (DO NOT EDIT)")
private Integer configVersion = 1;
```

## Next Steps

- **[Configuration Basics](Configuration-Basics)** - Understanding core concepts
- **[Advanced Topics](Advanced-Topics)** - Custom serializers and transformers
- **[Troubleshooting](Troubleshooting)** - Common issues and solutions

## See Also

- **[Serdes Extensions](Serdes-Extensions)** - Platform-specific type support
- **[Validation](Validation)** - Adding validation to configs
- **[Subconfigs & Serialization](Subconfigs-and-Serialization)** - Organizing complex configs
