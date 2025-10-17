# Annotations Guide

Okaeri Configs provides several annotations to customize and document your configuration classes. This guide covers all available annotations with practical examples.

## Overview

| Annotation | Target | Purpose |
|------------|--------|---------|
| [@Header](#header) | Class | Add header comments to the config file |
| [@Comment](#comment) | Field | Document individual fields |
| [@Variable](#variable) | Field | Use environment variables or JVM properties |
| [@Exclude](#exclude) | Field | Exclude field from serialization |
| [@ReadOnly](#readonly) | Field | Preserve original loaded values, ignore runtime modifications |
| [@Include](#include) | Class | Include fields from other config classes |
| [@TargetType](#targettype) | Field | Provide type hints for collections |
| [@Names](#names-deprecated) | Class | ⚠️ **Deprecated** - Global naming strategy |
| [@CustomKey](#customkey) | Field | Override field name in config file |

## @Header

Adds header comments to the top of your config file. Repeatable.

```java
import eu.okaeri.configs.annotation.Header;

@Header("################################")
@Header("#   My Application Config      #")
@Header("################################")
public class MyConfig extends OkaeriConfig {
    private String appName = "MyApp";
}
```

**Output (YAML):**
```yaml
################################
#   My Application Config      #
################################
appName: MyApp
```

## @Comment

Adds comments to fields. Repeatable.

```java
import eu.okaeri.configs.annotation.Comment;

public class ServerConfig extends OkaeriConfig {

    @Comment("Server hostname or IP")
    private String host = "localhost";

    @Comment("Server port (1-65535)")
    private Integer port = 8080;
}
```

**Output (YAML):**
```yaml
# Server hostname or IP
host: localhost

# Server port (1-65535)
port: 8080
```

> 💡 **Tip**: Use arrays for multi-line: `@Comment({"Line 1", "Line 2"})`

> ⚠️ **Note**: JSON format doesn't support comments in output

## @Variable

Allows field values to be overridden by environment variables or JVM system properties.

### Basic Usage

```java
import eu.okaeri.configs.annotation.Variable;

public class AppConfig extends OkaeriConfig {

    @Variable("API_KEY")
    private String apiKey = "default-key";

    @Variable("DATABASE_URL")
    private String dbUrl = "jdbc:mysql://localhost/db";
}
```

**Runtime behavior:**
```bash
# Environment variable takes precedence
export API_KEY="production-key-123"
java -jar myapp.jar
# apiKey will be "production-key-123"

# JVM property also works
java -DDATABASE_URL="jdbc:postgresql://prod-db/mydb" -jar myapp.jar
# dbUrl will be "jdbc:postgresql://prod-db/mydb"
```

### Variable Mode

The `mode` parameter controls when the variable is resolved:

#### RUNTIME Mode (Default)

Variable is resolved when the config is loaded, but the resolved value is **not** written to the config file:

```java
@Variable(value = "API_KEY", mode = VariableMode.RUNTIME)
private String apiKey = "default-key";
```

**Behavior:**
- Reads from environment/system property on load
- Uses default if variable not found
- **Does not** write the resolved value to file
- File always shows the default value

**Use case:** Sensitive data (API keys, passwords) that shouldn't be saved to disk.

#### WRITE Mode

Variable is resolved and the resolved value **is** written to the config file:

```java
@Variable(value = "INITIAL_ADMIN", mode = VariableMode.WRITE)
private String adminUser = "admin";
```

**Behavior:**
- Reads from environment/system property on first run
- Writes the resolved value to the config file
- Subsequent loads use the value from the file, not the variable

**Use case:** Initial setup values that should persist after first run.

### Complete Example

```java
import eu.okaeri.configs.annotation.Variable;
import eu.okaeri.configs.annotation.VariableMode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductionConfig extends OkaeriConfig {

    @Variable("APP_NAME")
    @Comment("Application name (from APP_NAME env var)")
    private String appName = "MyApp";

    @Variable(value = "SECRET_KEY", mode = VariableMode.RUNTIME)
    @Comment("Secret key (never written to file)")
    private String secretKey = "change-me";

    @Variable(value = "FIRST_RUN_ADMIN", mode = VariableMode.WRITE)
    @Comment("Initial admin user (written on first run)")
    private String adminUser = "admin";
}
```

### Priority Order

When resolving variables:

1. **JVM system property** (e.g., `-DAPI_KEY=value`)
2. **Environment variable** (e.g., `API_KEY`)
3. **Default value** from field initializer

## @Exclude

Excludes a field from being saved or loaded. Alternative to `transient` keyword.

### Basic Usage

```java
import eu.okaeri.configs.annotation.Exclude;

public class Config extends OkaeriConfig {

    private String savedField = "saved";

    @Exclude
    private String excludedField = "not saved";

    // Alternative: transient keyword
    private transient String alsoExcluded = "also not saved";
}
```

**Output (YAML):**
```yaml
savedField: saved
# excludedField and alsoExcluded are not in the file
```

### When to Use

Use `@Exclude` when:
- Field should exist in the class but not in the config file
- Runtime-only data (caches, connections, state)
- Computed values derived from other fields

### @Exclude vs transient

Both work identically for okaeri-configs:

```java
// These are equivalent:
@Exclude
private String field1;

private transient String field2;
```

**Difference:**
- `transient` also affects Java serialization
- `@Exclude` only affects okaeri-configs

> 💡 **Recommendation**: Use `transient` unless you specifically need Java serialization to work differently.

## @ReadOnly

Preserves the original loaded value when saving. The field can be modified at runtime, but saves will use the original value.

```java
import eu.okaeri.configs.annotation.ReadOnly;

public class AppConfig extends OkaeriConfig {

    @ReadOnly
    private String buildNumber = "1234";

    private String appVersion = "2.5.0";
}
```

**Example:**
```java
config.setBuildNumber("5678");   // Modify in code
config.setAppVersion("3.0.0");
config.save();
```

**Result in file:**
```yaml
buildNumber: 1234     # Original value preserved
appVersion: 3.0.0     # Modified value saved
```

**Use cases:** Build metadata, test environment markers, deployment timestamps - values that should only change through external processes (CI/CD, build tools), not runtime modifications.

## @Include

Makes parent class fields visible in the config. **Requires extending the included class.**

```java
import eu.okaeri.configs.annotation.Include;

// Parent
public class BaseConfig extends OkaeriConfig {
    private String appName = "MyApp";
    private String version = "1.0.0";
}

// Child extends parent and uses @Include
@Include(BaseConfig.class)
public class ServerConfig extends BaseConfig {
    private String host = "localhost";
    private Integer port = 8080;
}
```

**Output (YAML):**
```yaml
appName: MyApp
version: 1.0.0
host: localhost
port: 8080
```

**Without `@Include`, only `host` and `port` would appear** - parent fields aren't automatically scanned.

### Notes

- Must extend the included class (throws `IllegalArgumentException` if not)
- Child fields override parent fields with the same name
- For composition (not inheritance), use [subconfigs](Subconfigs-and-Serialization) instead

## @TargetType

Specifies the concrete implementation type for collection fields. Used to override default collection implementations when you can't change the field type.

> 📖 **See Also**: For complete details on collection types and defaults, see **[Supported Types - Collection Types](Supported-Types#collection-types)**.

### Quick Overview

When you can't change a field from an interface type (e.g., `Set<String>`) to a concrete type (e.g., `HashSet<String>`), use `@TargetType` to specify the implementation:

```java
import eu.okaeri.configs.annotation.TargetType;

public class TargetTypeExample extends OkaeriConfig {

    // Default: LinkedHashSet (ordered)
    private Set<String> defaultSet = new LinkedHashSet<>();

    // Force: HashSet (unordered, faster)
    @TargetType(HashSet.class)
    private Set<String> unorderedSet = new HashSet<>();

    // Force: TreeSet (sorted)
    @TargetType(TreeSet.class)
    private Set<Integer> sortedSet = new TreeSet<>();
}
```

### When to Use @TargetType

Use `@TargetType` **only when** you cannot change the field type:

```java
// ✅ PREFERRED: Use concrete type directly
private HashSet<String> names = new HashSet<>();

// ✅ Use @TargetType when field type must be interface (API compatibility)
@TargetType(HashSet.class)
private Set<String> names = new HashSet<>();  // API contract requires Set interface
```

### Use Case

When you must maintain an interface type (e.g., for API compatibility):

```java
import lombok.Getter;

@Getter
public class ApiConfig extends OkaeriConfig {

    // Public API requires Set interface, but we want TreeSet implementation
    @TargetType(TreeSet.class)
    private Set<String> allowedValues = new TreeSet<>();
}
```

> 💡 **Note**: If you can change the field type, just use the concrete type directly: `private TreeSet<String> allowedValues = new TreeSet<>();`

### What NOT to Use @TargetType For

#### ❌ Don't Use with Raw Types

Raw types (without generics) **will not work** with okaeri-configs:

```java
// ❌ WRONG - Raw types don't work!
@TargetType(ArrayList.class)
private List rawList = new ArrayList();  // No generics = error

// ✅ CORRECT - Always use generics
@TargetType(ArrayList.class)
private List<String> properList = new ArrayList<>();
```

> ⚠️ **Important**: Raw types are not supported. This annotation cannot fix raw type issues. See **[Supported Types - Unsupported Types](Supported-Types#unsupported-types)** for more information.

### Best Practices

```java
// ❌ Unnecessary - concrete type already specified
@TargetType(ArrayList.class)
private ArrayList<String> list = new ArrayList<>();

// ❌ Unnecessary - default is fine
@TargetType(ArrayList.class)
private List<String> list = new ArrayList<>();

// ❌ Won't work - raw types not supported
@TargetType(ArrayList.class)
private List rawList = new ArrayList();

// ✅ Good - concrete type is explicit and self-documenting
private HashSet<String> uniqueNames = new HashSet<>();

// ✅ Good - using @TargetType when you can't change field type
@TargetType(HashSet.class)
private Set<String> names = new HashSet<>();  // Must be Set for API
```

### See Also

For detailed information about collection types:
- **[Supported Types - Collection Types](Supported-Types#collection-types)** - Default implementations and how collections work
- **[Supported Types - Custom Collection Implementations](Supported-Types#custom-collection-implementations)** - Using TreeSet, ConcurrentHashMap, etc.
- **[Collections & Maps](Collections-and-Maps)** - Advanced collection usage patterns

## @Names (Deprecated)

⚠️ **This annotation is deprecated and should not be used in new code.**

### Why It's Deprecated

From the source code documentation:

> "This annotation was intended mainly for legacy compatibility use; however, it never worked 100% as one might expect... I, the author, believe the keys in the config files should match the field names whenever possible."

### Known Issues

The naming strategies have bugs:

```java
// Field: myVectorY
// Expected: my-vector-y
// Actual: my-vectory  ❌

// Field: myServiceAPI
// Expected: my-service-api
// Actual: my-service-a-pi  ❌
```

### Alternative

Just use matching field names:

```java
// ❌ DON'T USE @Names
@Names(strategy = NameStrategy.HYPHEN_CASE, modifier = NameModifier.TO_LOWER_CASE)
public class MyConfig extends OkaeriConfig {
    private String myField;
}

// ✅ MATCH FIELD NAMES TO CONFIG KEYS
public class MyConfig extends OkaeriConfig {
    private String myField;  // Config key: myField (camelCase)
}
```

## @CustomKey

Overrides the field name used in the configuration file. **Use sparingly - prefer matching field names to config keys.**

### When to Use

The **main valid use case** for @CustomKey is when you want a different **Java getter name** than the config file key:

```java
import eu.okaeri.configs.annotation.CustomKey;
import lombok.Getter;

@Getter
public class Config extends OkaeriConfig {

    @CustomKey("apiKey")
    private String apiSecretKey = "secret";
}
```

**Config file:**
```yaml
apiKey: secret
```

**Java code:**
```java
String key = config.getApiSecretKey();  // Uses field name for getter
```

### Legacy Migration

When loading old config files that used non-standard naming:

```java
// Old config has "old_field_name" (snake_case from v1)
@CustomKey("old_field_name")
private String properFieldName = "value";

// Old config has "max-players" (hyphen-case from old system)
@CustomKey("max-players")
private Integer maxPlayers = 100;
```

### Restrictions

**What doesn't work:**

- ❌ **Dots (`.`)** - Not supported, cannot move fields to different nesting levels
- ❌ **Empty strings** - `@CustomKey("")` is invalid

For nested structure, use [subconfigs](Subconfigs-and-Serialization) instead.

### What NOT to Do

```java
// 💭 RECONSIDER - Prefer matching field names
@CustomKey("server-host")
private String serverHost;

// ❌ POINTLESS - Same name, no effect
@CustomKey("serverHost")
private String serverHost;

// ❌ WON'T WORK - Dots not supported
@CustomKey("server.host")
private String serverHost;

// ✅ PREFERRED - No annotation needed
private String serverHost;
```

**Why avoid this?** It makes code harder to maintain with no benefit. Field names should match config keys.

### Complete Example

```java
@Getter
public class AppConfig extends OkaeriConfig {

    // ✅ Normal field - no annotation needed
    private String appName = "MyApp";

    // ✅ Different getter name
    @CustomKey("apiKey")
    private String apiSecretKey = "secret";

    // ✅ Loading legacy field
    @CustomKey("old_server_ip")
    private String serverIp = "localhost";

    // ❌ Don't do this
    // @CustomKey("max-players")
    // private Integer maxPlayers;

    // ✅ Do this instead
    private Integer maxPlayers = 100;
}
```

> 💡 **Best Practice**: Only use @CustomKey when the Java field name **must** differ from the config key. In 99% of cases, you don't need it.

## Combining Annotations

You can use multiple annotations on the same field or class:

### Field Annotations

```java
@Comment("Database connection URL")
@Variable("DATABASE_URL")
@CustomKey("db-url")
private String databaseUrl = "jdbc:mysql://localhost/mydb";
```

**Output (YAML):**
```yaml
# Database connection URL
db-url: jdbc:mysql://localhost/mydb
```

**Behavior:**
- Uses `DATABASE_URL` environment variable if available
- Saved in config file as `db-url`
- Has helpful comment

### Class Annotations

```java
@Header("################################")
@Header("#   Production Configuration   #")
@Header("################################")
@Header("")
@Header("DO NOT COMMIT WITH REAL VALUES")
public class ProductionConfig extends OkaeriConfig {

    @Comment("API endpoint")
    @Variable("API_ENDPOINT")
    private String apiEndpoint = "https://api.example.com";
}
```

## Best Practices

### ✅ DO

- **Use @Comment liberally** - Document what each field does
- **Use @Header for important info** - Version, warnings, instructions
- **Use @Variable for secrets** - With `VariableMode.RUNTIME`
- **Use @CustomKey for legacy configs** - When migrating from other systems
- **Keep comments concise** - Short, clear descriptions

### ❌ DON'T

- **Don't use @Names** - It's deprecated and buggy
- **Don't over-comment** - Obvious fields don't need comments
- **Don't put secrets in headers** - Use @Variable with RUNTIME mode
- **Don't use @CustomKey everywhere** - Match field names when possible

## Examples

### Production Config

```java
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Header("################################")
@Header("#   Production Configuration   #")
@Header("################################")
@Header("")
@Header("Environment variables:")
@Header("  - DATABASE_URL: Database connection string")
@Header("  - API_SECRET: API secret key")
public class ProductionConfig extends OkaeriConfig {

    @Comment("Application name")
    private String appName = "MyApp";

    @Comment("Database connection (from env var)")
    @Variable("DATABASE_URL")
    private String databaseUrl = "jdbc:mysql://localhost/mydb";

    @Comment("API secret key (never saved to file)")
    @Variable(value = "API_SECRET", mode = VariableMode.RUNTIME)
    private String apiSecret = "change-me-in-production";

    @Comment("Enable debug logging")
    private Boolean debug = false;

    @Exclude
    private transient long startupTime = System.currentTimeMillis();
}
```

### Legacy Migration Config

```java
@Header("Migrated from old config format")
public class LegacyConfig extends OkaeriConfig {

    @Comment("Server settings")
    @CustomKey("server_address")
    private String serverAddress = "localhost";

    @CustomKey("server_port")
    private Integer serverPort = 25565;

    @Comment("Player settings")
    @CustomKey("max-players")
    private Integer maxPlayers = 20;

    @CustomKey("pvp_enabled")
    private Boolean pvpEnabled = true;
}
```

## Format-Specific Behavior

### YAML (SnakeYAML, Bukkit, Bungee)

- ✅ @Header: Full support
- ✅ @Comment: Full support for all fields

### HJSON

- ✅ @Header: Full support
- ✅ @Comment: Full support for all fields

### JSON (GSON, json-simple)

- ❌ @Header: Ignored (JSON has no comment syntax)
- ❌ @Comment: Ignored (JSON has no comment syntax)

### HOCON (Lightbend)

- ⚠️ @Header: Works
- ⚠️ @Comment: **Only top-level fields** - nested subconfig comments don't work

## Next Steps

- **[Configuration Basics](Configuration-Basics)** - Understanding field declarations
- **[Supported Types](Supported-Types)** - What types can be used
- **[Subconfigs & Serialization](Subconfigs-and-Serialization)** - Nested configurations
- **[Validation](Validation)** - Add validation annotations

## See Also

- **[Getting Started](Getting-Started)** - First config setup
- **[Format Guides](Format-Guides)** - Format-specific features and limitations
