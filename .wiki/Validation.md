# Validation

This guide covers adding validation to your configurations using Okaeri Validator and Jakarta EE Validator. Validation ensures config values meet specified constraints, catching configuration errors early.

## Table of Contents

- [Why Validate Configurations?](#why-validate-configurations)
- [Okaeri Validator (Recommended)](#okaeri-validator-recommended)
- [Jakarta EE Validator](#jakarta-ee-validator)
- [Available Annotations](#available-annotations)
- [Validation Examples](#validation-examples)
- [Null Handling](#null-handling)
- [Best Practices](#best-practices)

## Why Validate Configurations?

Validation helps prevent runtime errors by ensuring configuration values are valid **before** they're used in your application:

**Without Validation:**
```yaml
# config.yml
serverPort: -1  # Invalid port!
maxPlayers: 10000  # Way too high!
```

Application crashes or behaves unexpectedly at runtime.

**With Validation:**
```java
@Min(1) @Max(65535)
private Integer serverPort = 25565;

@Min(1) @Max(1000)
private Integer maxPlayers = 100;
```

Validation fails immediately on load with a clear error message.

## Okaeri Validator (Recommended)

Okaeri Validator is a lightweight validation library (~15kB) with Jakarta EE-like annotations. It's the recommended choice for most projects.

### Installation

**Maven:**
```xml
<dependency>
    <groupId>eu.okaeri</groupId>
    <artifactId>okaeri-configs-validator-okaeri</artifactId>
    <version>5.0.13</version>
</dependency>
```

**Gradle (Kotlin DSL):**
```kotlin
implementation("eu.okaeri:okaeri-configs-validator-okaeri:5.0.13")
```

**Gradle (Groovy DSL):**
```groovy
implementation 'eu.okaeri:okaeri-configs-validator-okaeri:5.0.13'
```

### Usage

Wrap your configurer with `OkaeriValidator`:

```java
import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;
import eu.okaeri.configs.validator.okaeri.OkaeriValidator;

MyConfig config = ConfigManager.create(MyConfig.class, (it) -> {
    // Wrap configurer with validator
    it.withConfigurer(new OkaeriValidator(new YamlSnakeYamlConfigurer()));
    it.withBindFile("config.yml");
    it.saveDefaults();
    it.load(true);  // Validation happens here
});
```

### Basic Example

```java
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.validator.annotation.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ServerConfig extends OkaeriConfig {

    @Comment("Server port (1-65535)")
    @Min(1)
    @Max(65535)
    private Integer port = 25565;

    @Comment("Maximum players (1-1000)")
    @Min(1)
    @Max(1000)
    private Integer maxPlayers = 100;

    @Comment("Server name (cannot be blank)")
    @NotBlank
    private String serverName = "My Server";

    @Comment("Server description (3-100 characters)")
    @Size(min = 3, max = 100)
    private String description = "A Minecraft server";
}
```

If the config file has invalid values, loading will throw `ValidationException`:

```yaml
port: -1  # ❌ Fails: must be >= 1
maxPlayers: 5000  # ❌ Fails: must be <= 1000
serverName: ""  # ❌ Fails: cannot be blank
```

## Jakarta EE Validator

For projects that already use Hibernate Validator or need full Jakarta Bean Validation 3.0 support.

### Installation

**Maven:**
```xml
<dependency>
    <groupId>eu.okaeri</groupId>
    <artifactId>okaeri-configs-validator-jakartaee</artifactId>
    <version>5.0.13</version>
</dependency>
```

**Gradle (Kotlin DSL):**
```kotlin
implementation("eu.okaeri:okaeri-configs-validator-jakartaee:5.0.13")
```

### Usage

```java
import eu.okaeri.configs.validator.jakartaee.JakartaValidator;

MyConfig config = ConfigManager.create(MyConfig.class, (it) -> {
    it.withConfigurer(new JakartaValidator(new YamlSnakeYamlConfigurer()));
    it.withBindFile("config.yml");
    it.saveDefaults();
    it.load(true);
});
```

> ⚠️ **Size Warning**: Jakarta EE Validator adds ~2MB to your JAR. Use Okaeri Validator unless you specifically need Jakarta features.

## Available Annotations

### Okaeri Validator Annotations

#### Numeric Constraints

| Annotation | Description | Example |
|------------|-------------|---------|
| `@Min(value)` | Minimum numeric value | `@Min(0)` |
| `@Max(value)` | Maximum numeric value | `@Max(100)` |
| `@Positive` | Must be positive (> 0) | `@Positive` |
| `@PositiveOrZero` | Must be >= 0 | `@PositiveOrZero` |
| `@Negative` | Must be negative (< 0) | `@Negative` |
| `@NegativeOrZero` | Must be <= 0 | `@NegativeOrZero` |
| `@DecimalMin(value)` | Minimum decimal value | `@DecimalMin("0.0")` |
| `@DecimalMax(value)` | Maximum decimal value | `@DecimalMax("1.0")` |

#### String & Collection Constraints

| Annotation | Description | Example |
|------------|-------------|---------|
| `@NotNull` | Value cannot be null | `@NotNull` |
| `@NotBlank` | String not empty/whitespace | `@NotBlank` |
| `@Size(min, max)` | Length/size constraints | `@Size(min=3, max=10)` |
| `@Pattern(regex)` | Matches regex pattern | `@Pattern("[a-z]+")` |

#### Null Handling

| Annotation | Description | Example |
|------------|-------------|---------|
| `@Nullable` | Allows null (when using NOT_NULL policy) | `@Nullable` |

### Jakarta EE Annotations

Jakarta EE Validator supports all standard Jakarta Bean Validation 3.0 annotations:

- `@NotNull`, `@NotBlank`, `@NotEmpty`
- `@Min`, `@Max`, `@DecimalMin`, `@DecimalMax`
- `@Size`, `@Pattern`, `@Email`
- `@Positive`, `@PositiveOrZero`, `@Negative`, `@NegativeOrZero`
- `@Past`, `@PastOrPresent`, `@Future`, `@FutureOrPresent`
- `@AssertTrue`, `@AssertFalse`
- And more...

See [Jakarta Bean Validation Spec](https://jakarta.ee/specifications/bean-validation/3.0/jakarta-bean-validation-spec-3.0.html) for complete reference.

## Validation Examples

### Numeric Validation

```java
@Getter
@Setter
public class GameConfig extends OkaeriConfig {

    @Comment("Player health (1-100)")
    @Min(1)
    @Max(100)
    private Integer health = 100;

    @Comment("Damage multiplier (positive only)")
    @Positive
    private Double damageMultiplier = 1.0;

    @Comment("Experience points (cannot be negative)")
    @PositiveOrZero
    private Integer experience = 0;

    @Comment("Temperature (-100 to 100)")
    @Min(-100)
    @Max(100)
    private Integer temperature = 20;
}
```

### String Validation

```java
@Getter
@Setter
public class UserConfig extends OkaeriConfig {

    @Comment("Username (3-16 characters)")
    @Size(min = 3, max = 16)
    @NotBlank
    private String username = "player";

    @Comment("Email address")
    @Pattern(".+@.+\\..+")  // Simple email regex
    private String email = "player@example.com";

    @Comment("Server MOTD (max 100 chars)")
    @Size(max = 100)
    private String motd = "Welcome to the server!";

    @Comment("Alphanumeric code only")
    @Pattern("[a-zA-Z0-9]+")
    private String code = "ABC123";
}
```

### Collection Validation

```java
@Getter
@Setter
public class PermissionsConfig extends OkaeriConfig {

    @Comment("Admin users (1-10 admins)")
    @Size(min = 1, max = 10)
    private List<String> admins = List.of("admin");

    @Comment("Allowed commands (at least one)")
    @Size(min = 1)
    private Set<String> allowedCommands = Set.of("help", "info");

    @Comment("Role permissions (cannot be empty)")
    @Size(min = 1)
    private Map<String, List<String>> rolePermissions = Map.of(
        "admin", List.of("*")
    );
}
```

### Combined Constraints

```java
@Getter
@Setter
public class DatabaseConfig extends OkaeriConfig {

    @Comment("Database host (not blank)")
    @NotBlank
    private String host = "localhost";

    @Comment("Database port (1-65535)")
    @Min(1)
    @Max(65535)
    private Integer port = 5432;

    @Comment("Database name (3-63 chars, alphanumeric + underscore)")
    @Size(min = 3, max = 63)
    @Pattern("[a-zA-Z0-9_]+")
    private String database = "mydb";

    @Comment("Connection timeout seconds (1-300)")
    @Min(1)
    @Max(300)
    private Integer timeout = 30;

    @Comment("Max connections (1-100)")
    @Min(1)
    @Max(100)
    private Integer maxConnections = 10;
}
```

### Subconfig Validation

Validation works in subconfigs too:

```java
@Getter
@Setter
public class AppConfig extends OkaeriConfig {

    @Comment("Server settings")
    private ServerSettings server = new ServerSettings();

    @Comment("Database settings")
    private DatabaseSettings database = new DatabaseSettings();

    @Getter
    @Setter
    public static class ServerSettings extends OkaeriConfig {
        @Min(1) @Max(65535)
        private Integer port = 8080;

        @NotBlank
        private String host = "0.0.0.0";
    }

    @Getter
    @Setter
    public static class DatabaseSettings extends OkaeriConfig {
        @NotBlank
        private String url = "jdbc:postgresql://localhost/db";

        @Min(1) @Max(1000)
        private Integer poolSize = 10;
    }
}
```

### Serializable Object Validation

Validation also works on Serializable objects:

```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Player implements Serializable {
    @Size(min = 3, max = 16)
    private String username;

    @Min(0) @Max(100)
    private Integer level;

    @PositiveOrZero
    private Double experience;
}

@Getter
@Setter
public class PlayerConfig extends OkaeriConfig {

    @Comment("Player data")
    private Player player = new Player();
}
```

## Null Handling

### Default Behavior (Nullable)

By default, null values are allowed:

```java
// Default: nulls are allowed
MyConfig config = ConfigManager.create(MyConfig.class, (it) -> {
    it.withConfigurer(new OkaeriValidator(new YamlSnakeYamlConfigurer()));
    it.withBindFile("config.yml");
    it.load();
});

// These fields can be null without error
private String optionalField;  // ✅ Can be null
private Integer optionalPort;  // ✅ Can be null
```

### Strict Mode (@NotNull by Default)

Enable strict validation to require all fields be non-null by default:

```java
// Strict mode: all fields must be non-null by default
MyConfig config = ConfigManager.create(MyConfig.class, (it) -> {
    it.withConfigurer(new OkaeriValidator(new YamlSnakeYamlConfigurer(), true));  // true = strict
    it.withBindFile("config.yml");
    it.load();
});
```

In strict mode, use `@Nullable` to allow specific fields to be null:

```java
@Getter
@Setter
public class StrictConfig extends OkaeriConfig {

    // Strict mode enabled - must be non-null
    private String requiredField = "value";

    // Explicitly allow null
    @Nullable
    private String optionalField;

    @Nullable
    private Integer optionalPort;
}
```

### Explicit @NotNull

Use `@NotNull` to require non-null even in default mode:

```java
@Getter
@Setter
public class Config extends OkaeriConfig {

    @NotNull  // Explicit - must not be null
    private String requiredField = "value";

    private String optionalField;  // Can be null
}
```

## Best Practices

### ✅ Do's

1. **Validate critical configuration values:**
   ```java
   @Min(1) @Max(65535)
   private Integer port = 8080;  // ✅ Prevent invalid ports
   ```

2. **Use appropriate constraints for the data type:**
   ```java
   @NotBlank  // ✅ For strings
   private String name;

   @PositiveOrZero  // ✅ For counts/amounts
   private Integer count;

   @Size(min = 1)  // ✅ For collections
   private List<String> items;
   ```

3. **Combine constraints when needed:**
   ```java
   @NotBlank
   @Size(min = 3, max = 50)
   @Pattern("[a-zA-Z0-9_]+")
   private String username;  // ✅ Multiple constraints
   ```

4. **Add helpful comments explaining constraints:**
   ```java
   @Comment("Server port (1-65535)")
   @Min(1) @Max(65535)
   private Integer port = 25565;
   ```

5. **Use strict mode for critical configs:**
   ```java
   // ✅ Good for production configs
   new OkaeriValidator(configurer, true)  // Strict mode
   ```

6. **Validate subconfigs and collections:**
   ```java
   @Size(min = 1, max = 10)  // ✅ Validate collection size
   private List<ServerDef> servers;
   ```

### ❌ Don'ts

1. **Don't skip validation on important fields:**
   ```java
   // ❌ No validation - could be negative!
   private Integer port = 8080;

   // ✅ With validation
   @Min(1) @Max(65535)
   private Integer port = 8080;
   ```

2. **Don't use overly permissive constraints:**
   ```java
   @Min(0)  // ❌ Too permissive for a port
   private Integer port = 8080;

   @Min(1) @Max(65535)  // ✅ Realistic range
   private Integer port = 8080;
   ```

3. **Don't forget to wrap the configurer:**
   ```java
   // ❌ Forgot to wrap with validator!
   it.withConfigurer(new YamlSnakeYamlConfigurer());

   // ✅ Wrapped with validator
   it.withConfigurer(new OkaeriValidator(new YamlSnakeYamlConfigurer()));
   ```

4. **Don't use @NotNull when @NotBlank is needed:**
   ```java
   @NotNull  // ❌ Allows empty string ""
   private String name;

   @NotBlank  // ✅ Requires non-empty string
   private String name;
   ```

5. **Don't validate unchangeable defaults:**
   ```java
   @Min(1) @Max(1)  // ❌ Why validate if it's always 1?
   private final Integer version = 1;

   private final Integer version = 1;  // ✅ No validation needed
   ```

### Validation Error Messages

When validation fails, you'll get a clear error message:

```
ValidationException: port (-1) is invalid: must be greater than or equal to 1
ValidationException: maxPlayers (5000) is invalid: must be less than or equal to 1000
ValidationException: serverName () is invalid: must not be blank
```

## Next Steps

- **[Subconfigs & Serialization](Subconfigs-and-Serialization)** - Validate nested configurations
- **[Examples & Recipes](Examples-and-Recipes)** - Real-world validation examples
- **[Advanced Topics](Advanced-Topics)** - Custom validation logic

## See Also

- **[Configuration Basics](Configuration-Basics)** - Understanding OkaeriConfig
- **[Annotations Guide](Annotations-Guide)** - All available annotations
- [Okaeri Validator Repository](https://github.com/OkaeriPoland/okaeri-validator) - Full validator documentation
