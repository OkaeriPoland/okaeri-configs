# Okaeri Configs | HOCON (lightbend)

Based on [lightbend/config](https://github.com/lightbend/config), a part of [Play Framework](https://www.playframework.com/) ecosystem. Not recommended for configurations with multiple levels due to
limitations.

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
  <artifactId>okaeri-configs-hocon-lightbend</artifactId>
  <version>6.0.0-beta.4</version>
</dependency>
```

### Gradle (Kotlin)

Add repository to the `repositories` section:

```kotlin
maven("https://storehouse.okaeri.eu/repository/maven-public/")
```

Add dependency to the `dependencies` section:

```kotlin
implementation("eu.okaeri:okaeri-configs-hocon-lightbend:6.0.0-beta.4")
```

## Limitations

Lightbend's config has poor support for dynamically generated configs and is missing features:

- Only top-level comments and headers are available (through standard ConfigPostprocessor hack), see lightbend/config:
    - [Allow comment on root element (or header comment) #481 from Jul 20, 2017](https://github.com/lightbend/config/issues/481)
    - [multi-line comments #152 from Mar 18, 2014](https://github.com/lightbend/config/issues/152)
- Only the top root is guaranteed to be ordered (Lightbend's HOCON stores configs as unordered maps), see lightbend/config:
    - [Keep config item order in ConfigObject #365 from Dec 28, 2015](https://github.com/lightbend/config/issues/365)
- HOCON features like includes and substitutions have not been tested and are not expected to work

## Usage

Please use HoconLightbendConfigurer as your configurer:

```java
new HoconLightbendConfigurer()
```
