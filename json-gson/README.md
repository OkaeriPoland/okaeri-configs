# Okaeri Configs | JSON (gson)

Based on [google/gson](https://github.com/google/gson), a popular JSON library.

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
  <artifactId>okaeri-configs-json-gson</artifactId>
  <version>6.0.0-beta.10</version>
</dependency>
```

### Gradle (Kotlin)

Add repository to the `repositories` section:

```kotlin
maven("https://repo.okaeri.cloud/releases")
```

Add dependency to the `dependencies` section:

```kotlin
implementation("eu.okaeri:okaeri-configs-json-gson:6.0.0-beta.10")
```

## Limitations

- JSON does not support comments. All `@Header` and `@Comment` values will not be added to the output configuration file.

## Usage

Please use JsonGsonConfigurer as your configurer:

```java
// default
new JsonGsonConfigurer()
// attach own Gson instance (e.g., without pretty print)
new JsonGsonConfigurer(new Gson())
```
