# Okaeri Configs | JSON (Jackson)

Based on [FasterXML/jackson-databind](https://github.com/FasterXML/jackson-databind), a high-performance JSON library.

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
  <artifactId>okaeri-configs-json-jackson</artifactId>
  <version>6.0.0-beta.23</version>
</dependency>
```

### Gradle (Kotlin)

Add repository to the `repositories` section:

```kotlin
maven("https://repo.okaeri.cloud/releases")
```

Add dependency to the `dependencies` section:

```kotlin
implementation("eu.okaeri:okaeri-configs-json-jackson:6.0.0-beta.23")
```

## Limitations

- JSON does not support comments. All `@Header` and `@Comment` values will not be added to the output configuration file.

## Usage

Please use JsonJacksonConfigurer as your configurer:

```java
// default
new JsonJacksonConfigurer()
// attach own ObjectMapper instance (e.g., with custom configuration)
new JsonJacksonConfigurer(customObjectMapper)
```
