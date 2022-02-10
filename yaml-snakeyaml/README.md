# Okaeri Configs | SnakeYAML

Based on [asomov/snakeyaml](https://github.com/asomov/snakeyaml), a popular yaml library.

Can be used as a replacement for platform dependant
[yaml-bukkit](https://github.com/OkaeriPoland/okaeri-configs/tree/master/yaml-bukkit) or
[yaml-bungee](https://github.com/OkaeriPoland/okaeri-configs/tree/master/yaml-bungee), but is a lot more prone to version compatibility issues on these platforms (e.g. underlying snakeyaml version
changing in the runtime environment). Please use environment specific implementation if you are not sure if this one suits your needs.

**Warning:** When using in non-standalone environment like bukkit or bungee modules were indented to, it is highly recommended to not shade snakeyaml into the resulting plugin or, if required to do
so, use relocation. Ignoring this may and will result in incompatibilities.

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
  <version>4.0.0-beta6</version>
</dependency>
```

### Gradle

Add repository to the `repositories` section:

```groovy
maven { url "https://storehouse.okaeri.eu/repository/maven-public/" }
```

Add dependency to the `maven` section:

```groovy
implementation 'eu.okaeri:okaeri-configs-yaml-snakeyaml:4.0.0-beta6'
```

## Limitations

- Comments do not work on the elements of Collection or Map.

## Usage

Please use YamlSnakeYamlConfigurer as your configurer:

```java
// default ('# ', '')
new YamlSnakeYamlConfigurer()
// add empty spaces between sections
new YamlSnakeYamlConfigurer(SectionSeparator.NEW_LINE)
// change comment character and section separator (no space after # in comments, empty newlines)
new YamlSnakeYamlConfigurer("#", SectionSeparator.NEW_LINE)
```
