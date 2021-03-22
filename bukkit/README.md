# Okaeri Configs | Bukkit

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
  <version>1.1.0</version>
</dependency>
```
### Gradle
Add repository to the `repositories` section:
```groovy
maven { url "https://storehouse.okaeri.eu/repository/maven-public/" }
```
Add dependency to the `maven` section:
```groovy
implementation 'eu.okaeri:okaeri-configs-bukkit:1.1.0'
```

## Limitations
- Bukkit's YamlConfiguration does not have an easy way to inject property comments. 
  Only top level comments are supported at the moment.

## Usage

Please use BukkitConfigurer as your configurer:
```java
new BukkitConfigurer()
```
