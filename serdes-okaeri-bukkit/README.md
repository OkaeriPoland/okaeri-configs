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
  <version>5.0.0-beta.5</version>
</dependency>
```

### Gradle

Add dependency to the `maven` section:

```groovy
implementation 'eu.okaeri:okaeri-configs-serdes-okaeri-bukkit:5.0.0-beta.5'
```

## Supported types

### Serializers

| Class | Params |
|-|-|
| eu.okaeri.commons.bukkit.material.TagMaterialSet | tags, materials |
