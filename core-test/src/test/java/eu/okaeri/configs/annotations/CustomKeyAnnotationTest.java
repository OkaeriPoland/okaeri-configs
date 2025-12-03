package eu.okaeri.configs.annotations;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.CustomKey;
import eu.okaeri.configs.configurer.InMemoryConfigurer;
import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.schema.FieldDeclaration;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for @CustomKey annotation.
 * <p>
 * Verifies:
 * - Field with custom key name
 * - Custom key is used in serialization
 * - Load using custom key
 * - Get/set using custom key
 * - Empty value uses field name
 * - Custom key in nested config
 */
class CustomKeyAnnotationTest {

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class SimpleCustomKeyConfig extends OkaeriConfig {
        @CustomKey("custom-field-name")
        private String myField = "value1";

        private String normalField = "value2";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class MultipleCustomKeysConfig extends OkaeriConfig {
        @CustomKey("first-custom")
        private String field1 = "value1";

        @CustomKey("second-custom")
        private String field2 = "value2";

        @CustomKey("third-custom")
        private int field3 = 42;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class EmptyCustomKeyConfig extends OkaeriConfig {
        @CustomKey("")
        private String field = "value";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class NestedCustomKeyConfig extends OkaeriConfig {
        @CustomKey("outer-field")
        private String outerField = "outer";

        @CustomKey("nested-config")
        private SubConfig subConfig = new SubConfig();

        @Data
        @EqualsAndHashCode(callSuper = false)
        public static class SubConfig extends OkaeriConfig {
            @CustomKey("inner-field")
            private String innerField = "inner";
        }
    }

    // Tests

    @Test
    void testCustomKey_SimpleKey_InDeclaration() {
        // Given
        SimpleCustomKeyConfig config = ConfigManager.create(SimpleCustomKeyConfig.class);

        // When
        ConfigDeclaration declaration = config.getDeclaration();
        FieldDeclaration field = declaration.getField("custom-field-name").orElse(null);

        // Then
        assertThat(field).isNotNull();
        assertThat(field.getName()).isEqualTo("custom-field-name");
    }

    @Test
    void testCustomKey_OriginalFieldName_NotInDeclaration() {
        // Given
        SimpleCustomKeyConfig config = ConfigManager.create(SimpleCustomKeyConfig.class);

        // When
        ConfigDeclaration declaration = config.getDeclaration();
        FieldDeclaration field = declaration.getField("myField").orElse(null);

        // Then
        assertThat(field).isNull();
    }

    @Test
    void testCustomKey_SetValue_UsesCustomKey() {
        // Given
        SimpleCustomKeyConfig config = ConfigManager.create(SimpleCustomKeyConfig.class);
        config.withConfigurer(new InMemoryConfigurer());

        // When
        config.set("custom-field-name", "new value");

        // Then
        assertThat(config.getMyField()).isEqualTo("new value");
    }

    @Test
    void testCustomKey_GetValue_UsesCustomKey() {
        // Given
        SimpleCustomKeyConfig config = ConfigManager.create(SimpleCustomKeyConfig.class);
        config.withConfigurer(new InMemoryConfigurer());
        config.setMyField("test value");

        // When
        Object value = config.get("custom-field-name");

        // Then
        assertThat(value).isEqualTo("test value");
    }

    @Test
    void testCustomKey_MultipleKeys_AllWork() {
        // Given
        MultipleCustomKeysConfig config = ConfigManager.create(MultipleCustomKeysConfig.class);
        config.withConfigurer(new InMemoryConfigurer());

        // When
        config.set("first-custom", "updated1");
        config.set("second-custom", "updated2");
        config.set("third-custom", 999);

        // Then
        assertThat(config.getField1()).isEqualTo("updated1");
        assertThat(config.getField2()).isEqualTo("updated2");
        assertThat(config.getField3()).isEqualTo(999);
    }

    @Test
    void testCustomKey_EmptyValue_FallsBackToFieldName() {
        // Given
        EmptyCustomKeyConfig config = ConfigManager.create(EmptyCustomKeyConfig.class);

        // When
        ConfigDeclaration declaration = config.getDeclaration();
        FieldDeclaration field = declaration.getField("field").orElse(null);

        // Then
        assertThat(field).isNotNull();
        assertThat(field.getName()).isEqualTo("field");
    }

    @Test
    void testCustomKey_NormalField_UsesFieldName() {
        // Given
        SimpleCustomKeyConfig config = ConfigManager.create(SimpleCustomKeyConfig.class);

        // When
        ConfigDeclaration declaration = config.getDeclaration();
        FieldDeclaration field = declaration.getField("normalField").orElse(null);

        // Then
        assertThat(field).isNotNull();
        assertThat(field.getName()).isEqualTo("normalField");
    }

    @Test
    void testCustomKey_NestedConfig_BothLevelsWork() {
        // Given
        NestedCustomKeyConfig config = ConfigManager.create(NestedCustomKeyConfig.class);

        // When
        ConfigDeclaration declaration = config.getDeclaration();
        FieldDeclaration outerField = declaration.getField("outer-field").orElse(null);
        FieldDeclaration nestedField = declaration.getField("nested-config").orElse(null);

        // Then - Outer level
        assertThat(outerField).isNotNull();
        assertThat(outerField.getName()).isEqualTo("outer-field");
        assertThat(nestedField).isNotNull();
        assertThat(nestedField.getName()).isEqualTo("nested-config");

        // Then - Nested level
        ConfigDeclaration nestedDeclaration = config.getSubConfig().getDeclaration();
        FieldDeclaration innerField = nestedDeclaration.getField("inner-field").orElse(null);
        assertThat(innerField).isNotNull();
        assertThat(innerField.getName()).isEqualTo("inner-field");
    }

    @Test
    void testCustomKey_GetAllKeys_ReturnsCustomKeys() {
        // Given
        MultipleCustomKeysConfig config = ConfigManager.create(MultipleCustomKeysConfig.class);
        config.withConfigurer(new InMemoryConfigurer());

        // When - Trigger setValue for all fields
        config.set("first-custom", config.getField1());
        config.set("second-custom", config.getField2());
        config.set("third-custom", config.getField3());

        // Then
        assertThat(config.getInternalState().keySet()).containsExactlyInAnyOrder(
            "first-custom",
            "second-custom",
            "third-custom"
        );
    }
}
