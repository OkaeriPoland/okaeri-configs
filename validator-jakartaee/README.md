# Okaeri Configs | validator-jakartaee

Jakarta Bean Validation is a powerful and standardized validation solution. This module integrates with the Jakarta Bean Validation API, enabling use of all standard validation annotations (`@NotNull`, `@Size`, `@Min`, `@Max`, `@Pattern`, etc.). See more at [eclipse-ee4j/jakartaee-tutorial](https://github.com/eclipse-ee4j/jakartaee-tutorial/blob/569bf35a26f8965936ebd02cde84a2dcc11291f7/src/main/asciidoc/bean-validation/bean-validation002.adoc).

> ⚠️ This module requires a Bean Validation implementation compatible with the Jakarta Bean Validation API at runtime.

## Installation

### Maven

Add dependency to the `dependencies` section:

```xml
<dependency>
  <groupId>eu.okaeri</groupId>
  <artifactId>okaeri-configs-validator-jakartaee</artifactId>
  <version>6.0.0-beta.8</version>
</dependency>
```

### Gradle (Kotlin)

```kotlin
implementation("eu.okaeri:okaeri-configs-validator-jakartaee:6.0.0-beta.8")
```

### Bean Validation Implementation

You must also include a Bean Validation implementation. The most common choice is Hibernate Validator:

#### Maven

```xml
<dependency>
  <groupId>org.hibernate.validator</groupId>
  <artifactId>hibernate-validator</artifactId>
  <version>9.0.1.Final</version>
</dependency>
```

#### Gradle (Kotlin)

```kotlin
implementation("org.hibernate.validator:hibernate-validator:9.0.1.Final")
```

**Version Compatibility:**

| Hibernate Validator | Bean Validation | Jakarta EE | Minimal Java |
|---------------------|-----------------|------------|--------------|
| 9.x                 | 3.1             | 11         | 17           |
| 8.x                 | 3.0             | 10         | 11           |
| 7.x                 | 3.0             | 9+         | 8            |

### Expression Language (Optional)

Jakarta Expression Language (EL) is only required if you use complex constraint violation messages with EL expressions. For basic validation with simple messages, you can skip this dependency.

#### Maven

```xml
<dependency>
  <groupId>org.glassfish.expressly</groupId>
  <artifactId>expressly</artifactId>
  <version>6.0.0</version>
</dependency>
```

#### Gradle (Kotlin)

```kotlin
implementation("org.glassfish.expressly:expressly:6.0.0")
```

## Usage

Please wrap your current Configurer with JakartaValidator:

```java
new JakartaValidator(yourConfigurer)
```

## Environment Considerations

### Dependency Management

Some runtime environments already provide Bean Validation implementations:

- **Jakarta EE Application Servers** (WildFly, Payara, TomEE, etc.) - Typically include both Hibernate Validator and an EL implementation.
- **Spring Boot** - Often includes Hibernate Validator via `spring-boot-starter-validation`. Check your dependency tree to avoid duplicates.
- **Standalone Applications** - You'll need to explicitly include both Hibernate Validator and optionally the EL implementation.

### Size Impact

Adding Hibernate Validator and its dependencies to a standalone application increases jar size by approximately 2-3 MB. The Expression Language implementation adds another ~500KB. If jar size is a critical concern and you don't need the full Jakarta Bean Validation specification, consider using [validator-okaeri](https://github.com/OkaeriPoland/okaeri-configs/tree/master/validator-okaeri) instead, which is a lightweight alternative with no external dependencies.
