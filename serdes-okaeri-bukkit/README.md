# Okaeri Configs | okaeri-commons-bukkit

Serializers/Deserializers/Transformers for [okaeri-commons-bukkit](https://github.com/OkaeriPoland/okaeri-commons) types.

```java
new SerdesOkaeriBukkit()
```

## Installation

### Maven

Add dependency to the `dependencies` section:

```xml
<dependency>
  <groupId>eu.okaeri</groupId>
  <artifactId>okaeri-configs-serdes-okaeri-bukkit</artifactId>
  <version>6.0.0-beta.13</version>
</dependency>
```

### Gradle (Kotlin)

Add dependency to the `dependencies` section:

```kotlin
implementation("eu.okaeri:okaeri-configs-serdes-okaeri-bukkit:6.0.0-beta.13")
```

## Supported types

### Serializers

| Class | Params |
|-|-|
| eu.okaeri.commons.bukkit.material.TagMaterialSet | tags, materials |
