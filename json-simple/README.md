# Okaeri Configs | JSON (json-simple)

Based on [fangyidong/json-simple](https://github.com/fangyidong/json-simple). Not recommended as a configuration file provider due to heavy limitations.

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
  <artifactId>okaeri-configs-json-simple</artifactId>
  <version>6.0.0-beta.13</version>
</dependency>
```

### Gradle (Kotlin)

Add repository to the `repositories` section:

```kotlin
maven("https://repo.okaeri.cloud/releases")
```

Add dependency to the `dependencies` section:

```kotlin
implementation("eu.okaeri:okaeri-configs-json-simple:6.0.0-beta.13")
```

## Limitations

- The `com.googlecode.json-simple:json-simple` library is simple and does not allow for pretty print (indentation) to be applied.
- JSON does not support comments. All `@Header` and `@Comment` values will not be added to the output configuration file.

## Usage

Please use JsonSimpleConfigurer as your configurer:

```java
new JsonSimpleConfigurer()
```
