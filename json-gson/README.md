# Okaeri Configs | JSON (gson)

Based on [google/gson](https://github.com/google/gson), a popular json library.

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
  <artifactId>okaeri-configs-json-gson</artifactId>
  <version>2.5.2</version>
</dependency>
```
### Gradle
Add repository to the `repositories` section:
```groovy
maven { url "https://storehouse.okaeri.eu/repository/maven-public/" }
```
Add dependency to the `maven` section:
```groovy
implementation 'eu.okaeri:okaeri-configs-json-gson:2.5.2'
```

## Limitations
- JSON does not support comments. All `@Header` and `@Comment` values would not be added to the output configuration file.

## Usage

Please use JsonGsonConfigurer as your configurer:
```java
// default
new JsonGsonConfigurer()
// attach own Gson instance (eg. without pretty print)
new JsonGsonConfigurer(new Gson())
```
