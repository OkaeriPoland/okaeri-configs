# Using Lombok

Guide to using Project Lombok with okaeri-configs for cleaner, more maintainable configuration classes.

## Table of Contents

- [What is Lombok?](#what-is-lombok)
- [Why Use Lombok with Okaeri Configs?](#why-use-lombok-with-okaeri-configs)
- [Installation](#installation)
- [Common Annotations](#common-annotations)
- [Examples](#examples)
- [IDE Setup](#ide-setup)
- [Alternatives](#alternatives)

## What is Lombok?

Project Lombok is a Java library that automatically generates boilerplate code (getters, setters, constructors, etc.) at compile time using annotations. This reduces verbose code and makes config classes cleaner and easier to maintain.

**Website:** https://projectlombok.org/

## Why Use Lombok with Okaeri Configs?

Okaeri Configs requires getters and setters for config fields. Without Lombok, you write:

```java
// Without Lombok - verbose
public class ServerConfig extends OkaeriConfig {

    private String host = "localhost";
    private Integer port = 8080;
    private Integer maxPlayers = 100;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Integer getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(Integer maxPlayers) {
        this.maxPlayers = maxPlayers;
    }
}
```

With Lombok, the same config becomes:

```java
// With Lombok - clean
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ServerConfig extends OkaeriConfig {
    private String host = "localhost";
    private Integer port = 8080;
    private Integer maxPlayers = 100;
}
```

**Benefits:**
- Less boilerplate code (70%+ reduction)
- Easier to read and maintain
- Fewer chances for copy-paste errors
- Focus on configuration structure, not accessor code

## Installation

### Maven

**1. Add Lombok dependency:**
```xml
<dependencies>
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <version>1.18.34</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

**2. Ensure annotation processing is enabled:**
```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.11.0</version>
            <configuration>
                <annotationProcessorPaths>
                    <path>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                        <version>1.18.34</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    compileOnly("org.projectlombok:lombok:1.18.34")
    annotationProcessor("org.projectlombok:lombok:1.18.34")
}
```

### Gradle (Groovy DSL)

```groovy
dependencies {
    compileOnly 'org.projectlombok:lombok:1.18.34'
    annotationProcessor 'org.projectlombok:lombok:1.18.34'
}
```

## Common Annotations

### @Getter and @Setter

Generate getters and setters for all fields:

```java
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppConfig extends OkaeriConfig {
    private String appName = "MyApp";
    private Integer port = 8080;
}
```

### @Data

Combines `@Getter`, `@Setter`, `@ToString`, `@EqualsAndHashCode`, and `@RequiredArgsConstructor`:

```java
import lombok.Data;
import java.io.Serializable;

@Data
public class User implements Serializable {
    private String username;
    private String email;
    private Integer level;
}
```

Use for Serializable POJOs in configs.

### @NoArgsConstructor / @AllArgsConstructor

Generate constructors automatically:

```java
import lombok.*;
import java.io.Serializable;

@Data
@NoArgsConstructor  // Empty constructor
@AllArgsConstructor  // Constructor with all fields
public class ServerDef implements Serializable {
    private String host;
    private Integer port;
    private String region;
}
```

### @NonNull

Add null checks to parameters:

```java
import lombok.NonNull;
import lombok.Setter;

@Setter
public class Config extends OkaeriConfig {

    private String name;

    // Setter will throw NullPointerException if value is null
    public void setName(@NonNull String name) {
        this.name = name;
    }
}
```

Lombok generates:

```java
public void setName(String name) {
    if (name == null) {
        throw new NullPointerException("name is marked non-null but is null");
    }
    this.name = name;
}
```

## Examples

### Basic Config

```java
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DatabaseConfig extends OkaeriConfig {

    @Comment("Database host")
    private String host = "localhost";

    @Comment("Database port")
    private Integer port = 5432;

    @Comment("Database name")
    private String database = "myapp";

    @Comment("Username")
    private String username = "root";

    @Comment("Password")
    private String password = "";
}
```

### Config with Subconfigs

```java
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppConfig extends OkaeriConfig {

    @Comment("Application metadata")
    private AppMetadata app = new AppMetadata();

    @Comment("Server settings")
    private ServerSettings server = new ServerSettings();

    @Getter
    @Setter
    public static class AppMetadata extends OkaeriConfig {
        private String name = "MyApp";
        private String version = "1.0.0";
    }

    @Getter
    @Setter
    public static class ServerSettings extends OkaeriConfig {
        private String host = "0.0.0.0";
        private Integer port = 8080;
    }
}
```

### Config with Serializable Objects

```java
import lombok.*;
import java.io.Serializable;

@Getter
@Setter
public class PlayersConfig extends OkaeriConfig {

    @Comment("Player data")
    private Map<String, PlayerData> players = new LinkedHashMap<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlayerData implements Serializable {
        private String username;
        private Integer level;
        private Double experience;
        private List<String> achievements;
    }
}
```

### Multi-Server Config

```java
import lombok.*;
import java.io.Serializable;

@Getter
@Setter
public class MultiServerConfig extends OkaeriConfig {

    @Comment("Server definitions")
    private Map<String, ServerDef> servers = new LinkedHashMap<>();

    public MultiServerConfig() {
        servers.put("primary", new ServerDef("primary.example.com", 8080));
        servers.put("secondary", new ServerDef("secondary.example.com", 8080));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServerDef implements Serializable {
        private String host;
        private Integer port;
    }
}
```

## IDE Setup

### IntelliJ IDEA

1. Install Lombok plugin:
   - `File` → `Settings` → `Plugins`
   - Search for "Lombok"
   - Install and restart IDE

2. Enable annotation processing:
   - `File` → `Settings` → `Build, Execution, Deployment` → `Compiler` → `Annotation Processors`
   - Check "Enable annotation processing"

### Eclipse

1. Download `lombok.jar` from https://projectlombok.org/download
2. Run: `java -jar lombok.jar`
3. Select Eclipse installation directory
4. Click "Install/Update"
5. Restart Eclipse

### VS Code

1. Install "Language Support for Java" extension
2. Lombok works automatically with Maven/Gradle projects

## Alternatives

If you prefer not to use Lombok, you can:

### 1. Write Getters/Setters Manually

```java
public class Config extends OkaeriConfig {
    private String name = "value";

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
```

### 2. Use IDE Code Generation

Most IDEs can generate getters/setters:
- **IntelliJ**: `Alt + Insert` → `Getter and Setter`
- **Eclipse**: `Right-click` → `Source` → `Generate Getters and Setters`
- **VS Code**: `Right-click` → `Source Action` → `Generate Getters and Setters`

### 3. Use Records (Java 14+)

For immutable configuration data:

```java
public record ServerConfig(String host, int port) {}
```

**Note:** Records are immutable - setters are not generated. Use for read-only configuration objects or data classes in configs.

## Best Practices

### ✅ Do's

1. **Use @Getter @Setter on config classes:**
   ```java
   @Getter
   @Setter
   public class MyConfig extends OkaeriConfig
   ```

2. **Use @Data for Serializable POJOs:**
   ```java
   @Data
   @NoArgsConstructor
   @AllArgsConstructor
   public class PlayerData implements Serializable
   ```

3. **Use @NonNull for required fields:**
   ```java
   public void setApiKey(@NonNull String apiKey)
   ```

4. **Keep Lombok dependency as `provided` or `compileOnly`:**
   ```xml
   <scope>provided</scope>
   ```

### ❌ Don'ts

1. **Don't use @Data on OkaeriConfig classes:**
   ```java
   // Wrong - @Data generates toString, equals, hashCode which may cause issues
   @Data
   public class MyConfig extends OkaeriConfig

   // Correct - Use @Getter @Setter only
   @Getter
   @Setter
   public class MyConfig extends OkaeriConfig
   ```

2. **Don't use @Builder on config classes:**
   ```java
   // Wrong - Configs should use default constructors
   @Builder
   public class MyConfig extends OkaeriConfig

   // Correct - No builder
   public class MyConfig extends OkaeriConfig
   ```

3. **Don't use @Value for mutable configs:**
   ```java
   // Wrong - @Value creates immutable class
   @Value
   public class MyConfig extends OkaeriConfig

   // Correct - Use @Getter @Setter for mutable configs
   @Getter
   @Setter
   public class MyConfig extends OkaeriConfig
   ```

## Lombok and Validation

Lombok works perfectly with validation annotations:

```java
import eu.okaeri.validator.annotation.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ValidatedConfig extends OkaeriConfig {

    @NotBlank
    @Size(min = 3, max = 50)
    private String serverName = "My Server";

    @Min(1)
    @Max(65535)
    private Integer port = 8080;

    @NotNull
    @Pattern("[a-zA-Z0-9_]+")
    private String username = "admin";
}
```

## Next Steps

- **[Getting Started](Getting-Started)** - Create your first config with Lombok
- **[Configuration Basics](Configuration-Basics)** - Understanding config structure
- **[Validation](Validation)** - Add validation to Lombok-powered configs

## See Also

- **[Project Lombok Documentation](https://projectlombok.org/features/all)** - Complete Lombok feature reference
- **[Subconfigs & Serialization](Subconfigs-and-Serialization)** - Using Lombok with nested configs
- **[Examples & Recipes](Examples-and-Recipes)** - Real-world Lombok config examples
