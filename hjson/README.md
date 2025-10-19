# Okaeri Configs | HJSON (hjson-java)

Based on [OkaeriPoland/okaeri-hjson](https://github.com/OkaeriPoland/okaeri-hjson), a modification of the official HJSON implementation for Java.

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
  <artifactId>okaeri-configs-hjson</artifactId>
  <version>6.0.0-beta.1</version>
</dependency>
```

### Gradle (Kotlin)

Add repository to the `repositories` section:

```kotlin
maven("https://storehouse.okaeri.eu/repository/maven-public/")
```

Add dependency to the `dependencies` section:

```kotlin
implementation("eu.okaeri:okaeri-configs-hjson:6.0.0-beta.1")
```

## Usage

Please use HjsonConfigurer as your configurer:

```java
new HjsonConfigurer()
```
