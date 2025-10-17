package eu.okaeri.configs.annotations;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Exclude;
import eu.okaeri.configs.configurer.InMemoryConfigurer;
import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.schema.FieldDeclaration;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for @Exclude annotation.
 * <p>
 * Verifies:
 * - Excluded field is not in declaration
 * - Excluded field is not saved
 * - Excluded field is not loaded
 * - Excluded field can still be accessed in code
 * - Multiple excluded fields
 */
class ExcludeAnnotationTest {

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class SimpleExcludeConfig extends OkaeriConfig {
        private String normalField = "normal";

        @Exclude
        private String excludedField = "excluded";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class MultipleExcludeConfig extends OkaeriConfig {
        private String field1 = "value1";

        @Exclude
        private String excluded1 = "excluded1";

        private String field2 = "value2";

        @Exclude
        private String excluded2 = "excluded2";

        @Exclude
        private int excluded3 = 999;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class AllExcludedConfig extends OkaeriConfig {
        @Exclude
        private String field1 = "value1";

        @Exclude
        private String field2 = "value2";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class NestedExcludeConfig extends OkaeriConfig {
        private String normalField = "normal";

        @Exclude
        private SubConfig excludedSubConfig = new SubConfig();

        @Data
        @EqualsAndHashCode(callSuper = false)
        public static class SubConfig extends OkaeriConfig {
            private String innerField = "inner";
        }
    }

    // Tests

    @Test
    void testExclude_NotInDeclaration() {
        // Given
        SimpleExcludeConfig config = ConfigManager.create(SimpleExcludeConfig.class);

        // When
        ConfigDeclaration declaration = config.getDeclaration();
        FieldDeclaration excludedField = declaration.getField("excludedField").orElse(null);

        // Then
        assertThat(excludedField).isNull();
    }

    @Test
    void testExclude_NormalFieldInDeclaration() {
        // Given
        SimpleExcludeConfig config = ConfigManager.create(SimpleExcludeConfig.class);

        // When
        ConfigDeclaration declaration = config.getDeclaration();
        FieldDeclaration normalField = declaration.getField("normalField").orElse(null);

        // Then
        assertThat(normalField).isNotNull();
    }

    @Test
    void testExclude_CanStillAccessInCode() {
        // Given
        SimpleExcludeConfig config = ConfigManager.create(SimpleExcludeConfig.class);

        // When
        String value = config.getExcludedField();

        // Then
        assertThat(value).isEqualTo("excluded");
    }

    @Test
    void testExclude_CanStillModifyInCode() {
        // Given
        SimpleExcludeConfig config = ConfigManager.create(SimpleExcludeConfig.class);

        // When
        config.setExcludedField("modified");

        // Then
        assertThat(config.getExcludedField()).isEqualTo("modified");
    }

    @Test
    void testExclude_NotInConfigurer() {
        // Given
        SimpleExcludeConfig config = ConfigManager.create(SimpleExcludeConfig.class);
        config.withConfigurer(new InMemoryConfigurer());

        // When - Trigger setValue for all declared fields
        ConfigDeclaration declaration = config.getDeclaration();
        for (FieldDeclaration field : declaration.getFields()) {
            config.set(field.getName(), field.getValue());
        }

        // Then - Excluded field should not be in configurer
        assertThat(config.getConfigurer().getAllKeys()).doesNotContain("excludedField");
        assertThat(config.getConfigurer().getAllKeys()).contains("normalField");
    }

    @Test
    void testExclude_MultipleExcluded_NoneInDeclaration() {
        // Given
        MultipleExcludeConfig config = ConfigManager.create(MultipleExcludeConfig.class);

        // When
        ConfigDeclaration declaration = config.getDeclaration();

        // Then
        assertThat(declaration.getField("excluded1").isPresent()).isFalse();
        assertThat(declaration.getField("excluded2").isPresent()).isFalse();
        assertThat(declaration.getField("excluded3").isPresent()).isFalse();
        assertThat(declaration.getField("field1").isPresent()).isTrue();
        assertThat(declaration.getField("field2").isPresent()).isTrue();
    }

    @Test
    void testExclude_AllFieldsExcluded_EmptyDeclaration() {
        // Given
        AllExcludedConfig config = ConfigManager.create(AllExcludedConfig.class);

        // When
        ConfigDeclaration declaration = config.getDeclaration();

        // Then
        assertThat(declaration.getFields()).isEmpty();
    }

    @Test
    void testExclude_NestedConfig_ExcludedFromDeclaration() {
        // Given
        NestedExcludeConfig config = ConfigManager.create(NestedExcludeConfig.class);

        // When
        ConfigDeclaration declaration = config.getDeclaration();

        // Then
        assertThat(declaration.getField("excludedSubConfig").isPresent()).isFalse();
        assertThat(declaration.getField("normalField").isPresent()).isTrue();
    }

    @Test
    void testExclude_DeclarationFieldCount_OnlyNonExcluded() {
        // Given
        MultipleExcludeConfig config = ConfigManager.create(MultipleExcludeConfig.class);

        // When
        ConfigDeclaration declaration = config.getDeclaration();
        int fieldCount = declaration.getFields().size();

        // Then - Only field1 and field2 should be in declaration
        assertThat(fieldCount).isEqualTo(2);
    }

    @Test
    void testExclude_GetAllKeys_OnlyNonExcluded() {
        // Given
        MultipleExcludeConfig config = ConfigManager.create(MultipleExcludeConfig.class);
        config.withConfigurer(new InMemoryConfigurer());

        // When - Trigger setValue for all declared fields
        ConfigDeclaration declaration = config.getDeclaration();
        for (FieldDeclaration field : declaration.getFields()) {
            config.set(field.getName(), field.getValue());
        }

        // Then
        assertThat(config.getConfigurer().getAllKeys()).containsExactlyInAnyOrder("field1", "field2");
    }
}
