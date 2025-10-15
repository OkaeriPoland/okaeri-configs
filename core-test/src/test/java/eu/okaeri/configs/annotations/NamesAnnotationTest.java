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
 * 
 * Verifies:
 * - IDENTITY strategy (no change)
 * - SNAKE_CASE strategy (camelCase → camel_case)
 * - HYPHEN_CASE strategy (camelCase → camel-case)
 * - TO_UPPER_CASE modifier
 * - TO_LOWER_CASE modifier
 * - Combined strategy + modifier
 * - @CustomKey overrides @Names strategy
 * - Names annotation inheritance from enclosing class
 * 
 * Note: @Names is deprecated and has known bugs (see annotation docs).
 * These tests verify the actual (buggy) behavior, not ideal behavior.
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

        // Then
        // Note: Testing actual (buggy) behavior
        FieldDeclaration field1 = declaration.getField("my_field_name").orElse(null);
        FieldDeclaration field2 = declaration.getField("another_field").orElse(null);
        FieldDeclaration field3 = declaration.getField("simple_field").orElse(null);
        
        assertThat(field1).isNotNull();
        assertThat(field2).isNotNull();
        assertThat(field3).isNotNull();
    }

    @Test
    void testNames_HyphenCase_Transforms() {
        // Given
        HyphenCaseConfig config = ConfigManager.create(HyphenCaseConfig.class);

        // When
        ConfigDeclaration declaration = config.getDeclaration();

        // Then
        // Note: Testing actual (buggy) behavior
        FieldDeclaration field1 = declaration.getField("my-field-name").orElse(null);
        FieldDeclaration field2 = declaration.getField("another-field").orElse(null);
        FieldDeclaration field3 = declaration.getField("simple-field").orElse(null);
        
        assertThat(field1).isNotNull();
        assertThat(field2).isNotNull();
        assertThat(field3).isNotNull();
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
        // normalField should be transformed by SNAKE_CASE
        FieldDeclaration normalField = declaration.getField("normal_field").orElse(null);
        assertThat(normalField).isNotNull();

        // overriddenField should use custom key, NOT snake_case
        FieldDeclaration overriddenField = declaration.getField("custom-key").orElse(null);
        assertThat(overriddenField).isNotNull();
        assertThat(declaration.getField("overridden_field").isPresent()).isFalse();
    }

    @Test
    void testNames_InheritedFromEnclosingClass() {
        // Given
        OuterConfig.InnerConfig config = ConfigManager.create(OuterConfig.InnerConfig.class);

        // When
        ConfigDeclaration declaration = config.getDeclaration();
        FieldDeclaration field = declaration.getField("inner-field").orElse(null);

        // Then - Inner class should inherit HYPHEN_CASE from outer class
        assertThat(field).isNotNull();
        assertThat(field.getName()).isEqualTo("inner-field");
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

    @Test
    void testNames_NoAnnotation_NullInDeclaration() {
        // Given
        @Data
        @EqualsAndHashCode(callSuper = false)
        class NoNamesConfig extends OkaeriConfig {
            private String myField = "value";
        }

        // When
        NoNamesConfig config = ConfigManager.create(NoNamesConfig.class);
        ConfigDeclaration declaration = config.getDeclaration();

        // Then
        assertThat(declaration.getNameStrategy()).isNull();
        
        // Field should use original name
        FieldDeclaration field = declaration.getField("myField").orElse(null);
        assertThat(field).isNotNull();
    }
}
