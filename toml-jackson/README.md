# Okaeri Configs | TOML (Jackson)

Based on [Jackson dataformat-toml](https://github.com/FasterXML/jackson-dataformats-text/tree/master/toml) for TOML 1.0 parsing with post-processing for proper section formatting.

## Installation

### Maven

Add repository to the `repositories` section:

```xml
<repository>
    <id>okaeri-releases</id>
    <url>https://repo.okaeri.cloud/releases</url>
</repository>
```

Add dependency to the `dependencies` section:

```xml
<dependency>
  <groupId>eu.okaeri</groupId>
  <artifactId>okaeri-configs-toml-jackson</artifactId>
  <version>6.0.0-beta.22</version>
</dependency>
```

### Gradle (Kotlin)

Add repository to the `repositories` section:

```kotlin
maven("https://repo.okaeri.cloud/releases")
```

Add dependency to the `dependencies` section:

```kotlin
implementation("eu.okaeri:okaeri-configs-toml-jackson:6.0.0-beta.22")
```

## Usage

```java
new TomlJacksonConfigurer()
```

Example output:

```toml
# ===================
# My Application Config
# ===================

# The database host
host = 'localhost'
port = 5432
features = ['logging', 'metrics']

# Settings use dotted keys (plain Map)
settings.debug = true
settings.timeout = 30

# Database config section (OkaeriConfig subclass)
[database]
name = 'primary'
url = 'jdbc:mysql://localhost:3306/app'

# Nested config sections use dotted table names
[database.pool]
minSize = 5
maxSize = 20
```

## Features

- Full TOML 1.0 parsing via Jackson
- Proper nested table sections (`[section]`) for `OkaeriConfig` subclasses
- Plain `Map` fields use dotted keys (not sections)
- Header and field comments support
- Configurable max section depth (default: 2)

## Configuration

```java
new TomlJacksonConfigurer()
    .setMaxSectionDepth(3)  // Increase nesting depth for sections
    .setMapper(customMapper)  // Use custom TomlMapper
```

## Limitations

- Null values use `__null__` marker (TOML has no native null)
- Large long values (outside double precision range) are stored as strings
- Section depth is limited to prevent excessive nesting (configurable via `maxSectionDepth`)
