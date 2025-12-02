# Okaeri Configs | YAML (Jackson)

Based on [FasterXML/jackson-dataformat-yaml](https://github.com/FasterXML/jackson-dataformats-text/tree/2.x/yaml), which uses SnakeYAML underneath.

Useful when Jackson is already a dependency in your project.

## Installation

### Maven

Add repository to the `repositories` section:

```xml
<repository>
    <id>okaeri-releases</id>
    <url>https://repo.okaeri.cloud/releases</url>
</repository>
```

Add dependency to the `dependencies` section:

```xml
<dependency>
  <groupId>eu.okaeri</groupId>
  <artifactId>okaeri-configs-yaml-jackson</artifactId>
  <version>6.0.0-beta.12</version>
</dependency>
```

### Gradle (Kotlin)

Add repository to the `repositories` section:

```kotlin
maven("https://repo.okaeri.cloud/releases")
```

Add dependency to the `dependencies` section:

```kotlin
implementation("eu.okaeri:okaeri-configs-yaml-jackson:6.0.0-beta.12")
```

## Limitations

- Long strings with spaces use escaped line continuation style (`"text\ continues"`) instead of folded style.

## Usage

Please use YamlJacksonConfigurer as your configurer:

```java
// default
new YamlJacksonConfigurer()
// attach own ObjectMapper instance (e.g., with custom configuration)
new YamlJacksonConfigurer(customYamlMapper)
```
