# Okaeri Configs | Bukkit

An example plugin is available in [bukkit-example](https://github.com/OkaeriPoland/okaeri-configs/tree/master/yaml-bukkit-example).
For more real-life usage examples see [okaeri-minecraft](https://github.com/OkaeriPoland/okaeri-minecraft) repository.

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
  <version>2.7.29</version>
</dependency>
```
Additionally if you want to serialize/deserialize [supported bukkit objects](https://github.com/OkaeriPoland/okaeri-configs/tree/master/serdes-bukkit):
```xml
<dependency>
  <groupId>eu.okaeri</groupId>
  <artifactId>okaeri-configs-serdes-bukkit</artifactId>
  <version>2.7.29</version>
</dependency>
```
### Gradle
Add repository to the `repositories` section:
```groovy
maven { url "https://storehouse.okaeri.eu/repository/maven-public/" }
```
Add dependency to the `maven` section:
```groovy
implementation 'eu.okaeri:okaeri-configs-yaml-bukkit:2.7.29'
```

## Limitations
- Comments do not work on the elements of Collection or Map.

## Usage

Please use YamlBukkitConfigurer as your configurer:
```java
// default ('# ', '')
new YamlBukkitConfigurer()
// add empty spaces between sections
new YamlBukkitConfigurer(SectionSeparator.NEW_WLINE)
// change comment character and section separator (no space after # in comments, empty newlines)
new YamlBukkitConfigurer("#", SectionSeparator.NEW_WLINE)
```
For [serializers/deserializers/transformers](https://github.com/OkaeriPoland/okaeri-configs/tree/master/serdes-bukkit) use:
```java
new SerdesBukkit()
```
