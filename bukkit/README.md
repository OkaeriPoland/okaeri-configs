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
  <version>1.0.0</version>
</dependency>
```
### Gradle
Add repository to the `repositories` section:
```groovy
maven { url "https://storehouse.okaeri.eu/repository/maven-public/" }
```
Add dependency to the `maven` section:
```groovy
implementation 'eu.okaeri:okaeri-configs-bukkit:1.0.0'
```

## Limitations
- Bukkit's YamlConfiguration does not have an easy way to inject property comments. 
  Only top level comments are supported at the moment.

## Usage

```java
final class Demo {
    public static void main(String[] args) {

        // prepare bindFile, configurer
        File bindFile = new File("config.yml");
        BukkitConfigurer configurer = new BukkitConfigurer(new YamlConfiguration());

        // attach properties to config implementation
        TestConfig config = new TestConfig();
        config.setBindFile(bindFile);
        config.setConfigurer(configurer);

        // save defaults if file does not exists
        if (!bindFile.exists()) {
            config.save();
        }

        // load from file
        config.load();
    }
}
```