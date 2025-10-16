package eu.okaeri.configs.annotations;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.CustomKey;
import eu.okaeri.configs.annotation.NameModifier;
import eu.okaeri.configs.annotation.NameStrategy;
import eu.okaeri.configs.annotation.Names;
import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.schema.FieldDeclaration;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for @Names annotation.
 * <p>
 * Verifies:
 * - IDENTITY strategy (no change)
 * - SNAKE_CASE strategy (camelCase → camel_Case with default NameModifier.NONE)
 * - HYPHEN_CASE strategy (camelCase → camel-Case with default NameModifier.NONE)
 * - TO_UPPER_CASE modifier (all uppercase)
 * - TO_LOWER_CASE modifier (all lowercase)
 * - Combined strategy + modifier
 * - @CustomKey overrides @Names strategy
 * - Names annotation inheritance from enclosing class
 * <p>
 * Note: @Names is deprecated. Without an explicit modifier, the regex transformations
 * preserve original capitalization (e.g., myFieldName → my_Field_Name, not my_field_name).
 * This is documented as "buggy behavior" but is the expected result of NameModifier.NONE.
 * Use TO_LOWER_CASE or TO_UPPER_CASE modifier for consistent casing.
 */
class NamesAnnotationTest {

    @Data
    @EqualsAndHashCode(callSuper = false)
    @Names(strategy = NameStrategy.IDENTITY)
    public static class IdentityConfig extends OkaeriConfig {
        private String myFieldName = "value";
        private String anotherField = "value2";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @Names(strategy = NameStrategy.SNAKE_CASE)
    public static class SnakeCaseConfig extends OkaeriConfig {
        private String myFieldName = "value";
        private String anotherField = "value2";
        private String simpleField = "value3";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @Names(strategy = NameStrategy.HYPHEN_CASE)
    public static class HyphenCaseConfig extends OkaeriConfig {
        private String myFieldName = "value";
        private String anotherField = "value2";
        private String simpleField = "value3";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @Names(strategy = NameStrategy.IDENTITY, modifier = NameModifier.TO_UPPER_CASE)
    public static class UpperCaseConfig extends OkaeriConfig {
        private String myField = "value";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @Names(strategy = NameStrategy.IDENTITY, modifier = NameModifier.TO_LOWER_CASE)
    public static class LowerCaseConfig extends OkaeriConfig {
        private String MyField = "value";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @Names(strategy = NameStrategy.SNAKE_CASE, modifier = NameModifier.TO_UPPER_CASE)
    public static class SnakeCaseUpperConfig extends OkaeriConfig {
        private String myFieldName = "value";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @Names(strategy = NameStrategy.HYPHEN_CASE, modifier = NameModifier.TO_LOWER_CASE)
    public static class HyphenCaseLowerConfig extends OkaeriConfig {
        private String myFieldName = "value";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @Names(strategy = NameStrategy.SNAKE_CASE)
    public static class CustomKeyOverrideConfig extends OkaeriConfig {
        private String normalField = "value1";

        @CustomKey("custom-key")
        private String overriddenField = "value2";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @Names(strategy = NameStrategy.HYPHEN_CASE)
    public static class OuterConfig extends OkaeriConfig {
        private String outerField = "outer";

        @Data
        @EqualsAndHashCode(callSuper = false)
        public static class InnerConfig extends OkaeriConfig {
            private String innerField = "inner";
        }
    }

    // Tests

    @Test
    void testNames_Identity_NoChange() {
        // Given
        IdentityConfig config = ConfigManager.create(IdentityConfig.class);

        // When
        ConfigDeclaration declaration = config.getDeclaration();

        // Then
        assertThat(declaration.getField("myFieldName").isPresent()).isTrue();
        assertThat(declaration.getField("anotherField").isPresent()).isTrue();
    }

    @Test
    void testNames_SnakeCase_Transforms() {
        // Given
        SnakeCaseConfig config = ConfigManager.create(SnakeCaseConfig.class);

        // When
        ConfigDeclaration declaration = config.getDeclaration();

        // Then - With NameModifier.NONE (default), capitals are preserved
        // myFieldName -> my_Field_Name (add TO_LOWER_CASE modifier for all lowercase)
        assertThat(declaration.getField("my_Field_Name").isPresent()).isTrue();
        assertThat(declaration.getField("another_Field").isPresent()).isTrue();
        assertThat(declaration.getField("simple_Field").isPresent()).isTrue();
    }

