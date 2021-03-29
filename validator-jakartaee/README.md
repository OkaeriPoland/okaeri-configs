# Okaeri Configs | validator-jakartaee

Based on [hibernate/hibernate-validator](https://github.com/hibernate/hibernate-validator). 
Jakarta Bean Validation 3.0 is a powerful tool and the ultimate validation solution, but comes at the cost of additional ~2MB in the final jar size.
All supported annotations (`@NotNull`, `@Size`, `@Min`, `@Max`, `@Pattern`, etc.) are expected to be working.
See more at [eclipse-ee4j/jakartaee-tutorial](https://github.com/eclipse-ee4j/jakartaee-tutorial/blob/569bf35a26f8965936ebd02cde84a2dcc11291f7/src/main/asciidoc/bean-validation/bean-validation002.adoc).

## Installation
Note: One of okaeri-configs configurers (yaml-bukkit, json-gson, etc.) is required.
### Maven
Add dependency to the `dependencies` section:
```xml
<dependency>
  <groupId>eu.okaeri</groupId>
  <artifactId>okaeri-configs-validator-jakartaee</artifactId>
  <version>2.3.4</version>
</dependency>
```
### Gradle
```groovy
implementation 'eu.okaeri:okaeri-configs-validator-jakartaee:2.3.4'
```

## Usage

Please wrap your current Configurer with JakartaValidator:
```java
new JakartaValidator(yourConfigurer)
```
