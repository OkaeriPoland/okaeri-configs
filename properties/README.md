# Okaeri Configs | Properties & INI

Two flat key-value configuration formats in one module. No external dependencies required.

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
  <artifactId>okaeri-configs-properties</artifactId>
  <version>6.0.0-beta.14</version>
</dependency>
```

### Gradle (Kotlin)

Add repository to the `repositories` section:

```kotlin
maven("https://repo.okaeri.cloud/releases")
```

Add dependency to the `dependencies` section:

```kotlin
implementation("eu.okaeri:okaeri-configs-properties:6.0.0-beta.14")
```

## Usage

```java
new PropertiesConfigurer()  // flat key=value with # comments
new IniConfigurer()         // [section] headers with ; comments
```

Options:
```java
new PropertiesConfigurer().setEscapeUnicode(true)  // use \uXXXX for non-ASCII
new IniConfigurer().setMaxSectionDepth(3)          // increase section nesting
```

## Example

Same config in both formats:

**PropertiesConfigurer** (`new PropertiesConfigurer()`):
```properties
# My Application Config

appName=MyApp
features=logging,metrics
# Index notation for lists exceeding 80 chars
allowedOrigins.0=https://api.production.example.com
allowedOrigins.1=https://cdn.production.example.com
# Nested OkaeriConfig uses dot notation
database.host=localhost
database.port=5432
# List of objects
databases.0.name=primary
databases.0.url=jdbc:mysql://localhost:3306/app
databases.1.name=replica
databases.1.url=jdbc:mysql://localhost:3307/app
```

**IniConfigurer** (`new IniConfigurer()`):
```ini
; My Application Config

appName=MyApp
features=logging,metrics
; Index notation for lists exceeding 80 chars
allowedOrigins.0=https://api.production.example.com
allowedOrigins.1=https://cdn.production.example.com
; List of objects
databases.0.name=primary
databases.0.url=jdbc:mysql://localhost:3306/app
databases.1.name=replica
databases.1.url=jdbc:mysql://localhost:3307/app

; Nested OkaeriConfig becomes a section
[database]
host=localhost
port=5432
```

Only nested `OkaeriConfig` subclasses become INI sections. Lists use dot notation in both formats.

## Limitations

- Null values use `__null__` marker
