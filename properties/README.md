# Okaeri Configs | Properties

Based on Java built-in Properties format with dot notation for nested structures. No external dependencies required.

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
  <version>6.0.0-beta.9</version>
</dependency>
```

### Gradle (Kotlin)

Add repository to the `repositories` section:

```kotlin
maven("https://repo.okaeri.cloud/releases")
```

Add dependency to the `dependencies` section:

```kotlin
implementation("eu.okaeri:okaeri-configs-properties:6.0.0-beta.9")
```

## Usage

```java
new PropertiesConfigurer()
```

Example output:

```properties
# My Application Config

# The database host
host=localhost
port=5432
features=logging,metrics
# Index notation is used for lists with commas in values or lines >80 chars
allowedOrigins.0=https://example.com
allowedOrigins.1=https://api.example.com,https://cdn.example.com
# List of database connections
databases.0.name=primary
databases.0.url=jdbc:mysql://localhost:3306/app
databases.1.name=replica
databases.1.url=jdbc:mysql://localhost:3307/app
```

## Limitations

- Nested structures use dot notation which can be verbose for deep nesting
- Non-ASCII characters are escaped as `\uXXXX` (standard Properties behavior)
- Null values use `__null__` marker
