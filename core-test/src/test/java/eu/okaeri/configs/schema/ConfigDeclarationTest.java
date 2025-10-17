package eu.okaeri.configs.schema;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for ConfigDeclaration - focuses on declaration API and field collection.
 * Annotation-specific behavior is tested in annotation test classes.
 */
class ConfigDeclarationTest {

    // === Test Configs ===

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class SimpleConfig extends OkaeriConfig {
        private String field1 = "value1";
        private int field2 = 42;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @Header("Test Header")
    public static class ConfigWithHeader extends OkaeriConfig {
        private String field = "value";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @Names(strategy = NameStrategy.HYPHEN_CASE, modifier = NameModifier.TO_LOWER_CASE)
    public static class ConfigWithNames extends OkaeriConfig {
        private String myFieldName = "value";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class ConfigWithExcluded extends OkaeriConfig {
        private String normalField = "normal";

        @Exclude
        private String excludedField = "excluded";

        private transient String transientField = "transient";
    }

    public static class NonConfigClass {
        private String field = "value";
    }

    // === Tests ===

    @Test
    void testOf_WithClass_CreatesDeclaration() {
        // When
        ConfigDeclaration declaration = ConfigDeclaration.of(SimpleConfig.class);

        // Then
        assertThat(declaration).isNotNull();
        assertThat(declaration.getType()).isEqualTo(SimpleConfig.class);
        assertThat(declaration.isReal()).isTrue();
    }

    @Test
    void testOf_WithClassAndInstance_CreatesDeclaration() {
        // Given
        SimpleConfig config = new SimpleConfig();

        // When
        ConfigDeclaration declaration = ConfigDeclaration.of(SimpleConfig.class, config);

        // Then
        assertThat(declaration).isNotNull();
        assertThat(declaration.getType()).isEqualTo(SimpleConfig.class);
    }

    @Test
    void testOf_WithConfigInstance_CreatesDeclaration() {
        // Given
        SimpleConfig config = new SimpleConfig();

        // When
        ConfigDeclaration declaration = ConfigDeclaration.of(config);

        // Then
        assertThat(declaration).isNotNull();
        assertThat(declaration.getType()).isEqualTo(SimpleConfig.class);
    }

    @Test
    void testOf_WithObjectInstance_CreatesDeclaration() {
        // Given
        Object config = new SimpleConfig();

        // When
        ConfigDeclaration declaration = ConfigDeclaration.of(config);

        // Then
        assertThat(declaration).isNotNull();
        assertThat(declaration.getType()).isEqualTo(SimpleConfig.class);
    }

    @Test
    void testCaching_MultipleCallsSameClass_SharesTemplate() {
        // When
        ConfigDeclaration declaration1 = ConfigDeclaration.of(SimpleConfig.class);
        ConfigDeclaration declaration2 = ConfigDeclaration.of(SimpleConfig.class);

        // Then - not same instance (new declaration each time), but template data matches
        assertThat(declaration1).isNotSameAs(declaration2);
        assertThat(declaration1.getType()).isEqualTo(declaration2.getType());
        assertThat(declaration1.getHeader()).isEqualTo(declaration2.getHeader());
        assertThat(declaration1.getNameStrategy()).isEqualTo(declaration2.getNameStrategy());
    }

    @Test
    void testHeader_WithHeaderAnnotation_Captured() {
        // When
        ConfigDeclaration declaration = ConfigDeclaration.of(ConfigWithHeader.class);

        // Then - just verify header is captured (detailed behavior in HeaderAnnotationTest)
        assertThat(declaration.getHeader()).isNotNull();
    }

    @Test
    void testHeader_NoHeaderAnnotation_ReturnsNull() {
        // When
        ConfigDeclaration declaration = ConfigDeclaration.of(SimpleConfig.class);

        // Then
        assertThat(declaration.getHeader()).isNull();
    }

    @Test
    void testNameStrategy_WithNamesAnnotation_Captured() {
        // When
        ConfigDeclaration declaration = ConfigDeclaration.of(ConfigWithNames.class);

        // Then - just verify name strategy is captured (detailed behavior in NamesAnnotationTest)
        assertThat(declaration.getNameStrategy()).isNotNull();
    }

    @Test
    void testNameStrategy_NoNamesAnnotation_ReturnsNull() {
        // When
        ConfigDeclaration declaration = ConfigDeclaration.of(SimpleConfig.class);

        // Then
        assertThat(declaration.getNameStrategy()).isNull();
    }

    @Test
    void testFieldMap_SimpleConfig_ContainsAllFields() {
        // When
        ConfigDeclaration declaration = ConfigDeclaration.of(SimpleConfig.class);

        // Then
        assertThat(declaration.getFieldMap()).isNotNull();
        assertThat(declaration.getFieldMap()).hasSize(2);
        assertThat(declaration.getFieldMap()).containsKeys("field1", "field2");
    }

    @Test
    void testFieldMap_ExcludesTransientAndExcluded() {
        // When
        ConfigDeclaration declaration = ConfigDeclaration.of(ConfigWithExcluded.class);

        // Then
        assertThat(declaration.getFieldMap()).hasSize(1);
        assertThat(declaration.getFieldMap()).containsOnlyKeys("normalField");
    }

    @Test
    void testGetField_ExistingKey_ReturnsOptionalWithValue() {
        // Given
        ConfigDeclaration declaration = ConfigDeclaration.of(SimpleConfig.class);

        // When
        Optional<FieldDeclaration> field = declaration.getField("field1");

        // Then
        assertThat(field).isPresent();
        assertThat(field.get().getName()).isEqualTo("field1");
    }

    @Test
    void testGetField_NonExistingKey_ReturnsEmptyOptional() {
        // Given
        ConfigDeclaration declaration = ConfigDeclaration.of(SimpleConfig.class);

        // When
        Optional<FieldDeclaration> field = declaration.getField("nonexistent");

        // Then
        assertThat(field).isEmpty();
    }

    @Test
    void testGetGenericsOrNull_ExistingKey_ReturnsGenericsDeclaration() {
        // Given
        ConfigDeclaration declaration = ConfigDeclaration.of(SimpleConfig.class);

        // When
        GenericsDeclaration generics = declaration.getGenericsOrNull("field2");

        // Then
        assertThat(generics).isNotNull();
        assertThat(generics.getType()).isEqualTo(int.class);
    }

    @Test
    void testGetGenericsOrNull_NonExistingKey_ReturnsNull() {
        // Given
        ConfigDeclaration declaration = ConfigDeclaration.of(SimpleConfig.class);

        // When
        GenericsDeclaration generics = declaration.getGenericsOrNull("nonexistent");

        // Then
        assertThat(generics).isNull();
    }

    @Test
    void testGetFields_ReturnsAllFieldDeclarations() {
        // Given
        ConfigDeclaration declaration = ConfigDeclaration.of(SimpleConfig.class);

        // When
        Collection<FieldDeclaration> fields = declaration.getFields();

        // Then
        assertThat(fields).hasSize(2);
        assertThat(fields).extracting(FieldDeclaration::getName)
            .containsExactlyInAnyOrder("field1", "field2");
    }

    @Test
    void testIsReal_OkaeriConfigSubclass_ReturnsTrue() {
        // When
        ConfigDeclaration declaration = ConfigDeclaration.of(SimpleConfig.class);

        // Then
        assertThat(declaration.isReal()).isTrue();
    }

    @Test
    void testIsReal_NonOkaeriConfigClass_ReturnsFalse() {
        // When
        ConfigDeclaration declaration = ConfigDeclaration.of(NonConfigClass.class);

        // Then
        assertThat(declaration.isReal()).isFalse();
    }

    @Test
    void testFieldMap_PreservesOrderOfDeclaration() {
        // Given
        @Data
        @EqualsAndHashCode(callSuper = false)
        class OrderedConfig extends OkaeriConfig {
            private String first = "1";
            private String second = "2";
            private String third = "3";
        }

        // When
        ConfigDeclaration declaration = ConfigDeclaration.of(OrderedConfig.class);

        // Then
        assertThat(declaration.getFieldMap().keySet())
            .containsExactly("first", "second", "third");
    }

    @Test
    void testFieldDeclaration_WithInstance_CapturesStartingValues() {
        // Given
        SimpleConfig config = new SimpleConfig();
        config.setField1("custom");
        config.setField2(999);

        // When
        ConfigDeclaration declaration = ConfigDeclaration.of(config);
        FieldDeclaration field1 = declaration.getField("field1").get();
        FieldDeclaration field2 = declaration.getField("field2").get();

        // Then
        assertThat(field1.getStartingValue()).isEqualTo("custom");
        assertThat(field2.getStartingValue()).isEqualTo(999);
    }

    @Test
    void testFieldDeclaration_WithoutInstance_StartingValuesNull() {
        // When
        ConfigDeclaration declaration = ConfigDeclaration.of(SimpleConfig.class);
        FieldDeclaration field1 = declaration.getField("field1").get();

        // Then
        assertThat(field1.getStartingValue()).isNull();
    }
}
