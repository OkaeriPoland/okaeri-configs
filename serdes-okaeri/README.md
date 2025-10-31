# Okaeri Configs | okaeri-commons

Serializers/Deserializers/Transformers for [okaeri-commons](https://github.com/OkaeriPoland/okaeri-commons) types.

```java
new SerdesOkaeri()
```

## Installation

### Maven

Add dependency to the `dependencies` section:

```xml
<dependency>
  <groupId>eu.okaeri</groupId>
  <artifactId>okaeri-configs-serdes-okaeri</artifactId>
  <version>6.0.0-beta.2</version>
</dependency>
```

### Gradle (Kotlin)

Add dependency to the `dependencies` section:

```kotlin
implementation("eu.okaeri:okaeri-configs-serdes-okaeri:6.0.0-beta.2")
```

## Supported types

### Serializers

| Class | Params | Format Control |
|-|-|-|
| eu.okaeri.commons.indexedset.IndexedSet | *(dynamic)* | `@IndexedSetSpec` |
| eu.okaeri.commons.range.IntRange | - | `@RangeSpec` |
| eu.okaeri.commons.range.LongRange | - | `@RangeSpec` |
| eu.okaeri.commons.range.ShortRange | - | `@RangeSpec` |
| eu.okaeri.commons.range.ByteRange | - | `@RangeSpec` |
| eu.okaeri.commons.range.FloatRange | - | `@RangeSpec` |
| eu.okaeri.commons.range.DoubleRange | - | `@RangeSpec` |

### Transformers

| Side | Side | Type |
|-|-|-|
| java.lang.String | eu.okaeri.commons.RomanNumeral | Two-side |

## Range Serialization

Range types support two serialization formats, controlled by the `@RangeSpec` annotation:

### INLINE Format (Compact)

```java
@RangeSpec(format = RangeFormat.INLINE)
private IntRange damageRange = IntRange.of(10, 20);
```

**YAML Output:**
```yaml
damageRange: "10-20"
```

### SECTION Format (Verbose - Default)

```java
@RangeSpec(format = RangeFormat.SECTION)
private IntRange healthRange = IntRange.of(50, 100);

// Or without annotation (SECTION is the default):
private IntRange healthRange = IntRange.of(50, 100);
```

**YAML Output:**
```yaml
healthRange:
  min: 50
  max: 100
```

### Mixed Formats

You can use different formats for different fields in the same config:

```java
public class GameConfig extends OkaeriConfig {

    @RangeSpec(format = RangeFormat.INLINE)
    private IntRange damageRange = IntRange.of(10, 20);

    @RangeSpec(format = RangeFormat.SECTION)
    private IntRange healthRange = IntRange.of(50, 100);

    // Default format (SECTION)
    private LongRange experienceRange = LongRange.of(100L, 5000L);
}
```

**Note:** Deserialization automatically detects the format, so existing configs using either format will continue to work.
