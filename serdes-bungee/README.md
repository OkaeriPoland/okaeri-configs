# Okaeri Configs | Bungee

Serializers/Deserializers/Transformers for Bimgee types. See [yaml-bungee](https://github.com/OkaeriPoland/okaeri-configs/tree/master/yaml-bungee).

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
  <version>4.0.8</version>
</dependency>
```

### Gradle

Add dependency to the `maven` section:

```groovy
implementation 'eu.okaeri:okaeri-configs-serdes-bungee:4.0.8'
```

## Supported types

### Transformers

| Side | Side | Type |
|-|-|-|
| java.lang.String | net.md_5.bungee.api.ChatColor | Two-side |
