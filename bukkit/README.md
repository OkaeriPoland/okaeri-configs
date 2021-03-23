# Okaeri Configs | Bukkit

An example plugin is available in [bukkit-example](https://github.com/OkaeriPoland/okaeri-configs/tree/master/bukkit-example).
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
  <artifactId>okaeri-configs-bukkit</artifactId>
  <version>1.5.0</version>
</dependency>
```
Additionally if you want to serialize/deserialize [supported bukkit objects](https://github.com/OkaeriPoland/okaeri-configs/tree/master/bukkit-serdes):
```xml
<dependency>
  <groupId>eu.okaeri</groupId>
  <artifactId>okaeri-configs-bukkit-serdes</artifactId>
  <version>1.5.0</version>
</dependency>
```
### Gradle
Add repository to the `repositories` section:
```groovy
maven { url "https://storehouse.okaeri.eu/repository/maven-public/" }
```
Add dependency to the `maven` section:
```groovy
implementation 'eu.okaeri:okaeri-configs-bukkit:1.5.0'
```

## Limitations
- Bukkit's YamlConfiguration does not have an easy way to inject property comments. 
  Only top level comments are supported at the moment.

## Usage

Please use BukkitConfigurer as your configurer:
```java
new BukkitConfigurer()
```
For [serializers/deserializers/transformers](https://github.com/OkaeriPoland/okaeri-configs/tree/master/bukkit-serdes) use:
```java
new BukkitSerdes()
```
