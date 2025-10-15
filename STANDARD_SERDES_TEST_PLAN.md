# StandardSerdes Testing Plan

## Overview

This document tracks the comprehensive testing plan for `StandardSerdes` - the core serialization/deserialization framework in okaeri-configs. StandardSerdes registers all built-in transformers that enable type conversions.

**Important**: This library had **zero tests** until now, so failing tests may indicate bugs in the library itself, not in the tests.

## StandardSerdes Components

### Registered Transformers

Located in `core/src/main/java/eu/okaeri/configs/serdes/standard/`:

1. **Magic Transformers**
   - `ObjectToStringTransformer` - Converts any Object to String via toString()
   - `StringToStringTransformer` - Identity transformation for String

2. **Bidirectional String Transformers** (registered with `registerWithReversedToString`)
   - `StringToBigDecimalTransformer` ↔ BigDecimal → String
   - `StringToBigIntegerTransformer` ↔ BigInteger → String
   - `StringToBooleanTransformer` ↔ Boolean → String
   - `StringToByteTransformer` ↔ Byte → String
   - `StringToCharacterTransformer` ↔ Character → String
   - `StringToDoubleTransformer` ↔ Double → String
   - `StringToFloatTransformer` ↔ Float → String
   - `StringToIntegerTransformer` ↔ Integer → String
   - `StringToLongTransformer` ↔ Long → String
   - `StringToShortTransformer` ↔ Short → String
   - `StringToUuidTransformer` ↔ UUID → String

3. **Serializers**
   - `ConfigSerializableSerializer` - Handles serializable objects

## Testing Strategy

### Test File Location
`core-test/src/test/java/eu/okaeri/configs/serdes/StandardSerdesTest.java`

### Test Categories

#### 1. Registration Tests
Verify that all transformers are properly registered:
- [ ] ObjectToStringTransformer is registered
- [ ] StringToStringTransformer is registered
- [ ] All String→Type transformers are registered
- [ ] All Type→String reverse transformers are registered
- [ ] ConfigSerializableSerializer is registered

#### 2. String → Type Transformations
Test each transformer's forward conversion:
- [ ] String → BigDecimal
- [ ] String → BigInteger
- [ ] String → Boolean ("true", "false", "TRUE", "1", "0")
- [ ] String → Byte
- [ ] String → Character
- [ ] String → Double
- [ ] String → Float
- [ ] String → Integer
- [ ] String → Long
- [ ] String → Short
- [ ] String → UUID

#### 3. Type → String Transformations
Test reverse transformations (registered via `registerWithReversedToString`):
- [ ] BigDecimal → String
- [ ] BigInteger → String
- [ ] Boolean → String
- [ ] Byte → String
- [ ] Character → String
- [ ] Double → String
- [ ] Float → String
- [ ] Integer → String ⚠️ **POTENTIAL BUG** (see Known Issues)
- [ ] Long → String
- [ ] Short → String
- [ ] UUID → String

#### 4. Object → String Magic Transformer
- [ ] Any object → String via toString()
- [ ] Null handling
- [ ] Custom objects with overridden toString()

#### 5. Edge Cases
- [ ] String → Type with invalid format (should throw)
- [ ] String → Numeric type with overflow
- [ ] String → Boolean with invalid values
- [ ] Null string to Type
- [ ] Empty string to Type
- [ ] Whitespace handling

#### 6. Round-Trip Tests
Verify data integrity through conversion cycles:
- [ ] Type → String → Type (all types)
- [ ] String → Type → String (all types)

#### 7. Integration with SerdesRegistry
- [ ] getTransformer() retrieves correct transformer
- [ ] canTransform() reports correct capability
- [ ] getTransformersFrom() lists all available transformers
- [ ] Transformer priority/ordering

## Known Issues

### 🐛 Issue #1: Integer → String Conversion Failure

**Status**: Discovered during ConfigGetSetTest implementation  
**Test**: `ConfigGetSetTest.testGet_WithClass_TypeConversion`  
**Expected**: Integer should convert to String via reverse transformer  
**Actual**: Conversion fails  
**Root Cause**: Unknown - needs investigation

**Evidence Location**: 
- File: `core-test/src/test/java/eu/okaeri/configs/lifecycle/ConfigGetSetTest.java`
- Test: `testGet_WithClass_TypeConversion`

**Investigation Needed**:
1. Check if `registerWithReversedToString()` properly creates reverse transformer
2. Verify transformer registration in SerdesRegistry
3. Check if Integer → String path exists in transformation graph
4. Test if other Type → String conversions work

**Workaround**: None - this is a core functionality issue

## Test Implementation Checklist

### Phase 1: Basic Transformer Tests
- [ ] Create StandardSerdesTest.java
- [ ] Test registration of all transformers
- [ ] Test all String → Type conversions
- [ ] Test all Type → String conversions

### Phase 2: Edge Cases & Error Handling
- [ ] Invalid format handling
- [ ] Null handling
- [ ] Overflow handling
- [ ] Empty string handling

### Phase 3: Integration Tests
- [ ] Round-trip conversion tests
- [ ] SerdesRegistry integration
- [ ] Transformer chain tests

### Phase 4: Bug Investigation
- [ ] Investigate Integer → String failure
- [ ] Test other Type → String conversions
- [ ] Fix or document issues

## Test Data Examples

### Valid Conversions
```java
// String → Integer
"123" → 123
"-456" → -456
"0" → 0

// String → Boolean
"true" → true
"false" → false
"TRUE" → true
"1" → true (if supported)

// String → Double
"3.14" → 3.14
"-2.71" → -2.71
"1.0E10" → 1.0E10

// UUID
"550e8400-e29b-41d4-a716-446655440000" → UUID
```

### Invalid Conversions (should throw)
```java
"abc" → Integer (NumberFormatException)
"999999999999999999999" → Integer (overflow)
"maybe" → Boolean (invalid value)
"" → Integer (empty)
```

## Success Criteria

1. ✅ All registered transformers are tested
2. ✅ All forward conversions (String → Type) work correctly
3. ✅ All reverse conversions (Type → String) work correctly
4. ✅ Edge cases are handled properly
5. ✅ Round-trip conversions maintain data integrity
6. ✅ Known bugs are documented and tracked
7. ✅ Integration with SerdesRegistry is verified

## References

- **StandardSerdes**: `core/src/main/java/eu/okaeri/configs/serdes/standard/StandardSerdes.java`
- **SerdesRegistry**: `core/src/main/java/eu/okaeri/configs/serdes/SerdesRegistry.java`
- **Transformer Interface**: `core/src/main/java/eu/okaeri/configs/serdes/ObjectTransformer.java`
- **Related Test**: `core-test/src/test/java/eu/okaeri/configs/lifecycle/ConfigGetSetTest.java`

---

**Document Version**: 1.0  
**Created**: 2025-10-15  
**Status**: Planning Phase  
**Test Coverage**: 0% (not yet implemented)
