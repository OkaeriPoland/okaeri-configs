# Okaeri Configs - Library Bugs

This document details library bugs discovered during comprehensive test implementation (Session 21).

**Status**: 619/621 tests passing (99.7%)  
**Failing Tests**: 2 (intentionally failing to document bugs)  
**Priority**: High - These bugs affect core functionality

---

## Bug #1: Nested Orphan Removal Not Working

### Summary
When `withRemoveOrphans(true)` is set, orphaned keys are only removed at the root level. Orphaned keys inside nested configs (subconfigs) are **not** removed, leading to incomplete cleanup.

### Failing Test
- **Test**: `OrphanHandlingTest.testOrphans_RemovalWithNestedConfigs_RemovesAllOrphans`
- **File**: `core-test/src/test/java/eu/okaeri/configs/integration/OrphanHandlingTest.java`
- **Line**: 291

### Test Code
```java
@Test
void testOrphans_RemovalWithNestedConfigs_RemovesAllOrphans() throws Exception {
    File configFile = tempDir.resolve("orphans7.yml").toFile();
    
    // Create file with nested orphans
    String yaml = """
        declaredNested:
          nestedField: "nested value"
          orphanInNested: "should be removed"
        orphanAtRoot: "should be removed"
        """;
    TestUtils.writeFile(configFile, yaml);
    
    // Load
    ParentConfig config = ConfigManager.create(ParentConfig.class);
    config.withConfigurer(new YamlSnakeYamlConfigurer());
    config.withBindFile(configFile);
    config.withRemoveOrphans(true);
    config.load();
    config.save();
    
    // Verify orphans removed
    String content = TestUtils.readFile(configFile);
    assertThat(content).doesNotContain("orphanAtRoot");
    assertThat(content).doesNotContain("orphanInNested"); // ❌ FAILS - orphan still present
    assertThat(content).contains("nestedField");
}
```

### Current Behavior
**After save with `removeOrphans=true`:**
```yaml
declaredNested:
  nestedField: nested value
  orphanInNested: should be removed  # ❌ STILL HERE!
# orphanAtRoot removed ✅
```

**Console output:**
```
WARNING: Removed orphaned (undeclared) keys: [orphanAtRoot]
```

Only `orphanAtRoot` is removed. `orphanInNested` persists.

### Expected Behavior
**After save with `removeOrphans=true`:**
```yaml
declaredNested:
  nestedField: nested value
# Both orphans removed ✅
```

**Console output:**
```
WARNING: Removed orphaned (undeclared) keys: [orphanAtRoot, declaredNested.orphanInNested]
```

Both orphans should be removed recursively.

### Root Cause Analysis

The orphan removal logic is likely in `OkaeriConfig.save()` method:

**Suspected Issue Location:** `core/src/main/java/eu/okaeri/configs/OkaeriConfig.java`

**Current implementation** (suspected):
```java
public OkaeriConfig save() throws OkaeriException {
    // ... other logic ...
    
    if (this.removeOrphans) {
        // Get declared fields from declaration
        Set<String> declaredKeys = this.declaration.getFields().keySet();
        
        // Get all keys from configurer
        Set<String> configurerKeys = this.configurer.getAllKeys();
        
        // Find orphans (keys in configurer but not in declaration)
        Set<String> orphanKeys = new HashSet<>(configurerKeys);
        orphanKeys.removeAll(declaredKeys);
        
        // Remove orphans
        for (String orphan : orphanKeys) {
            this.configurer.remove(orphan);  // ❌ Only removes root-level keys
        }
        
        if (!orphanKeys.isEmpty()) {
            this.logger.warning("Removed orphaned (undeclared) keys: " + orphanKeys);
        }
    }
    
    // ... write to file ...
}
```

**Problem**: The orphan detection only checks root-level keys. It doesn't recursively check nested configs.

### Proposed Fix

**Option 1: Recursive Orphan Detection (Preferred)**

Modify `OkaeriConfig.save()` to recursively detect and remove orphans in nested configs:

