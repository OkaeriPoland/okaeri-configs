package eu.okaeri.configs.annotations;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Include;
import eu.okaeri.configs.annotation.Includes;
import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.schema.FieldDeclaration;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for @Include and @Includes annotations.
 * 
 * Verifies:
 * - Include fields from another class
 * - Multiple @Include annotations
 * - Include doesn't override existing fields
 * - Include with same field names (first wins)
 * - Declaration contains included fields
 */
class IncludeAnnotationTest {

    // Mixin classes
    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class CommonFieldsMixin extends OkaeriConfig {
        private String commonField1 = "common1";
        private String commonField2 = "common2";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class AdditionalFieldsMixin extends OkaeriConfig {
        private String additionalField1 = "additional1";
        private int additionalField2 = 42;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class ConflictingFieldsMixin extends OkaeriConfig {
        private String conflictField = "from mixin";
        private String uniqueField = "unique";
    }

    // Test configs

    @Data
    @EqualsAndHashCode(callSuper = false)
    @Include(CommonFieldsMixin.class)
    public static class SingleIncludeConfig extends OkaeriConfig {
        private String ownField = "own";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @Includes({
        @Include(CommonFieldsMixin.class),
        @Include(AdditionalFieldsMixin.class)
    })
    public static class MultipleIncludesConfig extends OkaeriConfig {
        private String ownField = "own";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @Include(ConflictingFieldsMixin.class)
    public static class ConflictingIncludeConfig extends OkaeriConfig {
        private String conflictField = "from main";
        private String mainField = "main";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class NoIncludeConfig extends OkaeriConfig {
        private String field1 = "value1";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @Include(CommonFieldsMixin.class)
    @Include(AdditionalFieldsMixin.class)
    public static class RepeatingIncludeConfig extends OkaeriConfig {
        private String ownField = "own";
    }

    // Tests

    @Test
    void testInclude_SingleMixin_FieldsIncluded() {
        // Given
        SingleIncludeConfig config = ConfigManager.create(SingleIncludeConfig.class);

        // When
        ConfigDeclaration declaration = config.getDeclaration();

        // Then - Should have own field + included fields
        assertThat(declaration.getField("ownField").isPresent()).isTrue();
        assertThat(declaration.getField("commonField1").isPresent()).isTrue();
        assertThat(declaration.getField("commonField2").isPresent()).isTrue();
    }

    @Test
    void testInclude_MultipleIncludes_AllFieldsIncluded() {
        // Given
        MultipleIncludesConfig config = ConfigManager.create(MultipleIncludesConfig.class);

        // When
        ConfigDeclaration declaration = config.getDeclaration();

        // Then - Should have own field + all included fields
        assertThat(declaration.getField("ownField").isPresent()).isTrue();
        assertThat(declaration.getField("commonField1").isPresent()).isTrue();
        assertThat(declaration.getField("commonField2").isPresent()).isTrue();
        assertThat(declaration.getField("additionalField1").isPresent()).isTrue();
        assertThat(declaration.getField("additionalField2").isPresent()).isTrue();
    }

    @Test
    void testInclude_ConflictingFields_MainClassWins() {
        // Given
        ConflictingIncludeConfig config = ConfigManager.create(ConflictingIncludeConfig.class);

        // When
        ConfigDeclaration declaration = config.getDeclaration();
        FieldDeclaration conflictField = declaration.getField("conflictField").orElse(null);

        // Then - Main class field should take precedence
        assertThat(conflictField).isNotNull();
        // The field should belong to the main config, not the mixin
        assertThat(conflictField.getStartingValue()).isEqualTo("from main");
    }

    @Test
    void testInclude_ConflictingFields_UniqueFieldStillIncluded() {
        // Given
        ConflictingIncludeConfig config = ConfigManager.create(ConflictingIncludeConfig.class);

        // When
        ConfigDeclaration declaration = config.getDeclaration();

        // Then - Unique field from mixin should be included
        assertThat(declaration.getField("uniqueField").isPresent()).isTrue();
        assertThat(declaration.getField("mainField").isPresent()).isTrue();
    }

    @Test
    void testInclude_NoAnnotation_OnlyOwnFields() {
        // Given
        NoIncludeConfig config = ConfigManager.create(NoIncludeConfig.class);

        // When
        ConfigDeclaration declaration = config.getDeclaration();

        // Then - Should only have own field
        assertThat(declaration.getField("field1").isPresent()).isTrue();
        assertThat(declaration.getFields()).hasSize(1);
    }

    @Test
    void testInclude_RepeatingAnnotations_AllIncluded() {
        // Given
        RepeatingIncludeConfig config = ConfigManager.create(RepeatingIncludeConfig.class);

        // When
        ConfigDeclaration declaration = config.getDeclaration();

        // Then - Should have fields from both includes
        assertThat(declaration.getField("ownField").isPresent()).isTrue();
        assertThat(declaration.getField("commonField1").isPresent()).isTrue();
        assertThat(declaration.getField("commonField2").isPresent()).isTrue();
        assertThat(declaration.getField("additionalField1").isPresent()).isTrue();
        assertThat(declaration.getField("additionalField2").isPresent()).isTrue();
    }

    @Test
    void testInclude_FieldCount_Correct() {
        // Given
        MultipleIncludesConfig config = ConfigManager.create(MultipleIncludesConfig.class);

        // When
        ConfigDeclaration declaration = config.getDeclaration();
        int fieldCount = declaration.getFields().size();

        // Then - 1 own + 2 from CommonFieldsMixin + 2 from AdditionalFieldsMixin = 5
        assertThat(fieldCount).isEqualTo(5);
    }

    @Test
    void testInclude_ConflictingFieldCount_Correct() {
        // Given
        ConflictingIncludeConfig config = ConfigManager.create(ConflictingIncludeConfig.class);

        // When
        ConfigDeclaration declaration = config.getDeclaration();
        int fieldCount = declaration.getFields().size();

        // Then - 2 from main + 1 unique from mixin (conflictField is not duplicated) = 3
        assertThat(fieldCount).isEqualTo(3);
    }

    @Test
    void testInclude_IncludedFieldValues_UseDefaultsFromMixin() {
        // Given
        SingleIncludeConfig config = ConfigManager.create(SingleIncludeConfig.class);

        // When
        ConfigDeclaration declaration = config.getDeclaration();
        FieldDeclaration field1 = declaration.getField("commonField1").orElse(null);
        FieldDeclaration field2 = declaration.getField("commonField2").orElse(null);

        // Then - Values should be from the mixin's defaults
        assertThat(field1).isNotNull();
        assertThat(field1.getStartingValue()).isEqualTo("common1");
        assertThat(field2).isNotNull();
        assertThat(field2.getStartingValue()).isEqualTo("common2");
    }
}
