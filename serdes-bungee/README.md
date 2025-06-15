# Okaeri Configs | Bungee

Serializers/Deserializers/Transformers for Bungee types. See [yaml-bungee](https://github.com/OkaeriPoland/okaeri-configs/tree/master/yaml-bungee).

```java
new SerdesBungee()
```

## Installation

### Maven

Add dependency to the `dependencies` section:

```xml
<dependency>
  <groupId>eu.okaeri</groupId>
  <artifactId>okaeri-configs-serdes-bungee</artifactId>
  <version>5.0.9</version>
</dependency>
```

### Gradle

Add dependency to the `maven` section:

```groovy
implementation 'eu.okaeri:okaeri-configs-serdes-bungee:5.0.9'
```

## Supported types

### Transformers

| Side | Side | Type |
|-|-|-|
| java.lang.String | net.md_5.bungee.api.ChatColor | Two-side |
