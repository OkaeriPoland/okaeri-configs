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
  <version>5.0.0-beta.5</version>
</dependency>
```

### Gradle

Add dependency to the `maven` section:

```groovy
implementation 'eu.okaeri:okaeri-configs-serdes-okaeri:5.0.0-beta.5'
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
