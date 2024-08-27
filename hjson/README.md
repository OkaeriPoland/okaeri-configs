# Okaeri Configs | HJSON (hjson-java)

Based on [OkaeriPoland/okaeri-hjson](https://github.com/OkaeriPoland/okaeri-hjson), modification of official hjson implementation for java.

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
  <version>5.0.4</version>
</dependency>
```

### Gradle

Add repository to the `repositories` section:

```groovy
maven { url "https://storehouse.okaeri.eu/repository/maven-public/" }
```

Add dependency to the `maven` section:

```groovy
implementation 'eu.okaeri:okaeri-configs-hjson:5.0.4'
```

## Usage

Please use HjsonConfigurer as your configurer:

```java
new HjsonConfigurer()
```
