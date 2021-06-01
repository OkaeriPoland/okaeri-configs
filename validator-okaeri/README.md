# Okaeri Configs | validator-okaeri

Based on [OkaeriPoland/okaeri-validator](https://github.com/OkaeriPoland/okaeri-validator).
Simple and small alternative to [validator-jakartaee](https://github.com/OkaeriPoland/okaeri-configs/tree/master/validator-jakartaee).
See more in the official [README](https://github.com/OkaeriPoland/okaeri-validator#readme).

## Installation
Note: One of okaeri-configs configurers (yaml-bukkit, json-gson, etc.) is required.
### Maven
Add dependency to the `dependencies` section:
```xml
<dependency>
  <groupId>eu.okaeri</groupId>
  <artifactId>okaeri-configs-validator-okaeri</artifactId>
  <version>2.7.19</version>
</dependency>
```
### Gradle
```groovy
implementation 'eu.okaeri:okaeri-configs-validator-okaeri:2.7.19'
```

## Usage

Please wrap your current Configurer with OkaeriValidator:
```java
// simple
new OkaeriValidator(yourConfigurer)
// force @NotNull policy by default (can be bypassed for single fields using @Nullable)
new OkaeriValidator(yourConfigurer, true)
```
