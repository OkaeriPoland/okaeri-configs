# Okaeri Configs - Test Implementation Plan

## Table of Contents
1. [Overview](#overview)
2. [Testing Philosophy](#testing-philosophy)
3. [Module Structure](#module-structure)
4. [Core Module Testing](#core-module-testing)
5. [Format Implementation Testing](#format-implementation-testing)
6. [Test Organization](#test-organization)
7. [CI/CD Setup](#cicd-setup)
8. [Questions & Decisions](#questions--decisions)

---

## Overview

This document outlines a comprehensive test implementation strategy for okaeri-configs, focusing on **feature-driven tests** rather than arbitrary coverage metrics. The goal is to enable safe collaborative development with robust CI safeguards.

### Scope
- **PRIMARY FOCUS**: Core library functionality (config lifecycle, serialization, type handling, annotations)
- **SECONDARY FOCUS**: Format implementation testing via comprehensive E2E MegaConfig
- **OUT OF SCOPE** (for now): Serdes extensions, Validators, Platform-specific types (ItemStack, etc.)

### Test Framework
- **JUnit 5 (Jupiter)** - Modern Java testing framework
- **AssertJ** - Fluent assertions for better readability
- **No Mocking** - Real objects and actual behavior testing
- **SnakeYAML** - Primary format for core tests

---

## Testing Philosophy

### Feature-Driven Testing
Each test should validate a **real use case** or **specific feature behavior**, not just increase line coverage. Tests should answer: "Does this feature work as documented?"

### Test Categories
1. **Unit Tests** - Individual components in isolation
2. **Integration Tests** - Components working together (e.g., OkaeriConfig + Configurer + SerdesRegistry)
3. **End-to-End Tests** - Complete workflows (create, save, load, update)
4. **Edge Case Tests** - Boundary conditions, error handling, unusual inputs

### Principles
- **No Mocks** - Test real behavior with actual implementations
- **Readable** - Tests should serve as documentation
- **Isolated** - Each test should be independent
- **Fast** - Unit tests should run quickly for rapid feedback
- **Comprehensive** - Cover all features, types, annotations, and edge cases

---

## Module Structure

```
okaeri-configs/
├── core/                    # Core library - includes InMemoryConfigurer tests
│   └── src/test/java/
│       └── eu/okaeri/configs/
│           └── configurer/
│               └── InMemoryConfigurerTest.java  # Basic in-memory configurer tests
│
├── core-test/               # PRIMARY TEST FOCUS - Core functionality tests
│   └── src/test/java/
│       └── eu/okaeri/configs/
│           ├── lifecycle/       # Config creation, save, load, update
│           ├── types/           # Type system tests
│           ├── annotations/     # Annotation behavior tests
│           ├── schema/          # Declaration system tests
│           ├── serdes/          # Serdes registry and standard serdes
│           ├── migration/       # Migration system tests
│           ├── manager/         # ConfigManager tests
│           └── integration/     # Full workflow tests
│
├── core-test-commons/       # SHARED TEST UTILITIES MODULE
│   └── src/main/java/
│       └── eu/okaeri/configs/test/
│           ├── TestUtils.java           # Common test utilities
│           ├── configs/                 # Feature-specific test configs
│           │   ├── PrimitivesTestConfig.java
│           │   ├── CollectionsTestConfig.java
│           │   ├── MapsTestConfig.java
│           │   ├── EnumsTestConfig.java
│           │   ├── NestedTestConfig.java
│           │   ├── SerializableTestConfig.java
│           │   └── AnnotationsTestConfig.java
│           └── MegaConfig.java          # Comprehensive E2E test config
│
├── yaml-snakeyaml/          # FORMAT TESTING
├── yaml-bukkit/             # FORMAT TESTING  
├── yaml-bungee/             # FORMAT TESTING
├── hjson/                   # FORMAT TESTING
├── json-gson/               # FORMAT TESTING
├── json-simple/             # FORMAT TESTING
├── hocon-lightbend/         # FORMAT TESTING
│
└── [other modules]          # OUT OF SCOPE for initial phase
```

---

## Core Module Testing

### 1. Config Lifecycle Tests (`lifecycle/`)

#### 1.1 Config Creation
**File: `ConfigCreationTest.java`**

Test scenarios:
- ✅ Create config using `ConfigManager.create(Class)`
- ✅ Create config using `ConfigManager.create(Class, initializer)`
- ✅ Create config without configurer (should work for in-memory only)
- ✅ Create config with configurer
- ✅ Create config with configurer + serdes packs
- ✅ Verify declaration is auto-generated in constructor
- ✅ Test `ConfigManager.createUnsafe()` for internal use
- ✅ Test `ConfigManager.initialize()` method
- ✅ Test fluent API chaining (withConfigurer, withBindFile, etc.)
- ❌ Create config with null class (should throw)
- ❌ Create config with invalid initializer (should throw)

#### 1.2 Save Operations
**File: `ConfigSaveTest.java`**

Test scenarios:
- ✅ Save to file (File)
- ✅ Save to file (Path)
- ✅ Save to file (String pathname)
- ✅ Save to OutputStream
- ✅ Save to String (`saveToString()`)
- ✅ `saveDefaults()` - creates file if not exists
- ✅ `saveDefaults()` - skips if file exists
- ✅ Save creates parent directories
- ✅ Save overwrites existing file
- ✅ Save with orphan removal enabled
- ✅ Save with orphan removal disabled
- ✅ Verify field values are written correctly
- ❌ Save without configurer (should throw)
- ❌ Save without bind file when using `save()` (should throw)
- ❌ Save with invalid field value (validation failure)

#### 1.3 Load Operations
**File: `ConfigLoadTest.java`**

Test scenarios:
- ✅ Load from file (File)
- ✅ Load from file (Path)
- ✅ Load from file (String pathname)
- ✅ Load from InputStream
- ✅ Load from String
- ✅ Load from Map<String, Object>
- ✅ Load from another OkaeriConfig
- ✅ Load with update (`load(true)`)
- ✅ Load without update (`load(false)`)
- ✅ Load updates field values correctly
- ✅ Load handles missing fields (keeps defaults)
- ✅ Load handles extra fields (orphans)
- ❌ Load without configurer (should throw)
- ❌ Load non-existent file (should throw)
- ❌ Load malformed data (should throw)

#### 1.4 Update Operations
**File: `ConfigUpdateTest.java`**

Test scenarios:
- ✅ `update()` synchronizes configurer data to fields
- ✅ `update()` respects @Variable annotation
- ✅ `update()` sets starting values
- ✅ `updateDeclaration()` regenerates schema
- ✅ Update after load reflects new values
- ✅ Update with validation
- ❌ Update without declaration (should throw)

#### 1.5 Get/Set Operations
**File: `ConfigGetSetTest.java`**

Test scenarios:
- ✅ `set(key, value)` updates field
- ✅ `set(key, value)` updates configurer
- ✅ `set(key, value)` with type transformation
- ✅ `get(key)` returns field value
- ✅ `get(key)` for undeclared key returns configurer value
- ✅ `get(key, Class)` with type conversion
- ✅ `get(key, GenericsDeclaration)` with generics
- ❌ `set()` without configurer (should throw)
- ❌ `get()` without configurer (should throw)

#### 1.6 Map Conversion
**File: `ConfigMapConversionTest.java`**

Test scenarios:
- ✅ `asMap(configurer, conservative=false)` - non-conservative
- ✅ `asMap(configurer, conservative=true)` - preserves primitives
- ✅ Map contains all declared fields
- ✅ Map contains orphaned fields (if present)
- ✅ Map values are properly simplified
- ✅ Nested configs are converted to maps

### 2. Type System Tests (`types/`)

#### 2.1 Primitive Types
**File: `PrimitiveTypesTest.java`**

Test each primitive:
- ✅ `boolean` / `Boolean`
- ✅ `byte` / `Byte`
- ✅ `char` / `Character`
- ✅ `double` / `Double`
- ✅ `float` / `Float`
- ✅ `int` / `Integer`
- ✅ `long` / `Long`
- ✅ `short` / `Short`

For each type:
- Save and load cycle maintains value
- Type conversion works (e.g., String "123" → int 123)
- Primitive ↔ Wrapper conversion
- Default values work
- Edge cases (min/max values, zero, negative)

#### 2.2 Basic Types
**File: `BasicTypesTest.java`**

Test scenarios:
- ✅ `String` - empty, null, unicode, special characters
- ✅ `BigInteger` - very large numbers
- ✅ `BigDecimal` - precise decimals
- ✅ Object type (dynamic typing)

#### 2.3 Collection Types
**File: `CollectionTypesTest.java`**

Test scenarios:
- ✅ `List<T>` - ArrayList, various element types
- ✅ `Set<T>` - LinkedHashSet, order preservation
- ✅ `List<String>`, `List<Integer>`, `List<CustomObject>`
- ✅ `Set<String>`, `Set<Integer>`, `Set<Enum>`
- ✅ Empty collections
- ✅ Null elements handling
- ✅ Nested collections (List<List<String>>)
- ✅ Custom collection implementations with @TargetType

#### 2.4 Map Types
**File: `MapTypesTest.java`**

Test scenarios:
- ✅ `Map<String, String>` - simple maps
- ✅ `Map<String, Integer>` - mixed types
- ✅ `Map<Integer, String>` - non-string keys
- ✅ `Map<String, List<String>>` - complex values
- ✅ `Map<String, Map<String, Integer>>` - nested maps
- ✅ `Map<Enum, CustomObject>` - enum keys
- ✅ Empty maps
- ✅ Null values handling
- ✅ Custom map implementations with @TargetType

#### 2.5 Enum Types
**File: `EnumTypesTest.java`**

Test scenarios:
- ✅ Simple enum serialization/deserialization
- ✅ Enum.valueOf() exact match
- ✅ Case-insensitive fallback
- ✅ List<Enum>
- ✅ Set<Enum>
- ✅ Map with enum keys
- ✅ Map with enum values
- ❌ Invalid enum value (should throw with helpful message)

#### 2.6 Subconfig Types
**File: `SubconfigTypesTest.java`**

Test scenarios:
- ✅ Nested OkaeriConfig as field
- ✅ Multiple levels of nesting
- ✅ Subconfig with its own annotations
- ✅ Subconfig serialization to map
- ✅ Subconfig deserialization from map
- ✅ Subconfig with custom types
- ✅ List<OkaeriConfig>
- ✅ Map<String, OkaeriConfig>

#### 2.7 Serializable Types
**File: `SerializableTypesTest.java`**

Test scenarios:
- ✅ Simple Serializable class
- ✅ Serializable with various field types
- ✅ Nested Serializable objects
- ✅ List<Serializable>
- ✅ Map<String, Serializable>
- ✅ Serializable to/from map conversion

#### 2.8 Type Transformations
**File: `TypeTransformationsTest.java`**

Test scenarios:
- ✅ String → Integer conversion
- ✅ String → Boolean conversion
- ✅ Integer → String conversion
- ✅ Integer → Long conversion (primitive cross-conversion)
- ✅ String → Enum conversion
- ✅ Enum → String conversion
- ✅ Two-step transformations (A → B → C)
- ✅ Custom transformers registered in registry
- ✅ Primitive unboxing/boxing
- ❌ Incompatible type conversion (should throw)

### 3. Annotation Tests (`annotations/`)

#### 3.1 @Header/@Headers
**File: `HeaderAnnotationTest.java`**

Test scenarios:
- ✅ Single @Header with one line
- ✅ Single @Header with multiple lines
- ✅ @Headers with multiple @Header annotations
- ✅ Header is included in declaration
- ✅ Header is preserved in save/load cycle
- ✅ No header when annotation absent

#### 3.2 @Comment/@Comments
**File: `CommentAnnotationTest.java`**

Test scenarios:
- ✅ Single @Comment with one line
- ✅ Single @Comment with multiple lines
- ✅ @Comments with multiple @Comment annotations
- ✅ Comment is included in field declaration
- ✅ Comments are preserved in save/load cycle
- ✅ Repeating @Comment on same field
- ✅ No comment when annotation absent

#### 3.3 @CustomKey
**File: `CustomKeyAnnotationTest.java`**

Test scenarios:
- ✅ Field with custom key name
- ✅ Custom key is used in serialization
- ✅ Load using custom key
- ✅ Get/set using custom key
- ✅ Empty value uses field name
- ✅ Custom key in nested config

#### 3.4 @Variable
**File: `VariableAnnotationTest.java`**

Test scenarios:
- ✅ Load from system property
- ✅ Load from environment variable
- ✅ System property takes precedence over env var
- ✅ Fallback to config value when variable not set
- ✅ Variable value is hidden in saves (variableHide)
- ✅ Variable with type conversion (String → Integer)
- ✅ Variable mode (RUNTIME vs other modes if implemented)
- ✅ Multiple variables in same config
- ❌ Variable validation failure

#### 3.5 @Exclude
**File: `ExcludeAnnotationTest.java`**

Test scenarios:
- ✅ Excluded field is not in declaration
- ✅ Excluded field is not saved
- ✅ Excluded field is not loaded
- ✅ Excluded field can still be accessed in code
- ✅ Multiple excluded fields

#### 3.6 @Names (NameStrategy)
**File: `NamesAnnotationTest.java`**

Test scenarios:
- ✅ HYPHEN_CASE strategy (camelCase → camel-case)
- ✅ SNAKE_CASE strategy (camelCase → camel_case)
- ✅ Other strategies (check NameStrategy enum)
- ✅ TO_UPPER_CASE modifier
- ✅ TO_LOWER_CASE modifier
- ✅ Combined strategy + modifier
- ✅ Names annotation on nested class
- ✅ Names annotation inheritance from enclosing class
- ✅ @CustomKey overrides @Names strategy

#### 3.7 @TargetType
**File: `TargetTypeAnnotationTest.java`**

Test scenarios:
- ✅ Collection with concrete implementation (List → ArrayList)
- ✅ Map with concrete implementation (Map → LinkedHashMap)
- ✅ Custom collection implementation
- ✅ TargetType for field-level type resolution
- ✅ TargetType doesn't affect nested generics

#### 3.8 @Include/@Includes
**File: `IncludeAnnotationTest.java`**

Test scenarios:
- ✅ Include fields from another class
- ✅ Multiple @Include annotations
- ✅ Include doesn't override existing fields
- ✅ Include with same field names (first wins)
- ✅ Declaration contains included fields
- ✅ Included fields are saved/loaded

### 4. Schema System Tests (`schema/`)

#### 4.1 ConfigDeclaration
**File: `ConfigDeclarationTest.java`**

Test scenarios:
- ✅ Declaration caching works
- ✅ Declaration contains all non-excluded fields
- ✅ Declaration preserves field order
- ✅ Header is captured correctly
- ✅ Name strategy is captured correctly
- ✅ `getField(key)` returns correct field
- ✅ `getFields()` returns all fields
- ✅ `getGenericsOrNull()` returns field type
- ✅ Declaration for class without instance
- ✅ Declaration for class with instance
- ✅ Declaration includes @Include fields

#### 4.2 FieldDeclaration
**File: `FieldDeclarationTest.java`**

Test scenarios:
- ✅ Field declaration caching works
- ✅ Name resolution (field name / @CustomKey / @Names)
- ✅ Comment reading (@Comment / @Comments)
- ✅ Type information (GenericsDeclaration)
- ✅ @Variable annotation capture
- ✅ `getValue()` returns current field value
- ✅ `updateValue()` sets field value
- ✅ Starting value is captured
- ✅ variableHide flag works
- ✅ Annotation retrieval (`getAnnotation()`)
- ✅ Static annotation reading for serdes
- ✅ Final field warning is logged
- ❌ Access private field fails without setAccessible

#### 4.3 GenericsDeclaration
**File: `GenericsDeclarationTest.java`**

Test scenarios:
- ✅ Create from Class
- ✅ Create from object instance
- ✅ Create from Field
- ✅ Create from Type
- ✅ Capture generic parameters (List<String>)
- ✅ Capture nested generics (Map<String, List<Integer>>)
- ✅ `getType()` returns raw type
- ✅ `getSubtypeAtOrNull()` returns generic parameter
- ✅ `isPrimitive()` detection
- ✅ `isPrimitiveWrapper()` detection
- ✅ `isEnum()` detection
- ✅ `wrap()` primitive to wrapper
- ✅ `unwrapValue()` wrapper to primitive
- ✅ `doBoxTypesMatch()` for compatible types

### 5. Serdes System Tests (`serdes/`)

#### 5.1 SerdesRegistry
**File: `SerdesRegistryTest.java`**

Test scenarios:
- ✅ Register ObjectSerializer
- ✅ Register ObjectTransformer
- ✅ Register BidirectionalTransformer
- ✅ Register OkaeriSerdesPack
- ✅ `getSerializer()` retrieves correct serializer
- ✅ `getTransformer()` retrieves correct transformer
- ✅ `getTransformersFrom()` lists all transformers
- ✅ `getTransformersTo()` lists all transformers
- ✅ `canTransform()` checks transformation capability
- ✅ StandardSerdes is auto-registered
- ✅ Multiple serdes packs registration
- ✅ Exclusive serializer registration

#### 5.2 SerializationData
**File: `SerializationDataTest.java`**

Test scenarios:
- ✅ `setValue()` for simple value
- ✅ `add()` for map-style serialization
- ✅ `addRaw()` for raw values
- ✅ `addCollection()` for collection fields
- ✅ `addArray()` for array fields
- ✅ `addAsMap()` for map fields
- ✅ `addFormatted()` for formatted strings
- ✅ `asMap()` returns serialization map
- ✅ `clear()` resets data
- ✅ Magic VALUE key behavior

#### 5.3 DeserializationData
**File: `DeserializationDataTest.java`**

Test scenarios:
- ✅ `getValue()` for simple value
- ✅ `get()` from map
- ✅ `getRaw()` without transformation
- ✅ `getDirect()` with GenericsDeclaration
- ✅ `getAsList()` converts to list
- ✅ `getAsSet()` converts to set
- ✅ `getAsMap()` converts to map
- ✅ `getAsCollection()` for custom collections
- ✅ `containsKey()` checks presence
- ✅ `isValue()` for magic VALUE key
- ✅ `asMap()` returns full map

#### 5.4 SerdesContext
**File: `SerdesContextTest.java`**

Test scenarios:
- ✅ Create from Configurer only
- ✅ Create from Configurer + Field
- ✅ Create from Configurer + Field + Attachments
- ✅ `getConfigurer()` returns configurer
- ✅ `getField()` returns field declaration
- ✅ `getConfigAnnotation()` retrieves class annotations
- ✅ `getFieldAnnotation()` retrieves field annotations
- ✅ `getAttachment()` retrieves custom attachment
- ✅ `getAttachment()` with default value
- ✅ Builder pattern for context creation

#### 5.5 StandardSerdes
**File: `StandardSerdesTest.java`**

Test scenarios for each standard serializer/transformer:
- ✅ All primitive types
- ✅ All primitive wrappers
- ✅ String
- ✅ BigInteger
- ✅ BigDecimal
- ✅ Collections (List, Set)
- ✅ Maps
- ✅ Enums
- ✅ Each serializer's serialize/deserialize cycle
- ✅ Each transformer's transform cycle

### 6. Migration System Tests (`migration/`)

#### 6.1 ConfigMigration
**File: `ConfigMigrationTest.java`**

Test scenarios:
- ✅ Simple migration implementation
- ✅ Migration returns true when performed
- ✅ Migration returns false when skipped
- ✅ Migration accesses config
- ✅ Migration accesses RawConfigView
- ✅ Multiple migrations in sequence
- ✅ Named migration logging

#### 6.2 ConfigMigrationDsl
**File: `ConfigMigrationDslTest.java`**

Test scenarios:
- ✅ `copy(from, to)` - copy key
- ✅ `delete(key)` - delete key
- ✅ `move(from, to)` - move key
- ✅ `move(from, to, updateFn)` - move with transformation
- ✅ `supply(key, supplier)` - supply value
- ✅ `update(key, function)` - update value
- ✅ `when(condition, true, false)` - conditional
- ✅ `when(condition, true)` - conditional without else
- ✅ `exists(key)` - existence check
- ✅ `multi(migrations...)` - multiple migrations
- ✅ `any(migrations...)` - any succeeds
- ✅ `all(migrations...)` - all succeed
- ✅ `noop(result)` - no-op migration
- ✅ `not(migration)` - negation
- ✅ `match(key, predicate)` - value matching

#### 6.3 RawConfigView
**File: `RawConfigViewTest.java`**

Test scenarios:
- ✅ Get raw value from config
- ✅ Set raw value in config
- ✅ Check key existence
- ✅ Remove key
- ✅ Access nested values
- ✅ Type-safe value retrieval

### 7. ConfigManager Tests (`manager/`)

**File: `ConfigManagerTest.java`**

Test scenarios:
- ✅ `create(Class)` creates instance
- ✅ `create(Class, initializer)` with initialization
- ✅ `createUnsafe(Class)` for internal use
- ✅ `transformCopy(config, targetClass)` copies to different type
- ✅ `deepCopy(config, newConfigurer, targetClass)` with new configurer
- ✅ `initialize(config)` post-processes config
- ❌ Create with invalid class (should throw)
- ❌ Create with null initializer (should throw)

### 8. Integration Tests (`integration/`)

#### 8.1 Complete Workflows
**File: `CompleteWorkflowTest.java`**

Test scenarios:
- ✅ Create → Save → Load → Verify
- ✅ Create → Load → Modify → Save → Load → Verify
- ✅ Create with defaults → SaveDefaults → Load
- ✅ Load → Migrate → Save
- ✅ Multiple configs with same backing file (safety)
- ✅ Config with all type combinations
- ✅ Config with all annotations
- ✅ Nested config hierarchy

#### 8.2 Orphan Handling
**File: `OrphanHandlingTest.java`**

Test scenarios:
- ✅ Load config with extra fields (orphans)
- ✅ Save with removeOrphans=false (keeps orphans)
- ✅ Save with removeOrphans=true (removes orphans)
- ✅ Orphan detection and reporting
- ✅ Orphan preservation across multiple saves

#### 8.3 Cross-Format Compatibility
**File: `CrossFormatTest.java`**

Test scenarios:
- ✅ Save as YAML, load as JSON (via map)
- ✅ Save as JSON, load as HJSON (via map)
- ✅ transformCopy between different configurers
- ✅ deepCopy with format change
- ✅ Data integrity across formats

#### 8.4 Edge Cases
**File: `EdgeCasesTest.java`**

Test scenarios:
- ✅ Empty config (no fields)
- ✅ Config with only excluded fields
- ✅ Config with only transient fields
- ✅ Circular reference handling (if supported)
- ✅ Very deep nesting
- ✅ Very large collections
- ✅ Unicode in keys and values
- ✅ Special characters in strings
- ✅ Null handling everywhere
- ❌ Invalid UTF-8 (should handle gracefully)

---

## Format Implementation Testing

### Testing Strategy

For format implementations (YAML, HJSON, JSON, HOCON), we test the **Configurer implementation** - how it manages the internal data structure and handles read/write operations.

### Understanding Configurers

After examining the source code, Configurers work as follows:

1. **Internal Storage**: Each Configurer maintains an internal data structure:
   - `YamlSnakeYamlConfigurer`, `JsonGsonConfigurer`, `JsonSimpleConfigurer`: `Map<String, Object>`
   - `YamlBukkitConfigurer`: Bukkit's `YamlConfiguration` (wraps Map)
   - `HjsonConfigurer`: `JsonObject`

2. **Key Operations**:
   - `setValue()`: Simplifies value → stores in internal structure
   - `getValue()`: Retrieves value from internal structure
   - `load()`: Reads InputStream → populates internal structure
   - `write()`: Writes internal structure → OutputStream

3. **Comment/Header Handling**:
   - **YAML formats**: Use `ConfigPostprocessor` with `YamlSectionWalker` to inject comments post-render
   - **HJSON**: Uses native comment support via `JsonValue.setFullComment()`
   - **JSON formats**: No comment support (format limitation)

4. **What We Test**:
   - ✅ Load from format → internal map population
   - ✅ Set/get values in internal structure
   - ✅ Write internal map → format output
   - ✅ Round-trip consistency (save → load → verify)
   - ✅ Comment preservation (where supported)
   - ✅ Header preservation (where supported)
   - ✅ Key ordering preservation (LinkedHashMap)
   - ❌ Format-specific parsing quirks (that's the underlying library's job)

### Primary Test Format: SnakeYAML

**Decision**: Use **YAML with SnakeYAML** as the primary format for core tests because:
1. **Industry Standard** - Widely adopted and understood
2. **Feature Rich** - Full support for all okaeri-configs features
3. **Comment Support** - Can test comment preservation
4. **Readable** - Clear, human-friendly syntax
5. **Battle-tested** - Mature, stable library

### Format-Specific Tests

For each format implementation, tests are organized into **three categories**:

#### 1. Configurer-Specific Features Tests
Test format-specific capabilities and behavior:
- Internal data structure operations (setValue/getValue)
- Load from InputStream → populate internal structure
- Write internal structure → OutputStream
- Comment handling (where supported)
- Header handling (where supported)
- Format-specific quirks and limitations

#### 2. Edge Cases Tests
Test boundary conditions and error handling:
- Empty/null config handling
- Malformed input handling
- Very large values
- Special characters and escaping
- Format-specific edge cases

#### 3. End-to-End MegaConfig Tests
Comprehensive integration test using the shared **MegaConfig** from `core-test-commons`:
- **Save MegaConfig** → Format file
- **Load** Format file → New config instance
- **Assert** all fields match (deep equality)
- **Comment Preservation** (where supported)
- **Header Preservation** (where supported)
- **Key Ordering** preservation

**MegaConfig** includes:
- All primitive types and wrappers
- All collection types (List, Set)
- All map types (various key/value combinations)
- Enums
- Nested subconfigs (multiple levels)
- Serializable objects
- All annotations (@Header, @Comment, @CustomKey, @Variable, @Exclude, @Names, @TargetType, @Include)
- Edge cases (empty collections, null values, unicode including Russian/Polish, special characters)

#### YAML (SnakeYAML)
**File: `yaml-snakeyaml/src/test/java/.../YamlSnakeYamlConfigurerTest.java`**

Test scenarios:
- ✅ MegaConfig E2E test (save/load/verify)
- ✅ Load from InputStream populates map correctly
- ✅ setValue/getValue operations
- ✅ Comment preservation via ConfigPostprocessor
- ✅ Header preservation
- ✅ Key ordering (LinkedHashMap preservation)
- ✅ Empty/null config handling
- ✅ Malformed YAML handling

#### HJSON
**File: `hjson/src/test/java/.../HjsonConfigurerTest.java`**

Test scenarios:
- ✅ MegaConfig E2E test (save/load/verify)
- ✅ Load from InputStream populates JsonObject correctly
- ✅ setValue/getValue operations (with JsonValue conversion)
- ✅ Comment preservation via native HJSON comments
- ✅ Header preservation
- ✅ Quoteless syntax handling
- ✅ Multiline string support
- ✅ char type conversion (forced to string)

#### JSON (GSON)
**File: `json-gson/src/test/java/.../JsonGsonConfigurerTest.java`**

Test scenarios:
- ✅ MegaConfig E2E test (save/load/verify)
- ✅ Load from InputStream populates map correctly
- ✅ setValue/getValue operations
- ✅ Pretty printing enabled
- ✅ Number type preservation
- ✅ No comment support (verify ignored)
- ✅ No header support (verify ignored)

#### JSON (Simple)
**File: `json-simple/src/test/java/.../JsonSimpleConfigurerTest.java`**

Test scenarios:
- ✅ MegaConfig E2E test (save/load/verify)
- ✅ Load from InputStream with ContainerFactory
- ✅ setValue/getValue operations
- ✅ char type conversion (forced to string)
- ✅ Limitations: no pretty print, no comments
- ✅ LinkedHashMap ordering preserved

#### HOCON (Lightbend)
**File: `hocon-lightbend/src/test/java/.../HoconConfigurerTest.java`**

Test scenarios:
- ✅ MegaConfig E2E test (save/load/verify)
- ✅ Load from InputStream populates map correctly
- ✅ setValue/getValue operations
- ✅ HOCON-specific: substitutions, includes (if implemented)
- ✅ Comment preservation (if implemented)
- ✅ Known limitations documented

#### Bukkit YAML
**File: `yaml-bukkit/src/test/java/.../YamlBukkitConfigurerTest.java`**

Test scenarios:
- ✅ MegaConfig E2E test (without Bukkit-specific types)
- ✅ MemorySection handling in simplify/resolveType
- ✅ setValue/getValue operations
- ✅ Comment preservation via ConfigPostprocessor
- ✅ YamlConfiguration internal storage
- **Note**: No ItemStack, Location etc. - just test the format itself

#### Bungee YAML
**File: `yaml-bungee/src/test/java/.../YamlBungeeConfigurerTest.java`**

Test scenarios:
- ✅ MegaConfig E2E test (without Bungee-specific types)
- ✅ setValue/getValue operations
- ✅ Comment preservation via ConfigPostprocessor
- **Note**: Just test the format itself, not Bungee-specific types

---

## Shared Test Commons Module

Create a new module `core-test-commons` with test utilities and configs shared across all format implementations.

### Module: core-test-commons

**Purpose**: Centralize test utilities, simple test configs, and the comprehensive MegaConfig for E2E testing.

**File: `core-test-commons/pom.xml`**
```xml
<artifactId>okaeri-configs-test-commons</artifactId>
<name>okaeri-configs (test-commons)</name>
<description>Shared test utilities and configs for okaeri-configs</description>

<dependencies>
    <dependency>
        <groupId>eu.okaeri</groupId>
        <artifactId>okaeri-configs-core</artifactId>
        <version>${project.version}</version>
    </dependency>
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>compile</scope> <!-- not test, this module IS for tests -->
    </dependency>
    <dependency>
        <groupId>org.assertj</groupId>
        <artifactId>assertj-core</artifactId>
        <scope>compile</scope>
    </dependency>
</dependencies>
```

**File: `core-test-commons/src/main/java/eu/okaeri/configs/test/TestUtils.java`**
```java
package eu.okaeri.configs.test;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Common test utilities for okaeri-configs testing.
 */
public class TestUtils {
    
    /**
     * Creates a temporary directory for test files.
     * Automatically deleted after JVM exit.
     */
    public static Path createTempTestDir() throws IOException {
        Path tempDir = Files.createTempDirectory("okaeri-test-");
        tempDir.toFile().deleteOnExit();
        return tempDir;
    }
    
    /**
     * Creates a temporary file with given content.
     */
    public static File createTempFile(String content, String suffix) throws IOException {
        File tempFile = File.createTempFile("okaeri-test-", suffix);
        tempFile.deleteOnExit();
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write(content);
        }
        return tempFile;
    }
    
    /**
     * Loads a test resource from classpath.
     */
    public static InputStream getTestResource(String path) {
        return TestUtils.class.getClassLoader().getResourceAsStream(path);
    }
    
    /**
     * Deep equality assertion for OkaeriConfig instances.
     * Compares all declared fields recursively.
     */
    public static void assertConfigEquals(OkaeriConfig expected, OkaeriConfig actual) {
        // Implementation: compare field by field using reflection
        // Can use assertj's recursive comparison
        throw new UnsupportedOperationException("TODO: implement deep config comparison");
    }
    
    /**
     * Asserts that two configs produce the same map representation.
     */
    public static void assertConfigMapEquals(OkaeriConfig expected, OkaeriConfig actual, Configurer configurer) {
        Map<String, Object> expectedMap = expected.asMap(configurer, true);
        Map<String, Object> actualMap = actual.asMap(configurer, true);
        assertThat(expectedMap).isEqualTo(actualMap);
    }
    
    /**
     * Creates a config from inline YAML string (for simple tests).
     */
    public static <T extends OkaeriConfig> T fromYaml(Class<T> clazz, String yaml) throws Exception {
        T config = ConfigManager.create(clazz);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.load(yaml);
        return config;
    }
}
```

**File: `core-test-commons/src/main/java/eu/okaeri/configs/test/configs/PrimitivesTestConfig.java`**
```java
package eu.okaeri.configs.test.configs;

import eu.okaeri.configs.OkaeriConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Test config for primitive types and their wrappers.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class PrimitivesTestConfig extends OkaeriConfig {
    
    // Primitives
    private boolean boolValue = true;
    private byte byteValue = 127;
    private char charValue = 'A';
    private double doubleValue = 3.14;
    private float floatValue = 2.71f;
    private int intValue = 42;
    private long longValue = 9999999999L;
    private short shortValue = 999;
    
    // Wrappers
    private Boolean boolWrapper = false;
    private Byte byteWrapper = 100;
    private Character charWrapper = 'Z';
    private Double doubleWrapper = 2.718;
    private Float floatWrapper = 1.414f;
    private Integer intWrapper = 123;
    private Long longWrapper = 987654321L;
    private Short shortWrapper = 555;
}
```

**File: `core-test-commons/src/main/java/eu/okaeri/configs/test/configs/CollectionsTestConfig.java`**
```java
package eu.okaeri.configs.test.configs;

import eu.okaeri.configs.OkaeriConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.*;

/**
 * Test config for collection types (List, Set).
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class CollectionsTestConfig extends OkaeriConfig {
    
    private List<String> stringList = Arrays.asList("alpha", "beta", "gamma");
    private List<Integer> intList = Arrays.asList(1, 2, 3, 5, 8);
    private Set<String> stringSet = new LinkedHashSet<>(Arrays.asList("one", "two", "three"));
    private Set<Integer> intSet = new LinkedHashSet<>(Arrays.asList(10, 20, 30));
    
    // Empty collections
    private List<String> emptyList = new ArrayList<>();
    private Set<String> emptySet = new LinkedHashSet<>();
    
    // Nested collections
    private List<List<String>> nestedList = Arrays.asList(
        Arrays.asList("a", "b"),
        Arrays.asList("c", "d")
    );
}
```

**File: `core-test-commons/src/main/java/eu/okaeri/configs/test/configs/MapsTestConfig.java`**
```java
package eu.okaeri.configs.test.configs;

import eu.okaeri.configs.OkaeriConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.*;

/**
 * Test config for map types with various key/value combinations.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class MapsTestConfig extends OkaeriConfig {
    
    private Map<String, String> simpleMap = new LinkedHashMap<String, String>() {{
        put("key1", "value1");
        put("key2", "value2");
    }};
    
    private Map<Integer, String> intKeyMap = new LinkedHashMap<Integer, String>() {{
        put(1, "one");
        put(2, "two");
    }};
    
    private Map<String, Integer> intValueMap = new LinkedHashMap<String, Integer>() {{
        put("a", 100);
        put("b", 200);
    }};
    
    private Map<String, List<String>> complexValueMap = new LinkedHashMap<String, List<String>>() {{
        put("group1", Arrays.asList("item1", "item2"));
        put("group2", Arrays.asList("item3", "item4"));
    }};
    
    private Map<String, Map<String, Integer>> nestedMap = new LinkedHashMap<String, Map<String, Integer>>() {{
        put("outer", new LinkedHashMap<String, Integer>() {{
            put("inner1", 1);
            put("inner2", 2);
        }});
    }};
    
    private Map<String, String> emptyMap = new LinkedHashMap<>();
}
```

**File: `core-test-commons/src/main/java/eu/okaeri/configs/test/configs/EnumsTestConfig.java`**
```java
package eu.okaeri.configs.test.configs;

import eu.okaeri.configs.OkaeriConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.*;

/**
 * Test config for enum types.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class EnumsTestConfig extends OkaeriConfig {
    
    public enum TestEnum {
        FIRST, SECOND, THIRD
    }
    
    private TestEnum singleEnum = TestEnum.SECOND;
    private List<TestEnum> enumList = Arrays.asList(TestEnum.FIRST, TestEnum.THIRD);
    private Set<TestEnum> enumSet = new LinkedHashSet<>(Arrays.asList(TestEnum.FIRST, TestEnum.SECOND));
    
    private Map<TestEnum, String> enumKeyMap = new LinkedHashMap<TestEnum, String>() {{
        put(TestEnum.FIRST, "first value");
        put(TestEnum.SECOND, "second value");
    }};
    
    private Map<String, TestEnum> enumValueMap = new LinkedHashMap<String, TestEnum>() {{
        put("a", TestEnum.FIRST);
        put("b", TestEnum.THIRD);
    }};
}
```

**File: `core-test-commons/src/main/java/eu/okaeri/configs/test/configs/NestedTestConfig.java`**
```java
package eu.okaeri.configs.test.configs;

import eu.okaeri.configs.OkaeriConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.*;

/**
 * Test config for nested subconfigs.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class NestedTestConfig extends OkaeriConfig {
    
    private SubConfig singleNested = new SubConfig("default", 42);
    private List<SubConfig> nestedList = Arrays.asList(
        new SubConfig("first", 10),
        new SubConfig("second", 20)
    );
    private Map<String, SubConfig> nestedMap = new LinkedHashMap<String, SubConfig>() {{
        put("config1", new SubConfig("map1", 100));
        put("config2", new SubConfig("map2", 200));
    }};
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = false)
    public static class SubConfig extends OkaeriConfig {
        private String name;
        private int value;
    }
}
```

**File: `core-test-commons/src/main/java/eu/okaeri/configs/test/configs/SerializableTestConfig.java`**
```java
package eu.okaeri.configs.test.configs;

import eu.okaeri.configs.OkaeriConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.*;

/**
 * Test config for Serializable custom objects.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class SerializableTestConfig extends OkaeriConfig {
    
    private CustomSerializable singleObject = new CustomSerializable("test", 999);
    private List<CustomSerializable> objectList = Arrays.asList(
        new CustomSerializable("item1", 1),
        new CustomSerializable("item2", 2)
    );
    private Map<String, CustomSerializable> objectMap = new LinkedHashMap<String, CustomSerializable>() {{
        put("obj1", new CustomSerializable("first", 10));
        put("obj2", new CustomSerializable("second", 20));
    }};
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomSerializable implements Serializable {
        private static final long serialVersionUID = 1L;
        private String name;
        private int id;
    }
}
```

**File: `core-test-commons/src/main/java/eu/okaeri/configs/test/configs/AnnotationsTestConfig.java`**
```java
package eu.okaeri.configs.test.configs;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Test config for various annotations.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Header("Test Header Line 1")
@Header("Test Header Line 2")
public class AnnotationsTestConfig extends OkaeriConfig {
    
    @Comment("This is a simple comment")
    private String commentedField = "value";
    
    @Comment({"Multi-line comment", "Line 2"})
    private String multiCommentField = "value2";
    
    @CustomKey("custom-key-name")
    private String customKeyField = "custom value";
    
    @Variable("TEST_VAR")
    private String variableField = "default";
    
    @Exclude
    private String excludedField = "should not serialize";
    
    private String normalField = "normal";
}
```

**File: `core-test-commons/src/main/java/eu/okaeri/configs/test/MegaConfig.java`**
```java
package eu.okaeri.configs.test;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/**
 * Comprehensive config that exercises ALL okaeri-configs features.
 * Used for E2E format testing.
 * 
 * This config is saved to a file and used by all format implementations
 * to ensure consistent behavior across formats.
 */
@Header("===========================================")
@Header("  Okaeri Configs - Mega Test Config")
@Header("  Tests ALL features comprehensively")
@Header("===========================================")
@Names(strategy = NameStrategy.IDENTITY, modifier = NameModifier.NONE)
public class MegaConfig extends OkaeriConfig {
    
    // === PRIMITIVES ===
    @Comment("Boolean primitive and wrapper")
    private boolean primBool = true;
    private Boolean wrapBool = false;
    
    @Comment("Numeric primitives")
    private byte primByte = 127;
    private short primShort = 32000;
    private int primInt = 2147483647;
    private long primLong = 9223372036854775807L;
    private float primFloat = 3.14159f;
    private double primDouble = 2.718281828;
    private char primChar = 'Ω';
    
    // === WRAPPERS ===
    @Comment("Wrapper types")
    private Byte wrapByte = 100;
    private Short wrapShort = 30000;
    private Integer wrapInt = 123456;
    private Long wrapLong = 987654321L;
    private Float wrapFloat = 1.414f;
    private Double wrapDouble = 1.732;
    private Character wrapChar = '€';
    
    // === STRINGS ===
    @Comment({"String tests", "including unicode and special chars"})
    private String simpleString = "Hello, World!";
    private String unicodeJapanese = "こんにちは世界 🌍";
    private String unicodeRussian = "Привет мир! Тестирование кириллицы";
    private String unicodePolish = "Część świecie! Łódź, Gdańsk, Krąków, źdźbło";
    private String specialChars = "!@#$%^&*()_+-=[]{}|;':\\\"<>?,./";
    private String emptyString = "";
    
    // === BIG NUMBERS ===
    @Comment("Math types for precision")
    private BigInteger bigInt = new BigInteger("999999999999999999999999999999");
    private BigDecimal bigDec = new BigDecimal("123.456789012345678901234567890");
    
    // === COLLECTIONS ===
    @Comment("List of strings")
    private List<String> stringList = Arrays.asList("alpha", "beta", "gamma");
    
    @Comment("List of integers")
    private List<Integer> intList = Arrays.asList(1, 2, 3, 5, 8, 13);
    
    @Comment("Set of strings (order preserved)")
    private Set<String> stringSet = new LinkedHashSet<>(Arrays.asList("one", "two", "three"));
    
    @Comment("Set of enums")
    private Set<TestEnum> enumSet = new LinkedHashSet<>(Arrays.asList(TestEnum.FIRST, TestEnum.SECOND));
    
    // === MAPS ===
    @Comment("Simple string-to-string map")
    private Map<String, String> simpleMap = new LinkedHashMap<String, String>() {{
        put("key1", "value1");
        put("key2", "value2");
    }};
    
    @Comment("Map with integer keys")
    private Map<Integer, String> intKeyMap = new LinkedHashMap<Integer, String>() {{
        put(1, "one");
        put(2, "two");
    }};
    
    @Comment("Nested map")
    private Map<String, Map<String, Integer>> nestedMap = new LinkedHashMap<String, Map<String, Integer>>() {{
        put("group1", new LinkedHashMap<String, Integer>() {{
            put("a", 1);
            put("b", 2);
        }});
    }};
    
    @Comment("Map with enum keys")
    private Map<TestEnum, String> enumKeyMap = new LinkedHashMap<TestEnum, String>() {{
        put(TestEnum.FIRST, "first value");
        put(TestEnum.SECOND, "second value");
    }};
    
    // === ENUMS ===
    @Comment("Simple enum")
    private TestEnum singleEnum = TestEnum.THIRD;
    
    @Comment("List of enums")
    private List<TestEnum> enumList = Arrays.asList(TestEnum.FIRST, TestEnum.THIRD);
    
    // === NESTED CONFIGS ===
    @Comment("Nested subconfig")
    private SubConfig subConfig = new SubConfig();
    
    @Comment("List of nested configs")
    private List<SubConfig> subConfigList = Arrays.asList(
        new SubConfig("sub1", 10),
        new SubConfig("sub2", 20)
    );
    
    // === SERIALIZABLE ===
    @Comment("Serializable custom object")
    private CustomSerializable customObj = new CustomSerializable("test", 999);
    
    // === ANNOTATIONS ===
    @CustomKey("custom-key-field")
    @Comment("Field with custom key")
    private String customKeyField = "custom value";
    
    @Variable("TEST_VARIABLE")
    @Comment("Field backed by environment variable")
    private String variableField = "default";
    
    @Exclude
    private String excludedField = "should not appear";
    
    // === EDGE CASES ===
    @Comment("Null value test")
    private String nullValue = null;
    
    @Comment("Empty collection")
    private List<String> emptyList = new ArrayList<>();
    
    @Comment("Empty map")
    private Map<String, String> emptyMap = new LinkedHashMap<>();
    
    // === NESTED CLASSES ===
    
    public static class SubConfig extends OkaeriConfig {
        @Comment("Subconfig field")
        private String subField = "default sub";
        private int subNumber = 42;
        
        public SubConfig() {}
        public SubConfig(String subField, int subNumber) {
            this.subField = subField;
            this.subNumber = subNumber;
        }
        
        // getters/setters (lombok recommended)
    }
    
    public enum TestEnum {
        FIRST, SECOND, THIRD
    }
    
    public static class CustomSerializable implements Serializable {
        private static final long serialVersionUID = 1L;
        private String name;
        private int id;
        
        public CustomSerializable() {}
        public CustomSerializable(String name, int id) {
            this.name = name;
            this.id = id;
        }
        
        // getters/setters (lombok recommended)
    }
}
```

All format implementations depend on `core-test-commons` and use MegaConfig for E2E testing.

**Format modules add dependency:**
```xml
<dependency>
    <groupId>eu.okaeri</groupId>
    <artifactId>okaeri-configs-test-commons</artifactId>
    <version>${project.version}</version>
    <scope>test</scope>
</dependency>
```

---

## Test Organization

### Directory Structure

```
core/src/test/java/eu/okaeri/configs/
├── lifecycle/
│   ├── ConfigCreationTest.java
│   ├── ConfigSaveTest.java
│   ├── ConfigLoadTest.java
│   ├── ConfigUpdateTest.java
│   ├── ConfigGetSetTest.java
│   └── ConfigMapConversionTest.java
├── types/
│   ├── PrimitiveTypesTest.java
│   ├── BasicTypesTest.java
│   ├── CollectionTypesTest.java
│   ├── MapTypesTest.java
│   ├── EnumTypesTest.java
│   ├── SubconfigTypesTest.java
│   ├── SerializableTypesTest.java
│   └── TypeTransformationsTest.java
├── annotations/
│   ├── HeaderAnnotationTest.java
│   ├── CommentAnnotationTest.java
│   ├── CustomKeyAnnotationTest.java
│   ├── VariableAnnotationTest.java
│   ├── ExcludeAnnotationTest.java
│   ├── NamesAnnotationTest.java
│   ├── TargetTypeAnnotationTest.java
│   └── IncludeAnnotationTest.java
├── schema/
│   ├── ConfigDeclarationTest.java
│   ├── FieldDeclarationTest.java
│   └── GenericsDeclarationTest.java
├── serdes/
│   ├── SerdesRegistryTest.java
│   ├── SerializationDataTest.java
│   ├── DeserializationDataTest.java
│   ├── SerdesContextTest.java
│   └── StandardSerdesTest.java
├── migration/
│   ├── ConfigMigrationTest.java
│   ├── ConfigMigrationDslTest.java
│   └── RawConfigViewTest.java
├── manager/
│   └── ConfigManagerTest.java
└── integration/
    ├── CompleteWorkflowTest.java
    ├── OrphanHandlingTest.java
    ├── CrossFormatTest.java
    └── EdgeCasesTest.java
```

### Test Resources

```
core/src/test/resources/
├── configs/
│   ├── simple-config.yml
│   ├── complex-config.yml
│   ├── all-types.yml
│   ├── nested-config.yml
│   └── migration-source.yml
└── fixtures/
    └── expected-output-*.txt
```

### Naming Conventions

1. **Test Classes**: `[Feature]Test.java` (e.g., `ConfigCreationTest.java`)
2. **Test Methods**: `testFeature_Scenario_ExpectedOutcome()`
   - Example: `testSave_WithOrphanRemoval_RemovesUndeclaredKeys()`
   - Example: `testLoad_FromMalformedFile_ThrowsException()`
3. **Test Config Classes**: Use `TestConfigs` from test-commons or local class

### Using Java Text Blocks for Inline Configs

For simple tests, use Java 15+ text blocks (`"""`) for inline config data:

```java
@Test
void testLoad_FromYamlString_LoadsCorrectly() throws Exception {
    String yaml = """
        name: Test Name
        value: 42
        items:
          - alpha
          - beta
          - gamma
        """;
    
    TestConfigs.SimpleConfig config = ConfigManager.create(TestConfigs.SimpleConfig.class);
    config.withConfigurer(new YamlSnakeYamlConfigurer());
    config.load(yaml);
    
    assertThat(config.getName()).isEqualTo("Test Name");
    assertThat(config.getValue()).isEqualTo(42);
}
```

---

## CI/CD Setup

### GitHub Actions Configuration

**File: `.github/workflows/test.yml`**

```yaml
name: Tests

on:
  push:
    branches: [ master, develop ]
  pull_request:
    branches: [ master, develop ]

jobs:
  test:
    name: Test on JDK ${{ matrix.java }}
    runs-on: ubuntu-latest
    
    strategy:
      matrix:
        java: [ 8, 11, 17, 21 ]
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK ${{ matrix.java }}
        uses: actions/setup-java@v3
        with:
          java-version: ${{ matrix.java }}
          distribution: 'temurin'
          cache: 'maven'
      
      - name: Run tests
        run: mvn clean test -B
      
      - name: Generate coverage report
        if: matrix.java == '11'
        run: mvn jacoco:report
      
      - name: Upload coverage to Codecov
        if: matrix.java == '11'
        uses: codecov/codecov-action@v3
        with:
          files: ./core/target/site/jacoco/jacoco.xml
          flags: core

  integration:
    name: Integration Tests
    runs-on: ubuntu-latest
    needs: test
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 11
        uses: actions/setup-java@v3
        with:
          java-version: '11'
          distribution: 'temurin'
          cache: 'maven'
      
      - name: Run integration tests
        run: mvn verify -B -Pintegration-tests
```

### Maven Configuration

**File: `pom.xml` (root)**

```xml
<build>
    <plugins>
        <!-- Surefire for unit tests -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>3.0.0</version>
            <configuration>
                <includes>
                    <include>**/*Test.java</include>
                </includes>
                <excludes>
                    <exclude>**/*IntegrationTest.java</exclude>
                </excludes>
            </configuration>
        </plugin>
        
        <!-- Failsafe for integration tests -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-failsafe-plugin</artifactId>
            <version>3.0.0</version>
            <configuration>
                <includes>
                    <include>**/*IntegrationTest.java</include>
                </includes>
            </configuration>
            <executions>
                <execution>
                    <goals>
                        <goal>integration-test</goal>
                        <goal>verify</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
        
        <!-- JaCoCo for coverage -->
        <plugin>
            <groupId>org.jacoco</groupId>
            <artifactId>jacoco-maven-plugin</artifactId>
            <version>0.8.10</version>
            <executions>
                <execution>
                    <goals>
                        <goal>prepare-agent</goal>
                    </goals>
                </execution>
                <execution>
                    <id>report</id>
                    <phase>test</phase>
                    <goals>
                        <goal>report</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>

<dependencies>
    <!-- JUnit 5 -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>5.10.0</version>
        <scope>test</scope>
    </dependency>
    
    <!-- AssertJ for fluent assertions -->
    <dependency>
        <groupId>org.assertj</groupId>
        <artifactId>assertj-core</artifactId>
        <version>3.27.6</version>
        <scope>test</scope>
    </dependency>
    
    <!-- Test Commons (shared utilities) -->
    <dependency>
        <groupId>eu.okaeri</groupId>
        <artifactId>okaeri-configs-test-commons</artifactId>
        <version>${project.version}</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### Test Execution Strategy

1. **Fast Feedback Loop**
   - Unit tests run on every commit
   - Fail fast on errors
   - Parallel execution where safe

2. **Comprehensive Validation**
   - Integration tests run on PR
   - Full test suite on merge to master
   - Multi-JDK testing (8, 11, 17, 21)

3. **Coverage Tracking**
   - JaCoCo reports generated
   - Codecov integration for visualization
   - Coverage trends monitored (but not gated)

---

## Questions & Decisions

### Decisions Made

✅ **Test Framework**: JUnit 5 (Jupiter) + AssertJ  
✅ **Assertion Library**: AssertJ for fluent, readable assertions  
✅ **Mocking**: No mocking, real objects only  
✅ **Focus**: Core functionality first, format implementations via MegaConfig E2E  
✅ **Coverage**: Feature-driven, comprehensive but not metric-focused  
✅ **Primary Format**: YAML with SnakeYAML  
✅ **Test Organization**: Separate core-test-commons module for shared utilities  
✅ **Platform Testing**: Don't test platform-specific types (ItemStack, etc.), just test formats  
✅ **Test Data**: Inline using Java text blocks for simple tests, external files for complex scenarios  
✅ **Performance**: No performance benchmarks for now  
✅ **CI**: GitHub Actions, multi-JDK support  

### Decisions Confirmed

#### 1. MegaConfig Complexity ✅ **CONFIRMED**
**Decision**: Keep it as one massive, comprehensive config
- Easier to maintain as single source of truth
- Better for E2E testing consistency across formats
- Well-organized with clear section comments (primitives, wrappers, strings, etc.)
- Includes everything: all types, annotations, unicode (Russian/Polish), edge cases

#### 2. Format Test Structure ✅ **CONFIRMED**
**Decision**: Each format has **three separate test categories**:
1. **Configurer-Specific Features Test** - Format capabilities, internal operations, comment/header handling
2. **Edge Cases Test** - Boundary conditions, malformed input, format-specific edge cases
3. **E2E MegaConfig Test** - Comprehensive integration test using shared MegaConfig

This ensures thorough testing without meaningless coverage inflation.

#### 3. Core Test Module Structure ✅ **CONFIRMED**
**Decision**: Separate **core-test** module for core functionality tests
- **core-test**: All core library functionality tests (lifecycle, types, annotations, schema, serdes, migrations)
- **core**: Only InMemoryConfigurer tests (basic test suite for the configurer itself)
- **core-test-commons**: Shared test utilities and configs
- Primary format for core-test: SnakeYAML (for full serialization cycle testing)

This avoids skipping the serdes layer while keeping tests organized.

#### 4. Test Config Organization ✅ **CONFIRMED**
**Decision**: Feature-specific configs, not generic TestConfigs
- Use dedicated configs: `PrimitivesTestConfig`, `CollectionsTestConfig`, `MapsTestConfig`, `EnumsTestConfig`, `NestedTestConfig`, `SerializableTestConfig`, `AnnotationsTestConfig`
- Each tests a specific feature category
- MegaConfig is "the one to rule them all" for E2E testing
- All configs use Lombok (@Data, @EqualsAndHashCode, etc.)
- Include at least one Serializable example in SerializableTestConfig

---

## Implementation Roadmap

### Phase 1: Foundation (Week 1-2)
- [ ] Create core-test-commons module
- [ ] Implement TestUtils class
- [ ] Implement TestConfigs class (simple configs)
- [ ] Implement MegaConfig (comprehensive E2E config)
- [ ] Set up test infrastructure (directories, dependencies)
- [ ] Implement lifecycle tests (creation, save, load)
- [ ] Implement basic type tests (primitives, strings)
- [ ] Set up CI pipeline skeleton

### Phase 2: Core Features (Week 3-4)
- [ ] Complete all type system tests
- [ ] Implement annotation tests
- [ ] Implement schema system tests
- [ ] Basic format implementation test (SnakeYAML with MegaConfig)

### Phase 3: Advanced Features (Week 5-6)
- [ ] Serdes system tests
- [ ] Migration system tests
- [ ] ConfigManager tests
- [ ] Cross-format tests

### Phase 4: Integration & Polish (Week 7-8)
- [ ] Complete workflow integration tests
- [ ] Edge case tests
- [ ] Remaining format implementation tests (HJSON, JSON, HOCON, Bukkit, Bungee)
- [ ] CI/CD finalization
- [ ] Documentation review

### Phase 5: Coverage Review
- [ ] Identify any gaps in feature coverage
- [ ] Add missing tests
- [ ] Final validation
- [ ] Prepare for collaborative development

---

## Success Criteria

### Test Suite Should:
1. ✅ Cover all documented features
2. ✅ Test all annotations and their behaviors
3. ✅ Test all supported types
4. ✅ Test error cases and exceptions
5. ✅ Test edge cases (null, empty, large, etc.)
6. ✅ Run fast (unit tests < 30s total)
7. ✅ Be maintainable (clear, documented, DRY)
8. ✅ Enable safe refactoring
9. ✅ Work across JDK 8, 11, 17, 21
10. ✅ Serve as living documentation

### What Success Looks Like:
- Any developer can understand a feature by reading its tests
- Breaking changes are caught immediately by CI
- Refactoring is safe and confident
- New features can be TDD'd
- Bug fixes include regression tests
- Contributors know what to test
- MegaConfig works consistently across all formats

---

## Next Steps

1. **Review this updated plan** - Address any remaining questions
2. **Approve test strategy** - Get consensus on approach
3. **Create core-test-commons module** - Set up shared utilities
4. **Implement MegaConfig** - Comprehensive test config
5. **Start with Phase 1** - Begin implementation
6. **Iterate and improve** - Adjust plan based on learnings

---

## Additional Notes

### Why No Mocks?
Mocking frameworks like Mockito are powerful but can lead to tests that:
- Test mock behavior instead of real behavior
- Become brittle and coupled to implementation
- Miss integration issues

Without mocks, we:
- Test real behavior
- Catch actual bugs
- Have more confidence in tests

### Why Feature-Driven?
Coverage metrics can be gamed. High coverage ≠ good tests. Instead:
- Each feature should have explicit tests
- Edge cases should be documented and tested
- Tests should fail when features break
- Tests should serve as examples

### Why JUnit 5 + AssertJ?
- **JUnit 5**: Modern, well-maintained, excellent parameterized tests, good IDE integration, industry standard
- **AssertJ**: Fluent assertions, better error messages, rich API for collections/exceptions/etc.
- Together they provide: Clear test intent, readable failures, powerful assertions

### Why core-test-commons Module?
- **Shared utilities**: Avoid code duplication across test modules
- **Consistent MegaConfig**: All formats test against the same comprehensive config
- **Reusable test configs**: Common scenarios available to all modules
- **Better organization**: Clear separation between test code and test utilities

---

**Document Version**: 2.0  
**Last Updated**: 2025-10-15  
**Author**: Test Implementation Planning  
**Status**: Updated - Ready for Review
