# Okaeri Configs | Bukkit

Serializers/Deserializers/Transformers for common but not mandatory types.

```java
new SerdesCommons()
```

## Installation

### Maven

Add dependency to the `dependencies` section:

```xml
<dependency>
  <groupId>eu.okaeri</groupId>
  <artifactId>okaeri-configs-serdes-commons</artifactId>
  <version>5.0.12</version>
</dependency>
```

### Gradle

Add dependency to the `maven` section:

```groovy
implementation 'eu.okaeri:okaeri-configs-serdes-commons:5.0.12'
```

## Supported types

### Transformers

| Side | Side | Type |
|-|-|-|
| java.lang.String | java.time.Instant | Two-side |
| java.lang.String | java.util.Locale | Two-side |
| java.lang.String | java.util.regex.Pattern | Two-side |
| java.lang.String | java.time.Duration | Two-side |
