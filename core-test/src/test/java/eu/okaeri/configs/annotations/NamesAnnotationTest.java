package eu.okaeri.configs.annotations;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.CustomKey;
import eu.okaeri.configs.annotation.NameModifier;
import eu.okaeri.configs.annotation.NameStrategy;
import eu.okaeri.configs.annotation.Names;
import eu.okaeri.configs.configurer.InMemoryConfigurer;
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
        InMemoryConfigurer configurer = new InMemoryConfigurer();
        config.withConfigurer(configurer);
        config.update();

        // When - Check configurer has transformed keys (not declaration)
        // Declaration always uses field names, @Names affects configurer keys
        
        // Then - Keys in configurer should be transformed
        assertThat(configurer.keyExists("my_field_name")).isTrue();
        assertThat(configurer.keyExists("another_field")).isTrue();
        assertThat(configurer.keyExists("simple_field")).isTrue();
    }

    @Test
    void testNames_HyphenCase_Transforms() {
        // Given
        HyphenCaseConfig config = ConfigManager.create(HyphenCaseConfig.class);
        InMemoryConfigurer configurer = new InMemoryConfigurer();
        config.withConfigurer(configurer);
        config.update();

        // When - Check configurer has transformed keys (not declaration)
        
        // Then - Keys in configurer should be transformed
        assertThat(configurer.keyExists("my-field-name")).isTrue();
        assertThat(configurer.keyExists("another-field")).isTrue();
        assertThat(configurer.keyExists("simple-field")).isTrue();
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
        InMemoryConfigurer configurer = new InMemoryConfigurer();
        config.withConfigurer(configurer);
        config.update();

        // When - Check configurer keys
        
        // Then
        // normalField should be transformed by SNAKE_CASE in configurer
        assertThat(configurer.keyExists("normal_field")).isTrue();

        // overriddenField should use custom key, NOT snake_case
        assertThat(configurer.keyExists("custom-key")).isTrue();
        assertThat(configurer.keyExists("overridden_field")).isFalse();
    }

    @Test
    void testNames_InheritedFromEnclosingClass() {
        // Given
        OuterConfig.InnerConfig config = ConfigManager.create(OuterConfig.InnerConfig.class);
        InMemoryConfigurer configurer = new InMemoryConfigurer();
        config.withConfigurer(configurer);
        config.update();

        // When - Check configurer keys
        
        // Then - Inner class should inherit HYPHEN_CASE from outer class
        assertThat(configurer.keyExists("inner-field")).isTrue();
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
