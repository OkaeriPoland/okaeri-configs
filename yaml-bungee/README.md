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
  <version>4.0.0-beta17</version>
</dependency>
```

### Gradle

Add repository to the `repositories` section:

```groovy
maven { url "https://storehouse.okaeri.eu/repository/maven-public/" }
```

Add dependency to the `maven` section:

```groovy
implementation 'eu.okaeri:okaeri-configs-yaml-bungee:4.0.0-beta17'
```

## Limitations

- Comments do not work on the elements of Collection or Map.

## Usage

Please use YamlBungeeConfigurer as your configurer:

```java
// default ('# ', '')
new YamlBungeeConfigurer()
// add empty spaces between sections
new YamlBungeeConfigurer(SectionSeparator.NEW_LINE)
// change comment character and section separator (no space after # in comments, empty newlines)
new YamlBungeeConfigurer("#", SectionSeparator.NEW_LINE)
```