```java
public OkaeriConfig save() throws OkaeriException {
    // ... other logic ...
    
    if (this.removeOrphans) {
        Set<String> allOrphans = new LinkedHashSet<>();
        removeOrphansRecursively(this, "", allOrphans);
        
        if (!allOrphans.isEmpty()) {
            this.logger.warning("Removed orphaned (undeclared) keys: " + allOrphans);
        }
    }
    
    // ... write to file ...
}

/**
 * Recursively remove orphans from this config and all nested configs.
 * 
 * @param config The config to process
 * @param prefix Key prefix for nested path tracking
 * @param orphans Set to collect orphan key names
 */
private void removeOrphansRecursively(OkaeriConfig config, String prefix, Set<String> orphans) {
    ConfigDeclaration declaration = config.getDeclaration();
    Configurer configurer = config.getConfigurer();
    
    if (declaration == null || configurer == null) {
        return;
    }
    
    // Get declared fields
    Set<String> declaredKeys = declaration.getFields().keySet();
    
    // Get all keys from configurer
    Set<String> configurerKeys = configurer.getAllKeys();
    
    // Find orphans at this level
    Set<String> orphanKeys = new HashSet<>(configurerKeys);
    orphanKeys.removeAll(declaredKeys);
    
    // Remove orphans and track them
    for (String orphan : orphanKeys) {
        configurer.remove(orphan);
        orphans.add(prefix.isEmpty() ? orphan : prefix + "." + orphan);
    }
    
    // Recursively process nested configs
    for (Map.Entry<String, FieldDeclaration> entry : declaration.getFields().entrySet()) {
        String key = entry.getKey();
        FieldDeclaration field = entry.getValue();
        
        // Check if field is a nested OkaeriConfig
        if (OkaeriConfig.class.isAssignableFrom(field.getType())) {
            try {
                Object fieldValue = field.getValue();
                if (fieldValue instanceof OkaeriConfig) {
                    OkaeriConfig nestedConfig = (OkaeriConfig) fieldValue;
                    String nestedPrefix = prefix.isEmpty() ? key : prefix + "." + key;
                    
                    // Recurse into nested config
                    removeOrphansRecursively(nestedConfig, nestedPrefix, orphans);
                }
            } catch (Exception e) {
                this.logger.warning("Failed to process nested config at " + key + ": " + e.getMessage());
            }
        }
    }
}
```

**Option 2: Delegate to Nested Configs**

Each nested config handles its own orphan removal:

```java
public OkaeriConfig save() throws OkaeriException {
    // ... other logic ...
    
    if (this.removeOrphans) {
        // Remove orphans at this level
        Set<String> declaredKeys = this.declaration.getFields().keySet();
        Set<String> configurerKeys = this.configurer.getAllKeys();
        Set<String> orphanKeys = new HashSet<>(configurerKeys);
        orphanKeys.removeAll(declaredKeys);
        
        for (String orphan : orphanKeys) {
            this.configurer.remove(orphan);
        }
        
        if (!orphanKeys.isEmpty()) {
            this.logger.warning("Removed orphaned (undeclared) keys: " + orphanKeys);
        }
        
        // Recursively trigger save on nested configs
        for (Map.Entry<String, FieldDeclaration> entry : this.declaration.getFields().entrySet()) {
            FieldDeclaration field = entry.getValue();
            if (OkaeriConfig.class.isAssignableFrom(field.getType())) {
                try {
                    Object fieldValue = field.getValue();
                    if (fieldValue instanceof OkaeriConfig) {
                        OkaeriConfig nestedConfig = (OkaeriConfig) fieldValue;
                        nestedConfig.withRemoveOrphans(this.removeOrphans);
                        nestedConfig.update(); // Sync nested config to parent's configurer
                    }
                } catch (Exception e) {
                    this.logger.warning("Failed to update nested config: " + e.getMessage());
                }
            }
        }
    }
    
    // ... write to file ...
}
```

### Implementation Notes

1. **Test Coverage**: Once fixed, the failing test should pass
2. **Backward Compatibility**: The fix is backward compatible - it only affects behavior when `removeOrphans=true`
3. **Performance**: Recursive approach may be slightly slower for deeply nested configs, but impact should be negligible
4. **Configurer Contract**: Need to verify `Configurer.remove()` and `Configurer.getAllKeys()` work correctly for nested structures

