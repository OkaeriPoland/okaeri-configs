# Okaeri Configs | SnakeYAML

Based on [asomov/snakeyaml](https://github.com/asomov/snakeyaml), a popular YAML library.

Can be used as a replacement for platform-dependent
[yaml-bukkit](https://github.com/OkaeriPoland/okaeri-configs/tree/master/yaml-bukkit) or
[yaml-bungee](https://github.com/OkaeriPoland/okaeri-configs/tree/master/yaml-bungee), but is more prone to version compatibility issues on these platforms (e.g., underlying SnakeYAML version
changing in the runtime environment). Please use the environment-specific implementation if you are not sure if this one suits your needs.

**Warning:** When using in non-standalone environments like Bukkit or Bungee modules were intended for, it is highly recommended to not shade SnakeYAML into the resulting plugin or, if required to do
so, use relocation. Ignoring this may result in incompatibilities.

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
  <artifactId>okaeri-configs-yaml-snakeyaml</artifactId>
  <version>5.0.13</version>
</dependency>
```

### Gradle (Kotlin)

Add repository to the `repositories` section:

```kotlin
maven("https://storehouse.okaeri.eu/repository/maven-public/")
```

Add dependency to the `dependencies` section:

```kotlin
implementation("eu.okaeri:okaeri-configs-yaml-snakeyaml:5.0.13")
```

## Limitations

- Comments do not work on the elements of Collection or Map.

## Usage

Please use YamlSnakeYamlConfigurer as your configurer:

```java
new YamlSnakeYamlConfigurer()
```
