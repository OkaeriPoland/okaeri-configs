# Okaeri Configs | Binary

Based on Java built-in ObjectOutputStream/ObjectInputStream. No external dependencies required.

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
  <artifactId>okaeri-configs-binary</artifactId>
  <version>6.0.0-beta.14</version>
</dependency>
```

### Gradle (Kotlin)

Add repository to the `repositories` section:

```kotlin
maven("https://repo.okaeri.cloud/releases")
```

Add dependency to the `dependencies` section:

```kotlin
implementation("eu.okaeri:okaeri-configs-binary:6.0.0-beta.14")
```

## Usage

```java
new BinaryConfigurer()
```

## Limitations

- Binary format is not human-readable
- Does not support comments (`@Header`, `@Comment` annotations are ignored)
- Does not provide Rust-style error messages
