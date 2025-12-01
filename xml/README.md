# Okaeri Configs | XML

Based on Java built-in XML APIs (DOM, XMLEncoder/XMLDecoder). No external dependencies required.

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
  <artifactId>okaeri-configs-xml</artifactId>
  <version>6.0.0-beta.6</version>
</dependency>
```

### Gradle (Kotlin)

Add repository to the `repositories` section:

```kotlin
maven("https://repo.okaeri.cloud/releases")
```

Add dependency to the `dependencies` section:

```kotlin
implementation("eu.okaeri:okaeri-configs-xml:6.0.0-beta.6")
```

## Usage

This module provides two configurer implementations:

### XmlSimpleConfigurer (Recommended)

Human-readable XML format using DOM APIs. Uses element names for keys with a clean structure.

```java
new XmlSimpleConfigurer()
```

Example output:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!-- Configuration File -->
<!-- Database Settings -->
<config>
  <!-- The database host -->
  <host>localhost</host>
  <!-- Port number -->
  <port>5432</port>
  <items type="list">
    <item>first</item>
    <item>second</item>
  </items>
</config>
```

### XmlBeanConfigurer

Uses Java's XMLEncoder/XMLDecoder for serialization. Produces more verbose output.

```java
new XmlBeanConfigurer()
```

## Limitations

- `XmlBeanConfigurer` does not support comments. All `@Header` and `@Comment` values will not be added to the output.
