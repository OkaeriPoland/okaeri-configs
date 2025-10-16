package eu.okaeri.configs.schema;

import eu.okaeri.configs.OkaeriConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for GenericsDeclaration - comprehensive type information and generic parameter handling.
 */
class GenericsDeclarationTest {

    // === Test Configs ===

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class GenericTypesConfig extends OkaeriConfig {
        private String stringField;
        private int primitiveInt;
        private Integer wrapperInt;
        private List<String> stringList;
        private Map<String, Integer> stringIntMap;
        private Map<String, List<Integer>> nestedGenericMap;
        private TestEnum enumField;
        private SubConfig subConfig;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class SubConfig extends OkaeriConfig {
        private String field;
    }

    public enum TestEnum {
        FIRST, SECOND
    }

    // === Tests ===

    // --- Factory Methods ---

    @Test
    void testOf_FromClass_CreatesDeclaration() {
        // When
        GenericsDeclaration declaration = GenericsDeclaration.of(String.class);

        // Then
        assertThat(declaration).isNotNull();
        assertThat(declaration.getType()).isEqualTo(String.class);
    }

    @Test
    void testOf_FromObject_CreatesDeclaration() {
        // When
        GenericsDeclaration declaration = GenericsDeclaration.of("test string");

        // Then
        assertThat(declaration).isNotNull();
        assertThat(declaration.getType()).isEqualTo(String.class);
    }

    @Test
    void testOf_FromGenericsDeclaration_ReturnsSame() {
        // Given
        GenericsDeclaration original = GenericsDeclaration.of(String.class);

        // When
        GenericsDeclaration result = GenericsDeclaration.of(original);

        // Then
        assertThat(result).isSameAs(original);
    }

    @Test
    void testOf_FromNull_ReturnsNull() {
        // When
        GenericsDeclaration declaration = GenericsDeclaration.of(null);

        // Then
        assertThat(declaration).isNull();
    }

    @Test
    void testOf_WithSubtypes_CreatesDeclarationWithGenerics() {
        // When
        GenericsDeclaration declaration = GenericsDeclaration.of(
            List.class,
            List.of(String.class)
        );

        // Then
        assertThat(declaration.getType()).isEqualTo(List.class);
        assertThat(declaration.hasSubtypes()).isTrue();
        assertThat(declaration.getSubtypeAtOrNull(0).getType()).isEqualTo(String.class);
    }

    // --- Type Detection ---

    @Test
    void testIsPrimitive_PrimitiveTypes_ReturnsTrue() throws Exception {
        // Given
        Field field = GenericTypesConfig.class.getDeclaredField("primitiveInt");
        GenericsDeclaration declaration = GenericsDeclaration.of(field.getGenericType());

        // Then
        assertThat(declaration.isPrimitive()).isTrue();
    }

    @Test
    void testIsPrimitive_WrapperTypes_ReturnsFalse() throws Exception {
        // Given
        Field field = GenericTypesConfig.class.getDeclaredField("wrapperInt");
        GenericsDeclaration declaration = GenericsDeclaration.of(field.getGenericType());

        // Then
        assertThat(declaration.isPrimitive()).isFalse();
    }

    @Test
    void testIsPrimitiveWrapper_WrapperTypes_ReturnsTrue() {
        // Given
        GenericsDeclaration declaration = GenericsDeclaration.of(Integer.class);

        // Then
        assertThat(declaration.isPrimitiveWrapper()).isTrue();
    }

    @Test
    void testIsPrimitiveWrapper_AllWrappers_ReturnsTrue() {
        assertThat(GenericsDeclaration.of(Boolean.class).isPrimitiveWrapper()).isTrue();
        assertThat(GenericsDeclaration.of(Byte.class).isPrimitiveWrapper()).isTrue();
        assertThat(GenericsDeclaration.of(Character.class).isPrimitiveWrapper()).isTrue();
        assertThat(GenericsDeclaration.of(Double.class).isPrimitiveWrapper()).isTrue();
        assertThat(GenericsDeclaration.of(Float.class).isPrimitiveWrapper()).isTrue();
        assertThat(GenericsDeclaration.of(Integer.class).isPrimitiveWrapper()).isTrue();
        assertThat(GenericsDeclaration.of(Long.class).isPrimitiveWrapper()).isTrue();
        assertThat(GenericsDeclaration.of(Short.class).isPrimitiveWrapper()).isTrue();
    }

    @Test
    void testIsPrimitiveWrapper_NonWrapper_ReturnsFalse() {
        // Given
        GenericsDeclaration declaration = GenericsDeclaration.of(String.class);

        // Then
        assertThat(declaration.isPrimitiveWrapper()).isFalse();
    }

    @Test
    void testIsEnum_EnumType_ReturnsTrue() throws Exception {
        // Given
        Field field = GenericTypesConfig.class.getDeclaredField("enumField");
        GenericsDeclaration declaration = GenericsDeclaration.of(field.getGenericType());

        // Then
        assertThat(declaration.isEnum()).isTrue();
    }

    @Test
    void testIsEnum_NonEnumType_ReturnsFalse() {
        // Given
        GenericsDeclaration declaration = GenericsDeclaration.of(String.class);

        // Then
        assertThat(declaration.isEnum()).isFalse();
    }

    @Test
    void testIsConfig_OkaeriConfigSubclass_ReturnsTrue() throws Exception {
        // Given
        Field field = GenericTypesConfig.class.getDeclaredField("subConfig");
        GenericsDeclaration declaration = GenericsDeclaration.of(field.getGenericType());

        // Then
        assertThat(declaration.isConfig()).isTrue();
    }

    @Test
    void testIsConfig_NonConfig_ReturnsFalse() {
        // Given
        GenericsDeclaration declaration = GenericsDeclaration.of(String.class);

        // Then
        assertThat(declaration.isConfig()).isFalse();
    }

    // --- Generic Parameters ---

    @Test
    void testGenericParameters_SimpleList_CapturesTypeParameter() throws Exception {
        // Given
        Field field = GenericTypesConfig.class.getDeclaredField("stringList");
        GenericsDeclaration declaration = GenericsDeclaration.of(field.getGenericType());

        // Then
        assertThat(declaration.getType()).isEqualTo(List.class);
        assertThat(declaration.hasSubtypes()).isTrue();
        assertThat(declaration.getSubtypeAtOrNull(0)).isNotNull();
        assertThat(declaration.getSubtypeAtOrNull(0).getType()).isEqualTo(String.class);
    }

    @Test
    void testGenericParameters_SimpleMap_CapturesBothTypeParameters() throws Exception {
        // Given
        Field field = GenericTypesConfig.class.getDeclaredField("stringIntMap");
        GenericsDeclaration declaration = GenericsDeclaration.of(field.getGenericType());

        // Then
        assertThat(declaration.getType()).isEqualTo(Map.class);
        assertThat(declaration.hasSubtypes()).isTrue();
        assertThat(declaration.getSubtypeAtOrNull(0).getType()).isEqualTo(String.class);
        assertThat(declaration.getSubtypeAtOrNull(1).getType()).isEqualTo(Integer.class);
    }

    @Test
    void testGenericParameters_NestedGenerics_CapturesNestedStructure() throws Exception {
        // Given
        Field field = GenericTypesConfig.class.getDeclaredField("nestedGenericMap");
        GenericsDeclaration declaration = GenericsDeclaration.of(field.getGenericType());

        // Then
        assertThat(declaration.getType()).isEqualTo(Map.class);
        
        // Map key type
        GenericsDeclaration keyType = declaration.getSubtypeAtOrNull(0);
        assertThat(keyType.getType()).isEqualTo(String.class);
        
        // Map value type (List<Integer>)
        GenericsDeclaration valueType = declaration.getSubtypeAtOrNull(1);
        assertThat(valueType.getType()).isEqualTo(List.class);
        assertThat(valueType.getSubtypeAtOrNull(0).getType()).isEqualTo(Integer.class);
    }

    @Test
    void testGetSubtypeAtOrNull_ValidIndex_ReturnsSubtype() throws Exception {
        // Given
        Field field = GenericTypesConfig.class.getDeclaredField("stringList");
        GenericsDeclaration declaration = GenericsDeclaration.of(field.getGenericType());

        // When
        GenericsDeclaration subtype = declaration.getSubtypeAtOrNull(0);

        // Then
        assertThat(subtype).isNotNull();
        assertThat(subtype.getType()).isEqualTo(String.class);
    }

    @Test
    void testGetSubtypeAtOrNull_InvalidIndex_ReturnsNull() throws Exception {
        // Given
        Field field = GenericTypesConfig.class.getDeclaredField("stringList");
        GenericsDeclaration declaration = GenericsDeclaration.of(field.getGenericType());

        // When
        GenericsDeclaration subtype = declaration.getSubtypeAtOrNull(99);

        // Then
        assertThat(subtype).isNull();
    }

    @Test
    void testGetSubtypeAtOrNull_NoSubtypes_ReturnsNull() {
        // Given
        GenericsDeclaration declaration = GenericsDeclaration.of(String.class);

        // When
        GenericsDeclaration subtype = declaration.getSubtypeAtOrNull(0);

        // Then
        assertThat(subtype).isNull();
    }

    @Test
    void testGetSubtypeAtOrThrow_ValidIndex_ReturnsSubtype() throws Exception {
        // Given
        Field field = GenericTypesConfig.class.getDeclaredField("stringList");
        GenericsDeclaration declaration = GenericsDeclaration.of(field.getGenericType());

        // When
        GenericsDeclaration subtype = declaration.getSubtypeAtOrThrow(0);

        // Then
        assertThat(subtype).isNotNull();
        assertThat(subtype.getType()).isEqualTo(String.class);
    }

    @Test
    void testGetSubtypeAtOrThrow_InvalidIndex_ThrowsException() throws Exception {
        // Given
        Field field = GenericTypesConfig.class.getDeclaredField("stringList");
        GenericsDeclaration declaration = GenericsDeclaration.of(field.getGenericType());

        // When/then
        assertThatThrownBy(() -> declaration.getSubtypeAtOrThrow(99))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testHasSubtypes_WithGenerics_ReturnsTrue() throws Exception {
        // Given
        Field field = GenericTypesConfig.class.getDeclaredField("stringList");
        GenericsDeclaration declaration = GenericsDeclaration.of(field.getGenericType());

        // Then
        assertThat(declaration.hasSubtypes()).isTrue();
    }

    @Test
    void testHasSubtypes_WithoutGenerics_ReturnsFalse() {
        // Given
        GenericsDeclaration declaration = GenericsDeclaration.of(String.class);

        // Then
        assertThat(declaration.hasSubtypes()).isFalse();
    }

    // --- Primitive Operations ---

    @Test
    void testWrap_AllPrimitives_ReturnsWrapper() {
        assertThat(GenericsDeclaration.of(boolean.class).wrap()).isEqualTo(Boolean.class);
        assertThat(GenericsDeclaration.of(byte.class).wrap()).isEqualTo(Byte.class);
        assertThat(GenericsDeclaration.of(char.class).wrap()).isEqualTo(Character.class);
        assertThat(GenericsDeclaration.of(double.class).wrap()).isEqualTo(Double.class);
        assertThat(GenericsDeclaration.of(float.class).wrap()).isEqualTo(Float.class);
        assertThat(GenericsDeclaration.of(int.class).wrap()).isEqualTo(Integer.class);
        assertThat(GenericsDeclaration.of(long.class).wrap()).isEqualTo(Long.class);
        assertThat(GenericsDeclaration.of(short.class).wrap()).isEqualTo(Short.class);
    }

    @Test
    void testWrap_NonPrimitive_ReturnsNull() {
        // Given
        GenericsDeclaration declaration = GenericsDeclaration.of(String.class);

        // When
        Class<?> wrapped = declaration.wrap();

        // Then
        assertThat(wrapped).isNull();
    }

    // --- Type Matching ---

    @Test
    void testDoBoxTypesMatch_PrimitiveAndWrapper_ReturnsTrue() {
        // When/then
        assertThat(GenericsDeclaration.doBoxTypesMatch(int.class, Integer.class)).isTrue();
        assertThat(GenericsDeclaration.doBoxTypesMatch(Integer.class, int.class)).isTrue();
    }

    @Test
    void testDoBoxTypesMatch_AllPrimitiveWrapperPairs_ReturnsTrue() {
        assertThat(GenericsDeclaration.doBoxTypesMatch(boolean.class, Boolean.class)).isTrue();
        assertThat(GenericsDeclaration.doBoxTypesMatch(byte.class, Byte.class)).isTrue();
        assertThat(GenericsDeclaration.doBoxTypesMatch(char.class, Character.class)).isTrue();
        assertThat(GenericsDeclaration.doBoxTypesMatch(double.class, Double.class)).isTrue();
        assertThat(GenericsDeclaration.doBoxTypesMatch(float.class, Float.class)).isTrue();
        assertThat(GenericsDeclaration.doBoxTypesMatch(long.class, Long.class)).isTrue();
        assertThat(GenericsDeclaration.doBoxTypesMatch(short.class, Short.class)).isTrue();
    }

    @Test
    void testDoBoxTypesMatch_DifferentTypes_ReturnsFalse() {
        // When/then
        assertThat(GenericsDeclaration.doBoxTypesMatch(int.class, Long.class)).isFalse();
        assertThat(GenericsDeclaration.doBoxTypesMatch(String.class, Integer.class)).isFalse();
    }

    @Test
    void testIsUnboxedCompatibleWithBoxed_MatchingPair_ReturnsTrue() {
        // When/then
        assertThat(GenericsDeclaration.isUnboxedCompatibleWithBoxed(int.class, Integer.class)).isTrue();
    }

    @Test
    void testIsUnboxedCompatibleWithBoxed_MismatchedPair_ReturnsFalse() {
        // When/then
        assertThat(GenericsDeclaration.isUnboxedCompatibleWithBoxed(int.class, Long.class)).isFalse();
    }
}
