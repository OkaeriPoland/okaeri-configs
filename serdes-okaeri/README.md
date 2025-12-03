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
  <version>6.0.0-beta.15</version>
</dependency>
```

### Gradle (Kotlin)

Add dependency to the `dependencies` section:

```kotlin
implementation("eu.okaeri:okaeri-configs-serdes-okaeri:6.0.0-beta.15")
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

### Class-Level Annotations

You can apply `@RangeSpec` (and other format annotations) at the class level to set a default format for all compatible fields. Field-level annotations take precedence over class-level annotations.

```java
@RangeSpec(format = RangeFormat.INLINE)
public class CompactGameConfig extends OkaeriConfig {
    // Uses INLINE from class-level annotation
    private IntRange damageRange = IntRange.of(10, 20);
    private LongRange healthRange = LongRange.of(50, 100);

    // Field-level annotation overrides class-level
    @RangeSpec(format = RangeFormat.SECTION)
    private IntRange experienceRange = IntRange.of(100, 5000);
}
```

**YAML Output:**
```yaml
damageRange: "10-20"
healthRange: "50-100"
experienceRange:
  min: 100
  max: 5000
```

This works for all SerdesContextAttachment annotations including:
- `@RangeSpec` - Range serialization format
- `@IndexedSetSpec` - IndexedSet key field
- `@ItemStackSpec` - ItemStack serialization format (serdes-bukkit)
- `@DurationSpec` - Duration serialization format (serdes-commons)
