# Okaeri Configs | Bungee

Support for BungeeCord's YamlConfiguration.

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
  <artifactId>okaeri-configs-yaml-bungee</artifactId>
  <version>5.0.2</version>
</dependency>
```

### Gradle

Add repository to the `repositories` section:

```groovy
maven { url "https://storehouse.okaeri.eu/repository/maven-public/" }
```

Add dependency to the `maven` section:

```groovy
implementation 'eu.okaeri:okaeri-configs-yaml-bungee:5.0.2'
```

## Limitations

- Comments do not work on the elements of Collection or Map.

## Usage

Please use YamlBungeeConfigurer as your configurer:

```java
new YamlBungeeConfigurer()
```

