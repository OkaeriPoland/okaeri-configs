# Okaeri Configs | okaeri-commons

Serializers/Deserializers/Transformers for [okaeri-commons](https://github.com/OkaeriPoland/okaeri-commons) types.

```java
new SerdesOkaeri()
```

## Installation

### Maven

Add dependency to the `dependencies` section:

```xml
<dependency>
  <groupId>eu.okaeri</groupId>
  <artifactId>okaeri-configs-serdes-okaeri</artifactId>
  <version>6.0.0-beta.1</version>
</dependency>
```

### Gradle (Kotlin)

Add dependency to the `dependencies` section:

```kotlin
implementation("eu.okaeri:okaeri-configs-serdes-okaeri:6.0.0-beta.1")
```

## Supported types

### Serializers

| Class | Params |
|-|-|
| eu.okaeri.commons.indexedset.IndexedSet | *(dynamic)* |

### Transformers

| Side | Side | Type |
|-|-|-|
| java.lang.String | eu.okaeri.commons.RomanNumeral | Two-side |
