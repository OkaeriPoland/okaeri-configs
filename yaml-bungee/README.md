# Okaeri Configs | Bungee

Support for BungeeCord's YamlConfiguration.

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
  <artifactId>okaeri-configs-yaml-bungee</artifactId>
  <version>6.0.0-beta.20</version>
</dependency>
```

### Gradle (Kotlin)

Add repository to the `repositories` section:

```kotlin
maven("https://repo.okaeri.cloud/releases")
```

Add dependency to the `dependencies` section:

```kotlin
implementation("eu.okaeri:okaeri-configs-yaml-bungee:6.0.0-beta.20")
```

## Usage

Please use YamlBungeeConfigurer as your configurer:

```java
new YamlBungeeConfigurer()
```

