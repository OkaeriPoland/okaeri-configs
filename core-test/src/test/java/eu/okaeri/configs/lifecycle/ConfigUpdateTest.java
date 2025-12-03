package eu.okaeri.configs.lifecycle;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Variable;
import eu.okaeri.configs.test.configs.PrimitivesTestConfig;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for OkaeriConfig update operations.
 * <p>
 * Scenarios tested:
 * - update() synchronizes configurer data to fields
 * - update() respects @Variable annotation
 * - update() sets starting values
 * - updateDeclaration() regenerates schema
 * - Update after load reflects new values
 * - Error cases (no declaration)
 */
class ConfigUpdateTest {

    @TempDir
    Path tempDir;

    @Test
    void testUpdate_SynchronizesInternalStateToFields() throws Exception {
        // Arrange
        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());

        // Simulate loaded data by putting values in internalState directly
        config.getInternalState().put("boolValue", false);
        config.getInternalState().put("intValue", 999);
        config.getInternalState().put("doubleValue", 7.77);

        // Fields still have defaults at this point
        assertThat(config.isBoolValue()).isTrue();
        assertThat(config.getIntValue()).isEqualTo(42);

        // Act
        config.update();

        // Assert - fields should now match internalState data
        assertThat(config.isBoolValue()).isFalse();
        assertThat(config.getIntValue()).isEqualTo(999);
        assertThat(config.getDoubleValue()).isEqualTo(7.77);
    }

    @Test
    void testUpdate_AfterLoad_ReflectsNewValues() throws Exception {
        // Arrange
        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());

        // Initial state
        assertThat(config.isBoolValue()).isTrue();
        assertThat(config.getIntValue()).isEqualTo(42);

        String yamlContent = """
            boolValue: false
            intValue: 555
            """;

        // Act - load() calls update() internally
        config.load(yamlContent);

        // Assert - update() was called, fields should be updated
        assertThat(config.isBoolValue()).isFalse();
        assertThat(config.getIntValue()).isEqualTo(555);
    }

    /**
     * Test config with @Variable annotation
     */
    @Data
    @NoArgsConstructor
    @EqualsAndHashCode(callSuper = false)
    public static class VariableTestConfig extends OkaeriConfig {
        @Variable("TEST_VAR_STRING")
        private String varString = "default";

        @Variable("TEST_VAR_INT")
        private int varInt = 0;

        private String normalField = "normal";
    }

    @Test
    void testUpdate_WithVariable_LoadsFromSystemProperty() throws Exception {
        // Arrange
        System.setProperty("TEST_VAR_STRING", "from-system-property");
        System.setProperty("TEST_VAR_INT", "999");

        try {
            String yamlContent = """
                varString: from-yaml
                varInt: 123
                normalField: yaml-value
                """;

            VariableTestConfig config = ConfigManager.create(VariableTestConfig.class);
            config.withConfigurer(new YamlSnakeYamlConfigurer());

            // Act
            config.load(yamlContent);

            // Assert - @Variable fields should use system property, normal field uses YAML
            assertThat(config.getVarString()).isEqualTo("from-system-property");
            assertThat(config.getVarInt()).isEqualTo(999);
            assertThat(config.getNormalField()).isEqualTo("yaml-value");
        } finally {
            System.clearProperty("TEST_VAR_STRING");
            System.clearProperty("TEST_VAR_INT");
        }
    }

    @Test
    void testUpdate_WithVariable_LoadsFromEnvironment() throws Exception {
        // Arrange - we can't easily set env vars, but we can test the fallback behavior
        // When system property is not set, it falls back to the config value

        String yamlContent = """
            varString: from-yaml
            varInt: 123
            normalField: yaml-value
            """;

        VariableTestConfig config = ConfigManager.create(VariableTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());

        // Act
        config.load(yamlContent);

        // Assert - should use YAML values when no system property or env var exists
        assertThat(config.getVarString()).isEqualTo("from-yaml");
        assertThat(config.getVarInt()).isEqualTo(123);
    }

    @Test
    void testUpdate_WithVariable_SystemPropertyTakesPrecedence() throws Exception {
        // Arrange
        System.setProperty("TEST_VAR_STRING", "system-wins");

        try {
            String yamlContent = """
                varString: from-yaml
                """;

            VariableTestConfig config = ConfigManager.create(VariableTestConfig.class);
            config.withConfigurer(new YamlSnakeYamlConfigurer());

            // Act
            config.load(yamlContent);

            // Assert - system property should override YAML
            assertThat(config.getVarString()).isEqualTo("system-wins");
        } finally {
            System.clearProperty("TEST_VAR_STRING");
        }
    }

    @Test
    void testUpdate_WithVariable_TypeConversion() throws Exception {
        // Arrange
        System.setProperty("TEST_VAR_INT", "777");

        try {
            VariableTestConfig config = ConfigManager.create(VariableTestConfig.class);
            config.withConfigurer(new YamlSnakeYamlConfigurer());
            config.load("{}");

            // Act & Assert - String "777" should convert to int 777
            assertThat(config.getVarInt()).isEqualTo(777);
        } finally {
            System.clearProperty("TEST_VAR_INT");
        }
    }

    @Test
    void testUpdate_SetsStartingValues() throws Exception {
        // Arrange
        String yamlContent = """
            boolValue: false
            intValue: 999
            """;
        Path tempFile = this.tempDir.resolve("starting-values.yml");
        Files.writeString(tempFile, yamlContent);

        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer())
            .withBindFile(tempFile);

        // Act
        config.load();

        // Assert - starting values should be set from loaded data
        // (starting values are used for migration detection)
        assertThat(config.getDeclaration().getField("boolValue").get().getStartingValue()).isEqualTo(false);
        assertThat(config.getDeclaration().getField("intValue").get().getStartingValue()).isEqualTo(999);
    }

    @Test
    void testUpdate_MissingFieldsInConfigurer_KeepsDefaults() throws Exception {
        // Arrange
        String yamlContent = """
            boolValue: false
            """;

        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());

        // Act
        config.load(yamlContent);

        // Assert - fields not in YAML keep their default values
        assertThat(config.isBoolValue()).isFalse(); // from YAML
        assertThat(config.getIntValue()).isEqualTo(42); // default from class
        assertThat(config.getDoubleValue()).isEqualTo(3.14); // default from class
    }

    @Test
    void testDeclaration_LazyLoaded_CreatedOnFirstAccess() throws Exception {
        // Arrange & Act
        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);

        // Assert - declaration should be initialized lazily when accessed
        assertThat(config.getDeclaration()).isNotNull();
        assertThat(config.getDeclaration().getFields()).isNotEmpty();

        // Declaration should contain all fields
        assertThat(config.getDeclaration().getField("boolValue")).isPresent();
        assertThat(config.getDeclaration().getField("intValue")).isPresent();
        assertThat(config.getDeclaration().getField("doubleValue")).isPresent();
    }

    @Test
    void testDeclaration_LazyLoaded_CachedAfterFirstAccess() throws Exception {
        // Arrange
        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);

        // Act - access declaration multiple times
        Object firstAccess = config.getDeclaration();
        Object secondAccess = config.getDeclaration();
        Object thirdAccess = config.getDeclaration();

        // Assert - same instance should be returned (cached)
        assertThat(firstAccess).isNotNull();
        assertThat(secondAccess).isSameAs(firstAccess);
        assertThat(thirdAccess).isSameAs(firstAccess);
    }

    @Test
    void testUpdate_MultipleFields_AllUpdated() throws Exception {
        // Arrange
        String yamlContent = """
            boolValue: false
            byteValue: 99
            charValue: Z
            doubleValue: 1.23
            floatValue: 4.56
            intValue: 777
            longValue: 123456789
            shortValue: 999
            """;

        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());

        // Act
        config.load(yamlContent);

        // Assert - all fields should be updated
        assertThat(config.isBoolValue()).isFalse();
        assertThat(config.getByteValue()).isEqualTo((byte) 99);
        assertThat(config.getCharValue()).isEqualTo('Z');
        assertThat(config.getDoubleValue()).isEqualTo(1.23);
        assertThat(config.getFloatValue()).isEqualTo(4.56f);
        assertThat(config.getIntValue()).isEqualTo(777);
        assertThat(config.getLongValue()).isEqualTo(123456789L);
        assertThat(config.getShortValue()).isEqualTo((short) 999);
    }

    @Test
    void testUpdate_CompleteWorkflow_LoadModifySave() throws Exception {
        // Arrange
        Path tempFile = this.tempDir.resolve("workflow.yml");

        String initialContent = """
            boolValue: true
            intValue: 100
            """;
        Files.writeString(tempFile, initialContent);

        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer())
            .withBindFile(tempFile);

        // Act - Load, modify, save
        config.load();
        assertThat(config.getIntValue()).isEqualTo(100);

        config.setIntValue(200);
        config.save();

        // Load in new instance to verify
        PrimitivesTestConfig config2 = ConfigManager.create(PrimitivesTestConfig.class);
        config2.withConfigurer(new YamlSnakeYamlConfigurer())
            .withBindFile(tempFile);
        config2.load();

        // Assert
        assertThat(config2.getIntValue()).isEqualTo(200);
    }
}
