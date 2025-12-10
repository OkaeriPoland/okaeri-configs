# Migrations

This guide covers configuration migrations - automatically updating config files when structure or keys change between versions.

## Table of Contents

- [Why Use Migrations?](#why-use-migrations)
- [Migration Basics](#migration-basics)
- [Migration File Organization](#migration-file-organization)
- [ConfigMigrationDsl](#configmigrationdsl)
- [RawConfigView](#rawconfigview)
- [Named Migrations](#named-migrations)
- [Applying Migrations](#applying-migrations)
- [Best Practices](#best-practices)
- [Complex Migration Scenarios](#complex-migration-scenarios)

## Why Use Migrations?

Migrations solve the problem of **breaking config changes** when you update your application:

**Without migrations:**
```yaml
# v1.0 config
server-host: localhost
server-port: 8080
```

```java
// v2.0 - restructured to nested config
@Getter
@Setter
public class AppConfig extends OkaeriConfig {
    private ServerConfig server = new ServerConfig();  // ❌ Old keys lost!
}
```

**Result:** Users lose their settings, must manually reconfigure.

**With migrations:**
```java
// Automatically migrate old structure to new
C0001_Migrate_server_to_nested_structure migration = ...;
config.migrate(migration);
```

**Result:** Config automatically updates, user settings preserved.

## Migration Basics

### ConfigMigration Interface

All migrations implement the `ConfigMigration` functional interface:

```java
@FunctionalInterface
public interface ConfigMigration {
    boolean migrate(OkaeriConfig config, RawConfigView view);
}
```

**Return value:**
- `true` - Migration was performed
- `false` - Migration was skipped (conditions not met)

### Simple Migration Example

```java
import eu.okaeri.configs.migrate.ConfigMigration;
import eu.okaeri.configs.migrate.view.RawConfigView;

ConfigMigration migration = (config, view) -> {
    // Check if old key exists
    if (view.exists("old-key")) {
        // Get old value
        Object value = view.get("old-key");

        // Set new key
        view.set("new-key", value);

        // Remove old key
        view.remove("old-key");

        return true;  // Migration performed
    }
    return false;  // Nothing to migrate
};

// Apply migration
config.migrate(migration);
```

## Migration File Organization

### Recommended Pattern

Organize migrations in a dedicated package using numbered files with descriptive names:

```
src/main/java/
  └── your/package/
      └── config/
          ├── AppConfig.java
          └── migration/
              ├── C0001_Fix_server_host_key_case.java
              ├── C0002_Migrate_server_to_nested_structure.java
              ├── C0003_Add_default_timeout_value.java
              └── C0004_Rename_maxConnections_to_max_connections.java
```

### Naming Convention

**Pattern:** `{Prefix}{number}_{description}.java`

- `{Prefix}` - Letter identifying the config (C for general config, or custom per config)
- `{number}` - Sequential number with leading zeros (0001, 0002, etc.)
- `{description}` - Descriptive snake_case or PascalCase description

**Common prefixes:**
- `C` - Config (default for single config or main config)
- `A` - AppConfig
- `D` - DatabaseConfig
- `M` - MessagesConfig
- `S` - ServerConfig

**Examples:**
- `C0001_Fix_server_host_key_case.java`
- `C0002_Migrate_database_credentials.java`
- `C0003_Add_missing_server_fields.java`

### Migration File Template

```java
package your.package.config.migration;

import eu.okaeri.configs.migrate.builtin.NamedMigration;
import static eu.okaeri.configs.migrate.ConfigMigrationDsl.*;

/**
 * Migration C0001: Fixes the inconsistency of the server-host key
 *
 * Changes:
 * - Renames "server-Host" to "server-host"
 */
public class C0001_Fix_server_host_key_case extends NamedMigration {

    public C0001_Fix_server_host_key_case() {
        super("fixes the inconsistency of the `server-Host` key by renaming it",
            move("server-Host", "server-host")
        );
    }
}
```

### Multiple Config Files

When working with multiple config files, use different prefixes for each:

```
src/main/java/
  └── your/package/
      └── config/
          ├── AppConfig.java
          ├── DatabaseConfig.java
          ├── MessagesConfig.java
          └── migration/
              ├── A0001_Migrate_app_settings.java        # AppConfig
              ├── A0002_Add_feature_flags.java           # AppConfig
              ├── D0001_Restructure_connection_pool.java # DatabaseConfig
              ├── D0002_Add_timeout_settings.java        # DatabaseConfig
              ├── M0001_Add_missing_translations.java    # MessagesConfig
              └── M0002_Rename_error_keys.java           # MessagesConfig
```

**Example with multiple configs:**

```java
// AppConfig migrations
public class A0001_Migrate_app_settings extends NamedMigration {
    public A0001_Migrate_app_settings() {
        super("migrates app settings to new structure",
            move("app-name", "application.name")
        );
    }
}

// DatabaseConfig migrations
public class D0001_Restructure_connection_pool extends NamedMigration {
    public D0001_Restructure_connection_pool() {
        super("restructures connection pool settings",
            multi(
                move("pool-min", "pool.min-size"),
                move("pool-max", "pool.max-size")
            )
        );
    }
}

// MessagesConfig migrations
public class M0001_Add_missing_translations extends NamedMigration {
    public M0001_Add_missing_translations() {
        super("adds missing translation keys",
            multi(
                supply("messages.welcome", () -> "Welcome!"),
                supply("messages.goodbye", () -> "Goodbye!")
            )
        );
    }
}
```

**Applying config-specific migrations:**

```java
// Apply only AppConfig migrations
appConfig.migrate(
    new A0001_Migrate_app_settings(),
    new A0002_Add_feature_flags()
);

// Apply only DatabaseConfig migrations
dbConfig.migrate(
    new D0001_Restructure_connection_pool(),
    new D0002_Add_timeout_settings()
);

// Apply only MessagesConfig migrations
messagesConfig.migrate(
    new M0001_Add_missing_translations(),
    new M0002_Rename_error_keys()
);
```

## ConfigMigrationDsl

The DSL provides convenient migration helpers via static imports:

```java
import static eu.okaeri.configs.migrate.ConfigMigrationDsl.*;
```

### Action Migrations

#### move()

Moves a key to a new location (copies then deletes):

```java
// Simple move
move("old-key", "new-key")

// Move with transformation
move("old-key", "new-key", value -> ((String) value).toUpperCase())
```

**Example:**
```java
// Rename key
move("maxPlayers", "max-players")

// Move to nested structure
move("serverHost", "server.host")

// Move and transform
move("timeout", "timeout-seconds", value -> ((Integer) value) * 1000)
```

#### copy()

Copies a key's value (keeps original):

```java
copy("source-key", "dest-key")
```

**Example:**
```java
// Create backup before migration
copy("database", "database-backup")
```

#### delete()

Removes a key:

```java
delete("obsolete-key")
```

**Example:**
```java
// Remove deprecated field
delete("legacy-feature-enabled")
```

#### update()

Updates a key's value in-place:

```java
update("key", value -> transform(value))
```

**Example:**
```java
// Convert string to lowercase
update("username", value -> ((String) value).toLowerCase())

// Increment version
update("config-version", value -> ((Integer) value) + 1)

// Update nested value
update("server.max-players", value -> Math.min((Integer) value, 1000))
```

#### supply()

Creates a key if it doesn't exist:

```java
supply("key", () -> defaultValue)
```

**Example:**
```java
// Add missing default value
supply("timeout", () -> 30)

// Add new field with computed value
supply("created-at", () -> System.currentTimeMillis())
```

### Conditional Migrations

#### exists()

Checks if a key exists:

```java
exists("key-name")
```

#### match()

Tests a value against a predicate:

```java
match("key", (Type value) -> predicate)
```

**Example:**
```java
// Check if version is 1
match("config-version", (Integer v) -> v == 1)

// Check if string matches pattern
match("mode", (String m) -> m.equals("legacy"))
```

#### when()

Conditional execution:

```java
// With else branch
when(condition, trueBranch, falseBranch)

// Without else branch
when(condition, trueBranch)
```

**Example:**
```java
// Migrate only if old key exists
when(
    exists("old-key"),
    move("old-key", "new-key")
)

// Version-specific migration
when(
    match("version", (Integer v) -> v == 1),
    multi(
        update("version", v -> 2),
        move("old-field", "new-field")
    ),
    noop(false)
)
```

### Combinators

#### multi()

Executes multiple migrations (continues even if some fail):

```java
multi(
    migration1,
    migration2,
    migration3
)
```

**Example:**
```java
multi(
    move("serverHost", "server.host"),
    move("serverPort", "server.port"),
    delete("server-legacy")
)
```

#### any()

Returns `true` if any migration succeeds:

```java
any(
    exists("key1"),
    exists("key2"),
    exists("key3")
)
```

#### all()

Returns `true` only if all migrations succeed:

```java
all(
    exists("required-key-1"),
    exists("required-key-2"),
    exists("required-key-3")
)
```

#### not()

Inverts a migration's result:

```java
not(exists("key"))  // True if key doesn't exist
```

#### noop()

Does nothing, returns specified result:

```java
noop(true)   // Always returns true
noop(false)  // Always returns false
```

## RawConfigView

`RawConfigView` provides low-level access to config data for migrations. It implements `TypedKeyReader` and `TypedKeyWriter` interfaces, giving you a rich set of methods for reading and writing values with automatic type resolution and simplification.

All key access uses dot notation for nested keys (e.g., `"server.database.host"`).

### Basic Methods

| Method | Description | Example |
|--------|-------------|---------|
| `exists(key)` | Check if key exists | `view.exists("server.host")` |
| `get(key)` | Get raw value (untyped) | `Object port = view.get("server.port")` |
| `set(key, value)` | Set value with auto-simplification | `view.set("timeout", 30)` |
| `remove(key)` | Remove key and return old value | `Object old = view.remove("obsolete")` |

### Typed Read Methods (TypedKeyReader)

All read methods support automatic type resolution and conversion:

| Method | Return | Description |
|--------|--------|-------------|
| `get(key, Class<T>)` | `T` | Get value resolved to type |
| `get(key, GenericsDeclaration)` | `T` | Get value with full generic type |
| `getOr(key, Class<T>, defaultValue)` | `T` | Get typed value with fallback |
| `getAsList(key, elementType)` | `List<T>` | Get list with element type |
| `getAsSet(key, elementType)` | `Set<T>` | Get set with element type |
| `getAsCollection(key, GenericsDeclaration)` | `Collection<T>` | Get complex collection type |
| `getAsMap(key, keyType, valueType)` | `Map<K,V>` | Get map with key/value types |
| `getAsMap(key, GenericsDeclaration)` | `Map<K,V>` | Get complex map type |

**Read examples:**
```java
// Simple typed reads
Integer port = view.get("server.port", Integer.class);
String host = view.get("server.host", String.class);
Boolean enabled = view.get("feature.enabled", Boolean.class);

// With defaults
Integer timeout = view.getOr("timeout", Integer.class, 30);
String mode = view.getOr("mode", String.class, "production");

// Lists and sets
List<String> tags = view.getAsList("tags", String.class);
Set<Integer> ports = view.getAsSet("allowed-ports", Integer.class);

// Maps
Map<String, Integer> limits = view.getAsMap("limits", String.class, Integer.class);

// Complex generic types
GenericsDeclaration listMapType = GenericsDeclaration.of(
    List.class,
    Collections.singletonList(
        GenericsDeclaration.of(Map.class, Arrays.asList(String.class, Duration.class))
    )
);
List<Map<String, Duration>> complex = view.get("schedule", listMapType);
```

### Typed Write Methods (TypedKeyWriter)

All write methods support automatic simplification for serialization:

| Method | Return | Description |
|--------|--------|-------------|
| `set(key, value)` | `Object` | Set with auto-simplification, returns old value |
| `set(key, value, Class<T>)` | `Object` | Set with explicit type |
| `set(key, value, GenericsDeclaration)` | `Object` | Set with full generic type |
| `setCollection(key, collection, elementType)` | `void` | Set collection with element type |
| `setCollection(key, collection, GenericsDeclaration)` | `void` | Set collection with full type |
| `setArray(key, array, elementType)` | `void` | Set array with element type |
| `setMap(key, map, keyType, valueType)` | `void` | Set map with key/value types |
| `setMap(key, map, GenericsDeclaration)` | `void` | Set map with full type |

**Write examples:**
```java
// Simple writes with auto-simplification
view.set("server.port", 8080);
view.set("server.host", "localhost");
view.set("enabled", true);

// With explicit types
view.set("timeout", Duration.ofSeconds(30), Duration.class);

// Collections
view.setCollection("tags", Arrays.asList("prod", "web"), String.class);
view.setArray("ports", new Integer[]{80, 443}, Integer.class);

// Maps
Map<String, Integer> limits = new HashMap<>();
limits.put("max-connections", 100);
view.setMap("limits", limits, String.class, Integer.class);

// Complex generic types
List<Map<String, String>> records = createRecords();
GenericsDeclaration type = GenericsDeclaration.of(
    List.class,
    Collections.singletonList(
        GenericsDeclaration.of(Map.class, Arrays.asList(String.class, String.class))
    )
);
view.setCollection("records", records, type);
```

### Nested Key Access

All methods support dot notation for nested structures:

```java
// Read nested values
String dbHost = view.get("server.database.host", String.class);
Integer dbPort = view.get("server.database.port", Integer.class);

// Write nested values (creates intermediate maps automatically)
view.set("server.database.host", "localhost");
view.set("server.database.port", 5432);

// Check and remove nested keys
if (view.exists("server.old.config")) {
    view.remove("server.old.config");
}
```

### Migration Example

**Recommended: Use DSL helpers for simple transformations**
```java
import static eu.okaeri.configs.migrate.ConfigMigrationDsl.*;

// Convert old timeout formats to Duration objects using move() with transform function
ConfigMigration migration = multi(
    move("timeout-seconds", "timeout", seconds -> Duration.ofSeconds((Integer) seconds)),
    move("retry-delay-ms", "retry-delay", millis -> Duration.ofMillis((Integer) millis)),
    move("blacklist", "security.blocked-users", str ->
        new HashSet<>(Arrays.asList(((String) str).split(","))))
);
```

**Manual approach with RawConfigView for complex scenarios:**
```java
// Use RawConfigView when you need conditional logic or multi-step transformations
ConfigMigration migration = (config, view) -> {
    if (!view.exists("connection.pool")) {
        return false;
    }

    // Read old pool configuration
    Map<String, Object> oldPool = view.getAsMap("connection.pool", String.class, Object.class);
    Integer maxSize = (Integer) oldPool.get("max-size");
    Integer minSize = (Integer) oldPool.get("min-size");

    // Transform: split into separate pools by priority
    if (maxSize != null && maxSize > 10) {
        // High-load config: create separate pools
        view.set("connection.primary-pool.size", maxSize / 2);
        view.set("connection.secondary-pool.size", maxSize / 2);
        view.set("connection.strategy", "load-balanced");
    } else {
        // Low-load config: single pool
        view.set("connection.primary-pool.size", maxSize != null ? maxSize : 5);
        view.set("connection.strategy", "single");
    }

    // Copy timeout with type conversion
    Duration timeout = view.getOr("connection.timeout-ms", Integer.class, 5000);
    view.set("connection.primary-pool.timeout", Duration.ofMillis(timeout), Duration.class);

    view.remove("connection.pool");
    return true;
};
```

## Named Migrations

`NamedMigration` adds a description to migrations (logged when applied).

### Creating Named Migrations

```java
import eu.okaeri.configs.migrate.builtin.NamedMigration;
import static eu.okaeri.configs.migrate.ConfigMigrationDsl.*;

public class C0001_Fix_server_key extends NamedMigration {

    public C0001_Fix_server_key() {
        super(
            "fixes server key naming inconsistency",
            move("server-Host", "server-host")
        );
    }
}
```

**With multiple operations:**

```java
public class C0002_Restructure_database extends NamedMigration {

    public C0002_Restructure_database() {
        super(
            "migrates database config to nested structure",
            multi(
                move("db-host", "database.host"),
                move("db-port", "database.port"),
                move("db-name", "database.name"),
                delete("db-legacy-field")
            )
        );
    }
}
```

### Logging

Named migrations automatically log their description:

```
INFO: C0001_Fix_server_key: fixes server key naming inconsistency
INFO: C0002_Restructure_database: migrates database config to nested structure
```

## Applying Migrations

### Single Migration

```java
MyConfig config = ConfigManager.create(MyConfig.class, (it) -> {
    it.configure(opt -> {
        opt.configurer(new YamlSnakeYamlConfigurer());
        opt.bindFile("config.yml");
    });
    it.load(); // load only, don't save after
});

// Apply single migration
config.migrate(new C0001_Fix_server_key());
```

### Multiple Migrations

```java
// Apply all migrations in order
config.migrate(
    new C0001_Fix_server_key(),
    new C0002_Restructure_database(),
    new C0003_Add_defaults()
);
```

### Automatic Migration on Load

```java
MyConfig config = ConfigManager.create(MyConfig.class, (it) -> {
    it.configure(opt -> {
        opt.configurer(new YamlSnakeYamlConfigurer());
        opt.bindFile("config.yml");
    });
    it.load(); // load only, don't save after

    // Migrate after loading
    it.migrate(
        new C0001_Fix_server_key(),
        new C0002_Restructure_database()
    );
});
```

### Migration with Callback

```java
config.migrate(
    (performedCount) -> {
        if (performedCount > 0) {
            System.out.println("Applied " + performedCount + " migrations");
        }
    },
    new C0001_Fix_server_key(),
    new C0002_Restructure_database()
);
```

### Version-Based Migrations

```java
@Getter
@Setter
public class AppConfig extends OkaeriConfig {

    @Comment("Config version (DO NOT EDIT)")
    private Integer configVersion = 3;

    // ... other fields
}

// Apply version-specific migrations
ConfigMigration versionMigration = (config, view) -> {
    Integer version = (Integer) view.get("configVersion");

    if (version == null || version < 1) {
        // Apply v1 migrations
        new C0001_Migrate_to_v1().migrate(config, view);
        view.set("configVersion", 1);
    }

    if (version < 2) {
        // Apply v2 migrations
        new C0002_Migrate_to_v2().migrate(config, view);
        view.set("configVersion", 2);
    }

    if (version < 3) {
        // Apply v3 migrations
        new C0003_Migrate_to_v3().migrate(config, view);
        view.set("configVersion", 3);
    }

    return true;
};

config.migrate(versionMigration);
```

## Best Practices

### ✅ Do's

1. **Use numbered migration files:**
   ```
   C0001_First_migration.java
   C0002_Second_migration.java
   C0003_Third_migration.java
   ```

2. **Add descriptive migration names:**
   ```java
   new NamedMigration(
       "migrates server config from flat to nested structure",
       multi(...)
   )
   ```

3. **Document what changed:**
   ```java
   /**
    * Migration C0001: Server key case fix
    *
    * Changes:
    * - Renames "serverHost" to "server-host"
    * - Renames "serverPort" to "server-port"
    */
   ```

4. **Test migrations:**
   ```java
   @Test
   void testC0001_FixesServerKeyCase() {
       // Create old config
       config.set("serverHost", "localhost");

       // Apply migration
       new C0001_Fix_server_key().migrate(config, view);

       // Verify
       assertThat(view.exists("server-host")).isTrue();
       assertThat(view.exists("serverHost")).isFalse();
   }
   ```

5. **Use DSL helpers:**
   ```java
   // ✅ Good - clear intent
   move("old-key", "new-key")

   // ❌ Avoid - verbose
   (config, view) -> {
       if (view.exists("old-key")) {
           view.set("new-key", view.get("old-key"));
           view.remove("old-key");
           return true;
       }
       return false;
   }
   ```

### ❌ Don'ts

1. **Don't modify migrations after release:**
   ```java
   // ❌ Bad - breaks existing deployments
   // Changed C0001 after users already applied it

   // ✅ Good - create new migration
   // C0002_Fix_C0001_issue.java
   ```


## Complex Migration Scenarios

### Restructuring to Nested Config

```java
public class C0001_Migrate_server_to_nested extends NamedMigration {

    public C0001_Migrate_server_to_nested() {
        super(
            "migrates flat server config to nested structure",
            when(
                any(
                    exists("serverHost"),
                    exists("serverPort"),
                    exists("serverTimeout")
                ),
                multi(
                    move("serverHost", "server.host"),
                    move("serverPort", "server.port"),
                    move("serverTimeout", "server.timeout"),
                    delete("server-legacy")
                )
            )
        );
    }
}
```

**Before:**
```yaml
serverHost: localhost
serverPort: 8080
serverTimeout: 30
```

**After:**
```yaml
server:
  host: localhost
  port: 8080
  timeout: 30
```

### Type Transformation

```java
public class C0002_Convert_timeout_to_seconds extends NamedMigration {

    public C0002_Convert_timeout_to_seconds() {
        super(
            "converts timeout from milliseconds to seconds",
            when(
                exists("timeout-ms"),
                move("timeout-ms", "timeout-seconds",
                    value -> ((Integer) value) / 1000)
            )
        );
    }
}
```

### Conditional Multi-Step Migration

```java
public class C0003_Database_migration extends NamedMigration {

    public C0003_Database_migration() {
        super(
            "migrates database configuration",
            when(
                match("config-version", (Integer v) -> v == 1),
                multi(
                    // Step 1: Backup
                    copy("database", "database-v1-backup"),

                    // Step 2: Restructure
                    move("database.url", "database.connection.url"),
                    move("database.user", "database.connection.username"),
                    move("database.pass", "database.connection.password"),

                    // Step 3: Add defaults
                    supply("database.pool.min-size", () -> 5),
                    supply("database.pool.max-size", () -> 20),

                    // Step 4: Update version
                    update("config-version", v -> 2)
                )
            )
        );
    }
}
```

### Merging Multiple Keys

```java
public class C0004_Merge_name_fields extends NamedMigration {

    public C0004_Merge_name_fields() {
        super(
            "merges firstName and lastName into fullName",
            (config, view) -> {
                if (view.exists("firstName") && view.exists("lastName")) {
                    String first = (String) view.get("firstName");
                    String last = (String) view.get("lastName");
                    String full = first + " " + last;

                    view.set("fullName", full);
                    view.remove("firstName");
                    view.remove("lastName");

                    return true;
                }
                return false;
            }
        );
    }
}
```

### Splitting a Key

```java
public class C0005_Split_full_name extends NamedMigration {

    public C0005_Split_full_name() {
        super(
            "splits fullName into firstName and lastName",
            (config, view) -> {
                if (view.exists("fullName")) {
                    String full = (String) view.get("fullName");
                    String[] parts = full.split(" ", 2);

                    view.set("firstName", parts[0]);
                    view.set("lastName", parts.length > 1 ? parts[1] : "");
                    view.remove("fullName");

                    return true;
                }
                return false;
            }
        );
    }
}
```

### All-or-Nothing Migration

```java
public class C0006_Require_all_credentials extends NamedMigration {

    public C0006_Require_all_credentials() {
        super(
            "migrates credentials only if all are present",
            when(
                all(
                    exists("username"),
                    exists("password"),
                    exists("api-key")
                ),
                multi(
                    move("username", "credentials.username"),
                    move("password", "credentials.password"),
                    move("api-key", "credentials.api-key")
                )
            )
        );
    }
}
```

## Next Steps

- **[Configuration Basics](Configuration-Basics)** - Understanding config structure
- **[Advanced Topics](Advanced-Topics)** - Custom serializers and transformers
- **[Troubleshooting](Troubleshooting)** - Migration issues and solutions

## See Also

- **[Examples & Recipes](Examples-and-Recipes)** - Migration examples in context
- **[Validation](Validation)** - Validating configs after migration
