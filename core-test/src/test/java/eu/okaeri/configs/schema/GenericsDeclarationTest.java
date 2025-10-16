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
        // when
        GenericsDeclaration declaration = GenericsDeclaration.of(String.class);

        // then
        assertThat(declaration).isNotNull();
        assertThat(declaration.getType()).isEqualTo(String.class);
    }

    @Test
    void testOf_FromObject_CreatesDeclaration() {
        // when
        GenericsDeclaration declaration = GenericsDeclaration.of("test string");

        // then
        assertThat(declaration).isNotNull();
        assertThat(declaration.getType()).isEqualTo(String.class);
    }

    @Test
    void testOf_FromGenericsDeclaration_ReturnsSame() {
        // given
        GenericsDeclaration original = GenericsDeclaration.of(String.class);

        // when
        GenericsDeclaration result = GenericsDeclaration.of(original);

        // then
        assertThat(result).isSameAs(original);
    }

    @Test
    void testOf_FromNull_ReturnsNull() {
        // when
        GenericsDeclaration declaration = GenericsDeclaration.of((Object) null);

        // then
        assertThat(declaration).isNull();
    }

    @Test
    void testOf_WithSubtypes_CreatesDeclarationWithGenerics() {
        // when
        GenericsDeclaration declaration = GenericsDeclaration.of(
            List.class,
            Arrays.asList(String.class)
        );

        // then
        assertThat(declaration.getType()).isEqualTo(List.class);
        assertThat(declaration.hasSubtypes()).isTrue();
        assertThat(declaration.getSubtypeAtOrNull(0).getType()).isEqualTo(String.class);
    }

    // --- Type Detection ---

    @Test
    void testIsPrimitive_PrimitiveTypes_ReturnsTrue() throws Exception {
        // given
        Field field = GenericTypesConfig.class.getDeclaredField("primitiveInt");
        GenericsDeclaration declaration = GenericsDeclaration.of(field.getGenericType());

        // then
        assertThat(declaration.isPrimitive()).isTrue();
    }

    @Test
    void testIsPrimitive_WrapperTypes_ReturnsFalse() throws Exception {
        // given
        Field field = GenericTypesConfig.class.getDeclaredField("wrapperInt");
        GenericsDeclaration declaration = GenericsDeclaration.of(field.getGenericType());

        // then
        assertThat(declaration.isPrimitive()).isFalse();
    }

    @Test
    void testIsPrimitiveWrapper_WrapperTypes_ReturnsTrue() {
        // given
        GenericsDeclaration declaration = GenericsDeclaration.of(Integer.class);

        // then
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
        // given
        GenericsDeclaration declaration = GenericsDeclaration.of(String.class);

        // then
        assertThat(declaration.isPrimitiveWrapper()).isFalse();
    }

    @Test
    void testIsEnum_EnumType_ReturnsTrue() throws Exception {
        // given
        Field field = GenericTypesConfig.class.getDeclaredField("enumField");
        GenericsDeclaration declaration = GenericsDeclaration.of(field.getGenericType());

        // then
        assertThat(declaration.isEnum()).isTrue();
    }

    @Test
    void testIsEnum_NonEnumType_ReturnsFalse() {
        // given
        GenericsDeclaration declaration = GenericsDeclaration.of(String.class);

        // then
        assertThat(declaration.isEnum()).isFalse();
    }

    @Test
    void testIsConfig_OkaeriConfigSubclass_ReturnsTrue() throws Exception {
        // given
        Field field = GenericTypesConfig.class.getDeclaredField("subConfig");
        GenericsDeclaration declaration = GenericsDeclaration.of(field.getGenericType());

        // then
        assertThat(declaration.isConfig()).isTrue();
    }

    @Test
    void testIsConfig_NonConfig_ReturnsFalse() {
        // given
        GenericsDeclaration declaration = GenericsDeclaration.of(String.class);

        // then
        assertThat(declaration.isConfig()).isFalse();
    }

    // --- Generic Parameters ---

    @Test
    void testGenericParameters_SimpleList_CapturesTypeParameter() throws Exception {
        // given
        Field field = GenericTypesConfig.class.getDeclaredField("stringList");
        GenericsDeclaration declaration = GenericsDeclaration.of(field.getGenericType());

        // then
        assertThat(declaration.getType()).isEqualTo(List.class);
        assertThat(declaration.hasSubtypes()).isTrue();
        assertThat(declaration.getSubtypeAtOrNull(0)).isNotNull();
        assertThat(declaration.getSubtypeAtOrNull(0).getType()).isEqualTo(String.class);
    }

    @Test
    void testGenericParameters_SimpleMap_CapturesBothTypeParameters() throws Exception {
        // given
        Field field = GenericTypesConfig.class.getDeclaredField("stringIntMap");
        GenericsDeclaration declaration = GenericsDeclaration.of(field.getGenericType());

        // then
        assertThat(declaration.getType()).isEqualTo(Map.class);
        assertThat(declaration.hasSubtypes()).isTrue();
        assertThat(declaration.getSubtypeAtOrNull(0).getType()).isEqualTo(String.class);
        assertThat(declaration.getSubtypeAtOrNull(1).getType()).isEqualTo(Integer.class);
    }

    @Test
    void testGenericParameters_NestedGenerics_CapturesNestedStructure() throws Exception {
        // given
        Field field = GenericTypesConfig.class.getDeclaredField("nestedGenericMap");
        GenericsDeclaration declaration = GenericsDeclaration.of(field.getGenericType());

        // then
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
        // given
        Field field = GenericTypesConfig.class.getDeclaredField("stringList");
        GenericsDeclaration declaration = GenericsDeclaration.of(field.getGenericType());

        // when
        GenericsDeclaration subtype = declaration.getSubtypeAtOrNull(0);

        // then
        assertThat(subtype).isNotNull();
        assertThat(subtype.getType()).isEqualTo(String.class);
    }

    @Test
    void testGetSubtypeAtOrNull_InvalidIndex_ReturnsNull() throws Exception {
        // given
        Field field = GenericTypesConfig.class.getDeclaredField("stringList");
        GenericsDeclaration declaration = GenericsDeclaration.of(field.getGenericType());

        // when
        GenericsDeclaration subtype = declaration.getSubtypeAtOrNull(99);

        // then
        assertThat(subtype).isNull();
    }

    @Test
    void testGetSubtypeAtOrNull_NoSubtypes_ReturnsNull() {
        // given
        GenericsDeclaration declaration = GenericsDeclaration.of(String.class);

        // when
        GenericsDeclaration subtype = declaration.getSubtypeAtOrNull(0);

        // then
        assertThat(subtype).isNull();
    }

    @Test
    void testGetSubtypeAtOrThrow_ValidIndex_ReturnsSubtype() throws Exception {
        // given
        Field field = GenericTypesConfig.class.getDeclaredField("stringList");
        GenericsDeclaration declaration = GenericsDeclaration.of(field.getGenericType());

        // when
        GenericsDeclaration subtype = declaration.getSubtypeAtOrThrow(0);

        // then
        assertThat(subtype).isNotNull();
        assertThat(subtype.getType()).isEqualTo(String.class);
    }

    @Test
    void testGetSubtypeAtOrThrow_InvalidIndex_ThrowsException() throws Exception {
        // given
        Field field = GenericTypesConfig.class.getDeclaredField("stringList");
        GenericsDeclaration declaration = GenericsDeclaration.of(field.getGenericType());

        // when/then
        assertThatThrownBy(() -> declaration.getSubtypeAtOrThrow(99))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testHasSubtypes_WithGenerics_ReturnsTrue() throws Exception {
        // given
        Field field = GenericTypesConfig.class.getDeclaredField("stringList");
        GenericsDeclaration declaration = GenericsDeclaration.of(field.getGenericType());

        // then
        assertThat(declaration.hasSubtypes()).isTrue();
    }

    @Test
    void testHasSubtypes_WithoutGenerics_ReturnsFalse() {
        // given
        GenericsDeclaration declaration = GenericsDeclaration.of(String.class);

        // then
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
        // given
        GenericsDeclaration declaration = GenericsDeclaration.of(String.class);

        // when
        Class<?> wrapped = declaration.wrap();

        // then
        assertThat(wrapped).isNull();
    }

    // --- Type Matching ---

    @Test
    void testDoBoxTypesMatch_PrimitiveAndWrapper_ReturnsTrue() {
        // when/then
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
        // when/then
        assertThat(GenericsDeclaration.doBoxTypesMatch(int.class, Long.class)).isFalse();
        assertThat(GenericsDeclaration.doBoxTypesMatch(String.class, Integer.class)).isFalse();
    }

    @Test
    void testIsUnboxedCompatibleWithBoxed_MatchingPair_ReturnsTrue() {
        // when/then
        assertThat(GenericsDeclaration.isUnboxedCompatibleWithBoxed(int.class, Integer.class)).isTrue();
    }

    @Test
    void testIsUnboxedCompatibleWithBoxed_MismatchedPair_ReturnsFalse() {
        // when/then
        assertThat(GenericsDeclaration.isUnboxedCompatibleWithBoxed(int.class, Long.class)).isFalse();
    }
}
