# Okaeri Configs | JSON (json-simple)

Based on [fangyidong/json-simple](https://github.com/fangyidong/json-simple). Not recommended as configuration file provider due to heavy limitations.

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
  <artifactId>okaeri-configs-json-simple</artifactId>
  <version>5.0.5</version>
</dependency>
```

### Gradle

Add repository to the `repositories` section:

```groovy
maven { url "https://storehouse.okaeri.eu/repository/maven-public/" }
```

Add dependency to the `maven` section:

```groovy
implementation 'eu.okaeri:okaeri-configs-json-simple:5.0.5'
```

## Limitations

- The `com.googlecode.json-simple:json-simple` is in fact `simple` and does not allow for pretty print (intent) to be applied.
- JSON does not support comments. All `@Header` and `@Comment` values would not be added to the output configuration file.

## Usage

Please use JsonSimpleConfigurer as your configurer:

```java
new JsonSimpleConfigurer()
```