### Impact Assessment

**Severity**: Medium-High  
**Users Affected**: Any code using `withRemoveOrphans(true)` with nested configs  
**Workaround**: Manually remove nested orphans before saving, or avoid using nested configs with orphan removal

---

## Bug #2: transformCopy Doesn't Sync Field State

### Summary
`ConfigManager.transformCopy()` does not synchronize field values to the configurer before copying. If fields are modified programmatically (via setters) after the config is created, these changes are **not** reflected in the copied config.

### Failing Test
- **Test**: `CrossFormatTest.testCrossFormat_ComplexNestedStructures_PreservedInOperations`
- **File**: `core-test/src/test/java/eu/okaeri/configs/integration/CrossFormatTest.java`
- **Line**: 207

### Test Code
```java
@Test
void testCrossFormat_ComplexNestedStructures_PreservedInOperations() throws Exception {
    // Original config
    Outer original = ConfigManager.create(Outer.class);
    original.withConfigurer(new YamlSnakeYamlConfigurer());
    original.setOuterField("modified");  // Modify field via setter
    original.getNested().setInnerField("modified nested");
    
    // Transform copy
    Outer copy1 = ConfigManager.transformCopy(original, Outer.class);
    assertThat(copy1.getOuterField()).isEqualTo("modified"); // ❌ FAILS - returns "outer" (default)
    assertThat(copy1.getNested().getInnerField()).isEqualTo("modified nested");
    // ...
}
```

### Current Behavior
```java
Outer original = ConfigManager.create(Outer.class);
original.withConfigurer(new YamlSnakeYamlConfigurer());
original.setOuterField("modified");  // Field changed in memory

// transformCopy doesn't see the modified field
Outer copy = ConfigManager.transformCopy(original, Outer.class);
System.out.println(copy.getOuterField());  // Prints "outer" (default) ❌
```

**Expected**: `"modified"`  
**Actual**: `"outer"` (default value)

### Expected Behavior
```java
Outer original = ConfigManager.create(Outer.class);
original.withConfigurer(new YamlSnakeYamlConfigurer());
original.setOuterField("modified");

// transformCopy should see modified field
Outer copy = ConfigManager.transformCopy(original, Outer.class);
System.out.println(copy.getOuterField());  // Should print "modified" ✅
```

### Root Cause Analysis

**Location:** `core/src/main/java/eu/okaeri/configs/ConfigManager.java`

**Current implementation:**
```java
public static <T extends OkaeriConfig> T transformCopy(@NonNull OkaeriConfig config, @NonNull Class<T> targetClazz) {
    Configurer configurer = config.getConfigurer();
    
    if (configurer == null) {
        throw new IllegalStateException("Config must have a configurer for transformCopy");
    }
    
    // Create new instance
    T copy = create(targetClazz);
    
    // Convert source config to map
    Map<String, Object> data = config.asMap(configurer, true);
    
    // Load data into new instance
    copy.load(data);
    
    return copy;
}
```

**Problem**: The method calls `config.asMap()` which reads from the **configurer**, not from the actual field values. If fields are modified via setters without calling `update()`, the configurer doesn't know about the changes.

**Why this happens**:
1. User creates config: `Outer original = ConfigManager.create(Outer.class);`
2. User modifies field: `original.setOuterField("modified");` - This changes the field in memory but **doesn't** update the configurer
3. User calls transformCopy: `ConfigManager.transformCopy(original, Outer.class);`
4. transformCopy reads from configurer via `asMap()` - But configurer still has default values!
5. Result: copied config has default values, not modified values

### Proposed Fix

**Option 1: Call update() Before asMap() (Preferred)**

Modify `transformCopy()` to ensure latest field state:

