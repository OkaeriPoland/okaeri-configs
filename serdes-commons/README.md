# Okaeri Configs | Commons

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
  <version>6.0.0-beta.11</version>
</dependency>
```

### Gradle (Kotlin)

Add dependency to the `dependencies` section:

```kotlin
implementation("eu.okaeri:okaeri-configs-serdes-commons:6.0.0-beta.11")
```

## Supported types

### Transformers

| Side | Side | Type |
|-|-|-|
| java.lang.String | java.time.Instant | Two-side |
| java.lang.String | java.util.Locale | Two-side |
| java.lang.String | java.util.regex.Pattern | Two-side |
| java.lang.String | java.time.Duration | Two-side |
