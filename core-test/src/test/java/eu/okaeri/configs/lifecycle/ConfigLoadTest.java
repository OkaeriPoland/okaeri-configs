package eu.okaeri.configs.lifecycle;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.exception.OkaeriException;
import eu.okaeri.configs.test.TestUtils;
import eu.okaeri.configs.test.configs.PrimitivesTestConfig;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for OkaeriConfig load operations.
 * <p>
 * Scenarios tested:
 * - Load from file (File, Path, String pathname)
 * - Load from InputStream
 * - Load from String
 * - Load from Map
 * - Load from another OkaeriConfig
 * - Load with update (save afterwards)
 * - Load without update
 * - Load updates field values correctly
 * - Load handles missing fields (keeps defaults)
 * - Load handles extra fields (orphans)
 * - Error cases (no configurer, non-existent file, malformed data)
 */
class ConfigLoadTest {

    @Test
    void testLoad_FromFile_LoadsSuccessfully() throws Exception {
        // Arrange
        String yamlContent = """
            boolValue: false
            intValue: 999
            doubleValue: 3.14
            """;
        File tempFile = TestUtils.createTempFile(yamlContent, ".yml");

        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());

        // Act
        config.load(tempFile);

        // Assert
        assertThat(config.isBoolValue()).isFalse();
        assertThat(config.getIntValue()).isEqualTo(999);
        assertThat(config.getDoubleValue()).isEqualTo(3.14);
    }

    @Test
    void testLoad_FromPath_LoadsSuccessfully() throws Exception {
        // Arrange
        String yamlContent = """
            byteValue: 42
            shortValue: 1234
            longValue: 9876543210
            """;
        Path tempDir = TestUtils.createTempTestDir();
        Path tempFile = tempDir.resolve("test-config.yml");
        Files.writeString(tempFile, yamlContent);

        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());

        // Act
        config.load(tempFile);

        // Assert
        assertThat(config.getByteValue()).isEqualTo((byte) 42);
        assertThat(config.getShortValue()).isEqualTo((short) 1234);
        assertThat(config.getLongValue()).isEqualTo(9876543210L);
    }

    @Test
    void testLoad_FromBindFile_LoadsSuccessfully() throws Exception {
        // Arrange
        String yamlContent = """
            floatValue: 2.71
            charValue: X
            """;
        Path tempDir = TestUtils.createTempTestDir();
        Path tempFile = tempDir.resolve("bound-config.yml");
        Files.writeString(tempFile, yamlContent);

        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer())
            .withBindFile(tempFile);

        // Act
        config.load();

        // Assert
        assertThat(config.getFloatValue()).isEqualTo(2.71f);
        assertThat(config.getCharValue()).isEqualTo('X');
    }

    @Test
    void testLoad_FromInputStream_LoadsSuccessfully() throws Exception {
        // Arrange
        String yamlContent = """
            intValue: 555
            boolValue: false
            """;
        ByteArrayInputStream inputStream = new ByteArrayInputStream(yamlContent.getBytes());

        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());

        // Act
        config.load(inputStream);

        // Assert
        assertThat(config.getIntValue()).isEqualTo(555);
        assertThat(config.isBoolValue()).isFalse();
    }

    @Test
    void testLoad_FromString_LoadsSuccessfully() throws Exception {
        // Arrange
        String yamlContent = """
            boolValue: false
            intValue: 777
            doubleValue: 9.87654
            """;

        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());

        // Act
        config.load(yamlContent);

        // Assert
        assertThat(config.isBoolValue()).isFalse();
        assertThat(config.getIntValue()).isEqualTo(777);
        assertThat(config.getDoubleValue()).isEqualTo(9.87654);
    }

    @Test
    void testLoad_FromMap_LoadsSuccessfully() throws Exception {
        // Arrange
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("boolValue", false);
        data.put("intValue", 123);
        data.put("doubleValue", 4.56);

        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());

        // Act
        config.load(data);

        // Assert
        assertThat(config.isBoolValue()).isFalse();
        assertThat(config.getIntValue()).isEqualTo(123);
        assertThat(config.getDoubleValue()).isEqualTo(4.56);
    }

    @Test
    void testLoad_FromOtherConfig_LoadsSuccessfully() throws Exception {
        // Arrange
        PrimitivesTestConfig sourceConfig = ConfigManager.create(PrimitivesTestConfig.class);
        sourceConfig.withConfigurer(new YamlSnakeYamlConfigurer());
        sourceConfig.setBoolValue(false);
        sourceConfig.setIntValue(999);
        sourceConfig.setDoubleValue(1.414);

        PrimitivesTestConfig targetConfig = ConfigManager.create(PrimitivesTestConfig.class);
        targetConfig.withConfigurer(new YamlSnakeYamlConfigurer());

        // Act
        targetConfig.load(sourceConfig);

        // Assert
        assertThat(targetConfig.isBoolValue()).isFalse();
        assertThat(targetConfig.getIntValue()).isEqualTo(999);
        assertThat(targetConfig.getDoubleValue()).isEqualTo(1.414);
    }

    @Test
    void testLoad_WithUpdate_SavesAfterLoading() throws Exception {
        // Arrange
        String yamlContent = """
            boolValue: false
            intValue: 42
            """;
        Path tempDir = TestUtils.createTempTestDir();
        Path tempFile = tempDir.resolve("update-test.yml");
        Files.writeString(tempFile, yamlContent);

        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer())
            .withBindFile(tempFile);

        // Modify file timestamp to detect if saved
        long originalModified = Files.getLastModifiedTime(tempFile).toMillis();
        Thread.sleep(10); // Ensure timestamp difference

        // Act
        config.load(true);

        // Assert
        assertThat(config.isBoolValue()).isFalse();
        assertThat(config.getIntValue()).isEqualTo(42);
        long newModified = Files.getLastModifiedTime(tempFile).toMillis();
        assertThat(newModified).isGreaterThan(originalModified);
    }

    @Test
    void testLoad_WithoutUpdate_DoesNotSave() throws Exception {
        // Arrange
        String yamlContent = """
            boolValue: false
            intValue: 42
            """;
        Path tempDir = TestUtils.createTempTestDir();
        Path tempFile = tempDir.resolve("no-update-test.yml");
        Files.writeString(tempFile, yamlContent);

        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer())
            .withBindFile(tempFile);

        long originalModified = Files.getLastModifiedTime(tempFile).toMillis();
        Thread.sleep(10);

        // Act
        config.load(false);

        // Assert
        assertThat(config.isBoolValue()).isFalse();
        assertThat(config.getIntValue()).isEqualTo(42);
        long newModified = Files.getLastModifiedTime(tempFile).toMillis();
        assertThat(newModified).isEqualTo(originalModified);
    }

    @Test
    void testLoad_UpdatesFieldValues() throws Exception {
        // Arrange
        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());

        // Initial values from class defaults
        assertThat(config.isBoolValue()).isTrue();
        assertThat(config.getIntValue()).isEqualTo(42);

        String yamlContent = """
            boolValue: false
            intValue: 999
            """;

        // Act
        config.load(yamlContent);

        // Assert - values should be updated
        assertThat(config.isBoolValue()).isFalse();
        assertThat(config.getIntValue()).isEqualTo(999);
    }

    @Test
    void testLoad_MissingFields_KeepsDefaults() throws Exception {
        // Arrange
        String yamlContent = """
            boolValue: false
            """;

        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());

        // Act
        config.load(yamlContent);

        // Assert - only boolValue should change, others keep defaults
        assertThat(config.isBoolValue()).isFalse();
        assertThat(config.getIntValue()).isEqualTo(42); // default from class
        assertThat(config.getDoubleValue()).isEqualTo(3.14); // default from class
    }

    @Test
    void testLoad_ExtraFields_HandlesOrphans() throws Exception {
        // Arrange
        String yamlContent = """
            boolValue: false
            intValue: 999
            orphanKey: orphan value
            anotherOrphan: 123
            """;

        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());

        // Act - should not throw exception
        assertThatCode(() -> config.load(yamlContent)).doesNotThrowAnyException();

        // Assert - declared fields are loaded
        assertThat(config.isBoolValue()).isFalse();
        assertThat(config.getIntValue()).isEqualTo(999);

        // Orphans should be accessible through configurer
        assertThat(config.get("orphanKey")).isEqualTo("orphan value");
        assertThat(config.get("anotherOrphan")).isEqualTo(123);
    }

    @Test
    void testLoad_WithoutConfigurer_ThrowsException() {
        // Arrange
        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        String yamlContent = "boolValue: false";

        // Act & Assert
        assertThatThrownBy(() -> config.load(yamlContent))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("configurer cannot be null");
    }

    @Test
    void testLoad_NonExistentFile_ThrowsException() {
        // Arrange
        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        File nonExistent = new File("/tmp/does-not-exist-" + System.nanoTime() + ".yml");

        // Act & Assert
        assertThatThrownBy(() -> config.load(nonExistent))
            .isInstanceOf(OkaeriException.class)
            .hasMessageContaining("failed #load");
    }

    @Test
    void testLoad_MalformedYaml_ThrowsException() {
        // Arrange
        String malformedYaml = """
            boolValue: false
            intValue: [this is not valid
            unclosed: {bracket
            """;

        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());

        // Act & Assert
        assertThatThrownBy(() -> config.load(malformedYaml))
            .isInstanceOf(OkaeriException.class)
            .hasMessageContaining("failed #load");
    }

    @Test
    void testLoad_TypeConversion_WorksCorrectly() throws Exception {
        // Arrange - String values that should convert to numbers
        String yamlContent = """
            intValue: "123"
            doubleValue: "4.56"
            boolValue: "false"
            """;

        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());

        // Act
        config.load(yamlContent);

        // Assert - type conversion should work
        assertThat(config.getIntValue()).isEqualTo(123);
        assertThat(config.getDoubleValue()).isEqualTo(4.56);
        assertThat(config.isBoolValue()).isFalse();
    }

    @Test
    void testLoad_CompleteWorkflow_CreateSaveLoad() throws Exception {
        // Arrange
        Path tempDir = TestUtils.createTempTestDir();
        Path tempFile = tempDir.resolve("workflow.yml");

        // Create and save
        PrimitivesTestConfig config1 = ConfigManager.create(PrimitivesTestConfig.class);
        config1.withConfigurer(new YamlSnakeYamlConfigurer())
            .withBindFile(tempFile);
        config1.setBoolValue(false);
        config1.setIntValue(777);
        config1.setDoubleValue(9.99);
        config1.save();

        // Load in new instance
        PrimitivesTestConfig config2 = ConfigManager.create(PrimitivesTestConfig.class);
        config2.withConfigurer(new YamlSnakeYamlConfigurer())
            .withBindFile(tempFile);

        // Act
        config2.load();

        // Assert - all values should match
        assertThat(config2.isBoolValue()).isEqualTo(config1.isBoolValue());
        assertThat(config2.getIntValue()).isEqualTo(config1.getIntValue());
        assertThat(config2.getDoubleValue()).isEqualTo(config1.getDoubleValue());
    }

    /**
     * Test config for nested structure test
     */
    @Data
    @NoArgsConstructor
    @EqualsAndHashCode(callSuper = false)
    public static class NestedLoadTestConfig extends OkaeriConfig {
        private String name = "default";
        private int value = 0;
        private SubConfig nested = new SubConfig();

        @Data
        @NoArgsConstructor
        @EqualsAndHashCode(callSuper = false)
        public static class SubConfig extends OkaeriConfig {
            private String subName = "sub-default";
            private int subValue = 0;
        }
    }

    @Test
    void testLoad_NestedConfig_LoadsCorrectly() throws Exception {
        // Arrange
        String yamlContent = """
            name: "test"
            value: 42
            nested:
              subName: "nested-test"
              subValue: 99
            """;

        NestedLoadTestConfig config = ConfigManager.create(NestedLoadTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());

        // Act
        config.load(yamlContent);

        // Assert
        assertThat(config.getName()).isEqualTo("test");
        assertThat(config.getValue()).isEqualTo(42);
        assertThat(config.getNested()).isNotNull();
        assertThat(config.getNested().getSubName()).isEqualTo("nested-test");
        assertThat(config.getNested().getSubValue()).isEqualTo(99);
    }
}