```java
/**
 * Creates a copy of a config by transforming it through map representation.
 * This method ensures the latest field values are captured before copying.
 * 
 * @param config The config to copy
 * @param targetClazz The target config class
 * @param <T> The target type
 * @return A new config instance with copied data
 */
public static <T extends OkaeriConfig> T transformCopy(@NonNull OkaeriConfig config, @NonNull Class<T> targetClazz) {
    Configurer configurer = config.getConfigurer();
    
    if (configurer == null) {
        throw new IllegalStateException("Config must have a configurer for transformCopy");
    }
    
    // Ensure latest field values are in configurer
    config.update();  // ✅ ADDED: Sync fields to configurer
    
    // Create new instance
    T copy = create(targetClazz);
    
    // Convert source config to map
    Map<String, Object> data = config.asMap(configurer, true);
    
    // Load data into new instance
    copy.load(data);
    
    return copy;
}
```

**Option 2: Read Directly from Fields**

Alternative approach - read from fields directly instead of configurer:

```java
public static <T extends OkaeriConfig> T transformCopy(@NonNull OkaeriConfig config, @NonNull Class<T> targetClazz) {
    // Create new instance
    T copy = create(targetClazz);
    
    // Read directly from source fields (bypassing configurer)
    ConfigDeclaration sourceDeclaration = config.getDeclaration();
    Map<String, Object> data = new LinkedHashMap<>();
    
    for (Map.Entry<String, FieldDeclaration> entry : sourceDeclaration.getFields().entrySet()) {
        String key = entry.getKey();
        FieldDeclaration field = entry.getValue();
        
        try {
            Object value = field.getValue();  // Read from field directly
            data.put(key, value);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read field " + key, e);
        }
    }
    
    // Load data into new instance
    copy.load(data);
    
    return copy;
}
```

**Recommendation**: **Option 1** is preferred because:
- Simpler implementation (one line change)
- Maintains existing behavior and contracts
- Reuses existing `update()` logic
- Users expect `update()` to be called before operations that read state

### Implementation Notes

1. **Test Coverage**: Once fixed, the failing test should pass
2. **Performance**: Calling `update()` adds minimal overhead
3. **Consistency**: This makes `transformCopy()` consistent with other operations like `save()` which also sync state
4. **Documentation**: Update JavaDoc to clarify that `transformCopy()` captures current field state

### Related Methods to Review

Consider applying similar fix to related methods:

```java
// Should these also call update() first?
public static <T extends OkaeriConfig> T deepCopy(...)
public Map<String, Object> asMap(...)
```

**Answer**: 
- `deepCopy()` - YES, should also call `update()` first
- `asMap()` - Consider documenting that it reads from configurer, or add a variant that reads from fields

### Impact Assessment

**Severity**: Medium  
**Users Affected**: Any code using `transformCopy()` or `deepCopy()` with programmatically modified fields  
**Workaround**: Manually call `config.update()` before `transformCopy()`

---

## Test Status

### Affected Tests
```
Tests run: 621
Failures: 2

❌ OrphanHandlingTest.testOrphans_RemovalWithNestedConfigs_RemovesAllOrphans
   → Bug #1: Nested orphan removal

❌ CrossFormatTest.testCrossFormat_ComplexNestedStructures_PreservedInOperations
   → Bug #2: transformCopy state sync
```

### Expected After Fixes
```
Tests run: 621
Failures: 0
Success rate: 100% ✅
```

---

## Priority & Roadmap

### Priority 1 (High) - Fix Before Release
- ✅ Bug #1: Nested orphan removal
- ✅ Bug #2: transformCopy state sync

### Priority 2 (Medium) - Consider for Next Release
- Review `deepCopy()` for same issue as Bug #2
- Add `asMapFromFields()` variant that reads directly from fields
- Performance optimization for recursive orphan removal

### Priority 3 (Low) - Nice to Have
- Add configuration option for orphan removal verbosity
- Add metrics/statistics for orphan removal operations

---

## References

**Failing Tests**:
- `core-test/src/test/java/eu/okaeri/configs/integration/OrphanHandlingTest.java:291`
- `core-test/src/test/java/eu/okaeri/configs/integration/CrossFormatTest.java:207`

**Test Execution**:
```bash
./run-tests.sh
```

**Full Test Report**:
- See `TEST_IMPL_PROGRESS.md` - Session 21
- Test output: `core-test/target/surefire-reports/`

---

**Document Version**: 1.0  
**Created**: 2025-10-16  
**Last Updated**: 2025-10-16 14:47  
**Status**: Active - Bugs Documented, Fixes Proposed
