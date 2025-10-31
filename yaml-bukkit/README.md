# Okaeri Configs | Bukkit

An example plugin is available in [yaml-bukkit-example](https://github.com/OkaeriPoland/okaeri-configs/tree/master/yaml-bukkit-example). For more real-life usage examples
see [okaeri-minecraft](https://github.com/OkaeriPoland/okaeri-minecraft) repository.

## Installation

### Maven

Add repository to the `repositories` section:

```xml
<repository>
    <id>okaeri-repo</id>
    <url>https://storehouse.okaeri.eu/repository/maven-public/</url>
</repository>
```

Add dependency to the `dependencies` section:

```xml
<dependency>
  <groupId>eu.okaeri</groupId>
  <artifactId>okaeri-configs-yaml-bukkit</artifactId>
  <version>6.0.0-beta.3</version>
</dependency>
```

Additionally, if you want to serialize/deserialize [supported Bukkit objects](https://github.com/OkaeriPoland/okaeri-configs/tree/master/serdes-bukkit):

```xml
<dependency>
  <groupId>eu.okaeri</groupId>
  <artifactId>okaeri-configs-serdes-bukkit</artifactId>
  <version>6.0.0-beta.3</version>
</dependency>
```

### Gradle (Kotlin)

Add repository to the `repositories` section:

```kotlin
maven("https://storehouse.okaeri.eu/repository/maven-public/")
```

Add dependency to the `dependencies` section:

```kotlin
implementation("eu.okaeri:okaeri-configs-yaml-bukkit:6.0.0-beta.3")
```

Additionally, if you want to serialize/deserialize [supported Bukkit objects](https://github.com/OkaeriPoland/okaeri-configs/tree/master/serdes-bukkit):

```kotlin
implementation("eu.okaeri:okaeri-configs-serdes-bukkit:6.0.0-beta.3")
```

## Limitations

- Comments do not work on the elements of Collection or Map.

## Usage

Please use YamlBukkitConfigurer as your configurer:

```java
new YamlBukkitConfigurer()
```

For [serializers/deserializers/transformers](https://github.com/OkaeriPoland/okaeri-configs/tree/master/serdes-bukkit) use:

```java
new SerdesBukkit()
```