    @Test
    void testNames_HyphenCase_Transforms() {
        // Given
        HyphenCaseConfig config = ConfigManager.create(HyphenCaseConfig.class);

        // When
        ConfigDeclaration declaration = config.getDeclaration();

        // Then - With NameModifier.NONE (default), capitals are preserved
        // myFieldName -> my-Field-Name (add TO_LOWER_CASE modifier for all lowercase)
        assertThat(declaration.getField("my-Field-Name").isPresent()).isTrue();
        assertThat(declaration.getField("another-Field").isPresent()).isTrue();
        assertThat(declaration.getField("simple-Field").isPresent()).isTrue();
    }

    @Test
    void testNames_UpperCaseModifier_ConvertsToUpperCase() {
        // Given
        UpperCaseConfig config = ConfigManager.create(UpperCaseConfig.class);

        // When
        ConfigDeclaration declaration = config.getDeclaration();
        FieldDeclaration field = declaration.getField("MYFIELD").orElse(null);

        // Then
        assertThat(field).isNotNull();
        assertThat(field.getName()).isEqualTo("MYFIELD");
    }

    @Test
    void testNames_LowerCaseModifier_ConvertsToLowerCase() {
        // Given
        LowerCaseConfig config = ConfigManager.create(LowerCaseConfig.class);

        // When
        ConfigDeclaration declaration = config.getDeclaration();
        FieldDeclaration field = declaration.getField("myfield").orElse(null);

        // Then
        assertThat(field).isNotNull();
        assertThat(field.getName()).isEqualTo("myfield");
    }

    @Test
    void testNames_SnakeCaseWithUpperCase_Combined() {
        // Given
        SnakeCaseUpperConfig config = ConfigManager.create(SnakeCaseUpperConfig.class);

        // When
        ConfigDeclaration declaration = config.getDeclaration();
        FieldDeclaration field = declaration.getField("MY_FIELD_NAME").orElse(null);

        // Then
        assertThat(field).isNotNull();
        assertThat(field.getName()).isEqualTo("MY_FIELD_NAME");
    }

    @Test
    void testNames_HyphenCaseWithLowerCase_Combined() {
        // Given
        HyphenCaseLowerConfig config = ConfigManager.create(HyphenCaseLowerConfig.class);

        // When
        ConfigDeclaration declaration = config.getDeclaration();
        FieldDeclaration field = declaration.getField("my-field-name").orElse(null);

        // Then
        assertThat(field).isNotNull();
        assertThat(field.getName()).isEqualTo("my-field-name");
    }

    @Test
    void testNames_CustomKeyOverrides_NameStrategy() {
        // Given
        CustomKeyOverrideConfig config = ConfigManager.create(CustomKeyOverrideConfig.class);

        // When
        ConfigDeclaration declaration = config.getDeclaration();

        // Then
        // normalField should be transformed by SNAKE_CASE (preserves capitals with NONE modifier)
        assertThat(declaration.getField("normal_Field").isPresent()).isTrue();

        // overriddenField should use custom key, NOT snake_case
        assertThat(declaration.getField("custom-key").isPresent()).isTrue();
        assertThat(declaration.getField("overridden_Field").isPresent()).isFalse();
    }

    @Test
    void testNames_InheritedFromEnclosingClass() {
        // Given
        OuterConfig.InnerConfig config = ConfigManager.create(OuterConfig.InnerConfig.class);

        // When
        ConfigDeclaration declaration = config.getDeclaration();

        // Then - Inner class should inherit HYPHEN_CASE from outer class (preserves capitals)
        assertThat(declaration.getField("inner-Field").isPresent()).isTrue();
    }

    @Test
    void testNames_InDeclaration_CapturedCorrectly() {
        // Given
        SnakeCaseConfig config = ConfigManager.create(SnakeCaseConfig.class);

        // When
        ConfigDeclaration declaration = config.getDeclaration();

        // Then
        assertThat(declaration.getNameStrategy()).isNotNull();
        assertThat(declaration.getNameStrategy().strategy()).isEqualTo(NameStrategy.SNAKE_CASE);
        assertThat(declaration.getNameStrategy().modifier()).isEqualTo(NameModifier.NONE);
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class NoNamesConfig extends OkaeriConfig {
        private String myField = "value";
    }

    @Test
    void testNames_NoAnnotation_NullInDeclaration() {
        // Given
        NoNamesConfig config = ConfigManager.create(NoNamesConfig.class);

        // When
        ConfigDeclaration declaration = config.getDeclaration();

        // Then
        assertThat(declaration.getNameStrategy()).isNull();

        // Field should use original name
        FieldDeclaration field = declaration.getField("myField").orElse(null);
        assertThat(field).isNotNull();
    }
}
