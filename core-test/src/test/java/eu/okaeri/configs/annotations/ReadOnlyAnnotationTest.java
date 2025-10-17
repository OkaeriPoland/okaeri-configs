package eu.okaeri.configs.annotations;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.ReadOnly;
import eu.okaeri.configs.configurer.InMemoryConfigurer;
import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.schema.FieldDeclaration;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for @ReadOnly annotation.
 * <p>
 * Verifies:
 * - ReadOnly field IS in declaration (unlike @Exclude)
 * - ReadOnly field CAN be loaded from input
 * - ReadOnly field IS saved to output (preserving original loaded value)
 * - ReadOnly field can be modified in code, but changes are discarded on save
 * - After save, ReadOnly field has its ORIGINAL value, not modified value
 * - Multiple read-only fields work correctly
 * - Mix of read-only and normal fields
 */
class ReadOnlyAnnotationTest {

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class SimpleReadOnlyConfig extends OkaeriConfig {
        private String normalField = "normal";

        @ReadOnly
        private String readOnlyField = "readonly";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class MultipleReadOnlyConfig extends OkaeriConfig {
        private String field1 = "value1";

        @ReadOnly
        private String readOnly1 = "readonly1";

        private String field2 = "value2";

        @ReadOnly
        private String readOnly2 = "readonly2";

        @ReadOnly
        private int readOnly3 = 999;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class AllReadOnlyConfig extends OkaeriConfig {
        @ReadOnly
        private String field1 = "value1";

        @ReadOnly
        private String field2 = "value2";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class NestedReadOnlyConfig extends OkaeriConfig {
        private String normalField = "normal";

        @ReadOnly
        private SubConfig readOnlySubConfig = new SubConfig();

        @Data
        @EqualsAndHashCode(callSuper = false)
        public static class SubConfig extends OkaeriConfig {
            private String innerField = "inner";
        }
    }

    // Tests

    @Test
    void testReadOnly_IsInDeclaration() {
        // Given
        SimpleReadOnlyConfig config = ConfigManager.create(SimpleReadOnlyConfig.class);

        // When
        ConfigDeclaration declaration = config.getDeclaration();
        FieldDeclaration readOnlyField = declaration.getField("readOnlyField").orElse(null);

        // Then - ReadOnly field SHOULD be in declaration (unlike @Exclude)
        assertThat(readOnlyField).isNotNull();
    }

    @Test
    void testReadOnly_NormalFieldInDeclaration() {
        // Given
        SimpleReadOnlyConfig config = ConfigManager.create(SimpleReadOnlyConfig.class);

        // When
        ConfigDeclaration declaration = config.getDeclaration();
        FieldDeclaration normalField = declaration.getField("normalField").orElse(null);

        // Then
        assertThat(normalField).isNotNull();
    }

    @Test
    void testReadOnly_CanAccessInCode() {
        // Given
        SimpleReadOnlyConfig config = ConfigManager.create(SimpleReadOnlyConfig.class);

        // When
        String value = config.getReadOnlyField();

        // Then
        assertThat(value).isEqualTo("readonly");
    }

    @Test
    void testReadOnly_CanModifyInCode() {
        // Given
        SimpleReadOnlyConfig config = ConfigManager.create(SimpleReadOnlyConfig.class);

        // When
        config.setReadOnlyField("modified");

        // Then
        assertThat(config.getReadOnlyField()).isEqualTo("modified");
    }

    @Test
    void testReadOnly_SavesOriginalValue() {
        // Given
        SimpleReadOnlyConfig config = ConfigManager.create(SimpleReadOnlyConfig.class);
        config.withConfigurer(new InMemoryConfigurer());

        // When - save the config
        String saved = config.saveToString();

        // Then - ReadOnly field SHOULD be saved with its original (default) value
        assertThat(config.getConfigurer().keyExists("readOnlyField")).isTrue();
        assertThat(config.getConfigurer().keyExists("normalField")).isTrue();
        assertThat(config.getConfigurer().getValue("readOnlyField")).isEqualTo("readonly");
        assertThat(config.getConfigurer().getValue("normalField")).isEqualTo("normal");
    }

    @Test
    void testReadOnly_CanBeLoadedFromInput() {
        // Given
        SimpleReadOnlyConfig config = ConfigManager.create(SimpleReadOnlyConfig.class);
        config.withConfigurer(new InMemoryConfigurer());

        // When - manually set values to simulate loading
        config.set("normalField", "loaded_normal");
        config.set("readOnlyField", "loaded_readonly");

        // Then - ReadOnly field should be loadable
        assertThat(config.getNormalField()).isEqualTo("loaded_normal");
        assertThat(config.getReadOnlyField()).isEqualTo("loaded_readonly");
    }

    @Test
    void testReadOnly_CanBeLoadedFromYamlInput() {
        // Given
        SimpleReadOnlyConfig config = ConfigManager.create(SimpleReadOnlyConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());

        // When - load from YAML string with both normalField and readOnlyField
        String yamlInput = "normalField: loaded_normal\nreadOnlyField: loaded_readonly";
        config.load(yamlInput);

        // Then - Both fields should be loaded successfully
        assertThat(config.getNormalField()).isEqualTo("loaded_normal");
        assertThat(config.getReadOnlyField()).isEqualTo("loaded_readonly");

        // When - modify the readOnlyField and save
        config.setReadOnlyField("modified_readonly");
        String savedYaml = config.saveToString();

        // Then - both fields should be in saved output
        // But readOnlyField should have the ORIGINAL loaded value, not the modified value
        assertThat(savedYaml).contains("normalField: loaded_normal");
        assertThat(savedYaml).contains("readOnlyField: loaded_readonly");
        assertThat(savedYaml).doesNotContain("modified_readonly");

        // Verify field was modified in memory but not persisted
        assertThat(config.getReadOnlyField()).isEqualTo("modified_readonly");
    }

    @Test
    void testReadOnly_ModificationsDiscarded() {
        // Given
        SimpleReadOnlyConfig config = ConfigManager.create(SimpleReadOnlyConfig.class);
        config.withConfigurer(new InMemoryConfigurer());

        // When - modify both fields and save
        config.setNormalField("modified_normal");
        config.setReadOnlyField("modified_readonly");
        String saved = config.saveToString();

        // Then - normalField saves modified value, readOnlyField saves original value
        assertThat(config.getConfigurer().keyExists("normalField")).isTrue();
        assertThat(config.getConfigurer().keyExists("readOnlyField")).isTrue();
        assertThat(config.getConfigurer().getValue("normalField")).isEqualTo("modified_normal");
        assertThat(config.getConfigurer().getValue("readOnlyField")).isEqualTo("readonly"); // Original default value

        // Fields still have modified values in memory
        assertThat(config.getNormalField()).isEqualTo("modified_normal");
        assertThat(config.getReadOnlyField()).isEqualTo("modified_readonly");
    }

    @Test
    void testReadOnly_MultipleFields_AllInDeclaration() {
        // Given
        MultipleReadOnlyConfig config = ConfigManager.create(MultipleReadOnlyConfig.class);

        // When
        ConfigDeclaration declaration = config.getDeclaration();

        // Then - All fields (including read-only) should be in declaration
        assertThat(declaration.getField("readOnly1").isPresent()).isTrue();
        assertThat(declaration.getField("readOnly2").isPresent()).isTrue();
        assertThat(declaration.getField("readOnly3").isPresent()).isTrue();
        assertThat(declaration.getField("field1").isPresent()).isTrue();
        assertThat(declaration.getField("field2").isPresent()).isTrue();
    }

    @Test
    void testReadOnly_MultipleFields_AllSaved() {
        // Given
        MultipleReadOnlyConfig config = ConfigManager.create(MultipleReadOnlyConfig.class);
        config.withConfigurer(new InMemoryConfigurer());

        // When - save config
        config.saveToString();

        // Then - ALL fields should be saved (including read-only with their original values)
        assertThat(config.getConfigurer().getAllKeys()).containsExactlyInAnyOrder(
            "field1", "field2", "readOnly1", "readOnly2", "readOnly3");
    }

    @Test
    void testReadOnly_AllFieldsReadOnly_AllSaved() {
        // Given
        AllReadOnlyConfig config = ConfigManager.create(AllReadOnlyConfig.class);
        config.withConfigurer(new InMemoryConfigurer());

        // When
        config.saveToString();

        // Then - All fields should be saved with their default values
        assertThat(config.getConfigurer().getAllKeys()).containsExactlyInAnyOrder("field1", "field2");
        assertThat(config.getConfigurer().getValue("field1")).isEqualTo("value1");
        assertThat(config.getConfigurer().getValue("field2")).isEqualTo("value2");
    }

    @Test
    void testReadOnly_AllFieldsReadOnly_StillInDeclaration() {
        // Given
        AllReadOnlyConfig config = ConfigManager.create(AllReadOnlyConfig.class);

        // When
        ConfigDeclaration declaration = config.getDeclaration();

        // Then - Fields should still be in declaration (for loading purposes)
        assertThat(declaration.getFields()).hasSize(2);
        assertThat(declaration.getField("field1").isPresent()).isTrue();
        assertThat(declaration.getField("field2").isPresent()).isTrue();
    }

    @Test
    void testReadOnly_NestedConfig_InDeclaration() {
        // Given
        NestedReadOnlyConfig config = ConfigManager.create(NestedReadOnlyConfig.class);

        // When
        ConfigDeclaration declaration = config.getDeclaration();

        // Then - ReadOnly nested config should be in declaration
        assertThat(declaration.getField("readOnlySubConfig").isPresent()).isTrue();
        assertThat(declaration.getField("normalField").isPresent()).isTrue();
    }

    @Test
    void testReadOnly_NestedConfig_SavesOriginalValue() {
        // Given
        NestedReadOnlyConfig config = ConfigManager.create(NestedReadOnlyConfig.class);
        config.withConfigurer(new InMemoryConfigurer());

        // When
        config.saveToString();

        // Then - ReadOnly nested config SHOULD be saved with its original value
        assertThat(config.getConfigurer().keyExists("readOnlySubConfig")).isTrue();
        assertThat(config.getConfigurer().keyExists("normalField")).isTrue();
    }

    @Test
    void testReadOnly_DeclarationFieldCount_AllFields() {
        // Given
        MultipleReadOnlyConfig config = ConfigManager.create(MultipleReadOnlyConfig.class);

        // When
        ConfigDeclaration declaration = config.getDeclaration();
        int fieldCount = declaration.getFields().size();

        // Then - All fields should be in declaration (5 total)
        assertThat(fieldCount).isEqualTo(5);
    }

    @Test
    void testReadOnly_SavedOutputCount_AllFields() {
        // Given
        MultipleReadOnlyConfig config = ConfigManager.create(MultipleReadOnlyConfig.class);
        config.withConfigurer(new InMemoryConfigurer());

        // When
        config.saveToString();
        int savedCount = config.getConfigurer().getAllKeys().size();

        // Then - All fields should be saved (5 total: 2 normal + 3 read-only)
        assertThat(savedCount).isEqualTo(5);
    }

    @Test
    void testReadOnly_LoadAndSaveRoundTrip() {
        // Given
        SimpleReadOnlyConfig config1 = ConfigManager.create(SimpleReadOnlyConfig.class);
        config1.withConfigurer(new InMemoryConfigurer());

        // When - modify both fields and save
        config1.setNormalField("modified_normal");
        config1.setReadOnlyField("modified_readonly");

        // Save to string
        String saved = config1.saveToString();

        // Then - both fields should be saved
        // normalField has modified value, readOnlyField has original default value
        assertThat(config1.getConfigurer().getAllKeys()).containsExactlyInAnyOrder("normalField", "readOnlyField");
        assertThat(config1.getConfigurer().getValue("normalField")).isEqualTo("modified_normal");
        assertThat(config1.getConfigurer().getValue("readOnlyField")).isEqualTo("readonly"); // Original default

        // Fields still have modified values in memory
        assertThat(config1.getNormalField()).isEqualTo("modified_normal");
        assertThat(config1.getReadOnlyField()).isEqualTo("modified_readonly");
    }
}
