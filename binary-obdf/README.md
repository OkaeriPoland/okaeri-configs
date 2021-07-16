# Okaeri Configs | Binary (okaeri-bin)

Based on [OkaeriPoland/okaeri-bin](https://github.com/OkaeriPoland/okaeri-bin), simple binary-like data format with built-in data deduplication. 
Provides ability to save and read configs from disk with minimal source code size increase (`60kB core library + 17kB okaeri-bin = 77kB`).

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
  <artifactId>okaeri-configs-binary-obdf</artifactId>
  <version>2.7.24</version>
</dependency>
```
### Gradle
Add repository to the `repositories` section:
```groovy
maven { url "https://storehouse.okaeri.eu/repository/maven-public/" }
```
Add dependency to the `maven` section:
```groovy
implementation 'eu.okaeri:okaeri-configs-binary-obdf:2.7.24'
```

## Limitations
- OBDF is a binary format, and thus does not support comments. All `@Header` and `@Comment` values would not be added to the output configuration file.

## Usage

Please use ObdfConfigurer as your configurer:
```java
new ObdfConfigurer()
```
