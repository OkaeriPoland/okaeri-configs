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
 * - Include fields from parent/base classes
 * - Multiple @Include annotations
 * - Include doesn't override existing fields
 * - Include with same field names (child wins)
 * - Declaration contains included fields
 * 
 * Note: @Include is used when the library doesn't automatically scan
 * parent classes, so you explicitly include them.
 */
class IncludeAnnotationTest {

    // ===== Base Classes =====
    
    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class BaseConfig extends OkaeriConfig {
        private String baseField1 = "base1";
        private String baseField2 = "base2";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class AnotherBase extends OkaeriConfig {
        private String anotherField1 = "another1";
        private int anotherField2 = 42;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class ConflictingBase extends OkaeriConfig {
        private String conflictField = "from base";
        private String uniqueField = "unique";
    }

    // ===== Test Configs - Single Include =====

    @Data
    @EqualsAndHashCode(callSuper = false)
    @Include(BaseConfig.class)
    public static class SingleIncludeConfig extends BaseConfig {
        private String ownField = "own";
    }

    // ===== Test Configs - Invalid Usage (Include without extends) =====

    @Data
    @EqualsAndHashCode(callSuper = false)
    @Includes({
        @Include(BaseConfig.class),
        @Include(AnotherBase.class)  // INVALID: Not extended!
    })
    public static class InvalidMultipleIncludesConfig extends BaseConfig {
        private String ownField = "own";
    }

    // ===== Test Configs - Conflicting Fields =====

    @Data
    @EqualsAndHashCode(callSuper = false)
    @Include(ConflictingBase.class)
    public static class ConflictingIncludeConfig extends ConflictingBase {
        private String conflictField = "from child";
        private String childField = "child";
    }

    // ===== Test Configs - No Include =====

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class NoIncludeConfig extends OkaeriConfig {
        private String field1 = "value1";
    }

    // Tests

    @Test
    void testInclude_SingleMixin_FieldsIncluded() {
        // Given
        SingleIncludeConfig config = ConfigManager.create(SingleIncludeConfig.class);

        // When
        ConfigDeclaration declaration = config.getDeclaration();

        // Then - Should have own field + fields from base class
        assertThat(declaration.getField("ownField").isPresent()).isTrue();
        assertThat(declaration.getField("baseField1").isPresent()).isTrue();
        assertThat(declaration.getField("baseField2").isPresent()).isTrue();
    }


    @Test
    void testInclude_ConflictingFields_ChildClassWins() {
        // Given
        ConflictingIncludeConfig config = ConfigManager.create(ConflictingIncludeConfig.class);

        // When
        ConfigDeclaration declaration = config.getDeclaration();
        FieldDeclaration conflictField = declaration.getField("conflictField").orElse(null);

        // Then - Child class field should take precedence over parent
        assertThat(conflictField).isNotNull();
        assertThat(conflictField.getStartingValue()).isEqualTo("from child");
    }

    @Test
    void testInclude_ConflictingFields_UniqueFieldStillIncluded() {
        // Given
        ConflictingIncludeConfig config = ConfigManager.create(ConflictingIncludeConfig.class);

        // When
        ConfigDeclaration declaration = config.getDeclaration();

        // Then - Unique field from parent should be included
        assertThat(declaration.getField("uniqueField").isPresent()).isTrue();
        assertThat(declaration.getField("childField").isPresent()).isTrue();
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
    void testInclude_InvalidUsage_IncludeWithoutExtends_ThrowsException() {
        // Given / When / Then - Trying to @Include a class you don't extend should fail
        // The library tries to read AnotherBase fields from the config instance, which fails
        assertThat(org.assertj.core.api.Assertions.catchThrowable(() -> {
            ConfigManager.create(InvalidMultipleIncludesConfig.class);
        }))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Can not get");
    }

    @Test
    void testInclude_ConflictingFieldCount_Correct() {
        // Given
        ConflictingIncludeConfig config = ConfigManager.create(ConflictingIncludeConfig.class);

        // When
        ConfigDeclaration declaration = config.getDeclaration();
        int fieldCount = declaration.getFields().size();

        // Then - 2 from child + 1 unique from parent (conflictField is not duplicated) = 3
        assertThat(fieldCount).isEqualTo(3);
    }

    @Test
    void testInclude_IncludedFieldValues_UseDefaultsFromBase() {
        // Given
        SingleIncludeConfig config = ConfigManager.create(SingleIncludeConfig.class);

        // When
        ConfigDeclaration declaration = config.getDeclaration();
        FieldDeclaration field1 = declaration.getField("baseField1").orElse(null);
        FieldDeclaration field2 = declaration.getField("baseField2").orElse(null);

        // Then - Values should be from the base class defaults
        assertThat(field1).isNotNull();
        assertThat(field1.getStartingValue()).isEqualTo("base1");
        assertThat(field2).isNotNull();
        assertThat(field2.getStartingValue()).isEqualTo("base2");
    }
}
