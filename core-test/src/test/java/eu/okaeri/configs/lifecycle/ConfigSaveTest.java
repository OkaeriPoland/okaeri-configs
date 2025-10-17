package eu.okaeri.configs.lifecycle;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.exception.OkaeriException;
import eu.okaeri.configs.test.TestUtils;
import eu.okaeri.configs.test.configs.PrimitivesTestConfig;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for OkaeriConfig save operations.
 * <p>
 * Scenarios tested:
 * - Save to file (File, Path, String pathname)
 * - Save to OutputStream
 * - Save to String (saveToString())
 * - saveDefaults() behavior (creates file if not exists, skips if exists)
 * - Parent directory creation
 * - File overwriting
 * - Orphan removal (enabled/disabled)
 * - Data loss prevention (file preserved on serialization errors)
 * - Error cases (no configurer, no bind file)
 */
class ConfigSaveTest {

    // Test config classes

    /**
     * Custom type without a registered serializer - used to trigger serialization errors
     */
    public static class UnserializableType {
        private String data = "test";
    }

    /**
     * Config with an unserializable field to test error handling
     */
    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class ConfigWithUnserializableField extends OkaeriConfig {
        private String normalField = "normal";
        private UnserializableType problematicField = new UnserializableType();
    }

    @Test
    void testSave_ToFile_SavesSuccessfully() throws Exception {
        // Arrange
        File tempFile = TestUtils.createTempFile("", ".yml");
        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.setBoolValue(false);
        config.setIntValue(999);

        // Act
        config.save(tempFile);

        // Assert
        assertThat(tempFile).exists();
        String content = Files.readString(tempFile.toPath());
        assertThat(content).contains("boolValue: false");
        assertThat(content).contains("intValue: 999");
    }

    @Test
    void testSave_ToPath_SavesSuccessfully() throws Exception {
        // Arrange
        Path tempDir = TestUtils.createTempTestDir();
        Path tempFile = tempDir.resolve("test-config.yml");
        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.setByteValue((byte) 42);
        config.setShortValue((short) 1234);

        // Act
        config.save(tempFile);

        // Assert
        assertThat(tempFile).exists();
        String content = Files.readString(tempFile);
        assertThat(content).contains("byteValue: 42");
        assertThat(content).contains("shortValue: 1234");
    }

    @Test
    void testSave_ToBindFile_SavesSuccessfully() throws Exception {
        // Arrange
        Path tempDir = TestUtils.createTempTestDir();
        Path tempFile = tempDir.resolve("bound-config.yml");
        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer())
            .withBindFile(tempFile);
        config.setDoubleValue(3.14159);

        // Act
        config.save();

        // Assert
        assertThat(tempFile).exists();
        String content = Files.readString(tempFile);
        assertThat(content).contains("doubleValue: 3.14159");
    }

    @Test
    void testSave_ToOutputStream_WritesCorrectly() throws Exception {
        // Arrange
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.setCharValue('X');
        config.setLongValue(9876543210L);

        // Act
        config.save(outputStream);

        // Assert
        String result = outputStream.toString("UTF-8");
        assertThat(result).contains("charValue: X");
        assertThat(result).contains("longValue: 9876543210");
    }

    @Test
    void testSaveToString_ReturnsCorrectString() throws Exception {
        // Arrange
        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.setFloatValue(2.71f);
        config.setIntWrapper(12345);

        // Act
        String result = config.saveToString();

        // Assert
        assertThat(result).isNotEmpty();
        assertThat(result).contains("floatValue: 2.71");
        assertThat(result).contains("intWrapper: 12345");
    }

    @Test
    void testSaveDefaults_FileDoesNotExist_CreatesFile() throws Exception {
        // Arrange
        Path tempDir = TestUtils.createTempTestDir();
        Path tempFile = tempDir.resolve("defaults.yml");
        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer())
            .withBindFile(tempFile);

        // Act
        config.saveDefaults();

        // Assert
        assertThat(tempFile).exists();
    }

    @Test
    void testSaveDefaults_FileExists_DoesNotOverwrite() throws Exception {
        // Arrange
        Path tempDir = TestUtils.createTempTestDir();
        Path tempFile = tempDir.resolve("existing.yml");
        Files.writeString(tempFile, "existing: content");

        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer())
            .withBindFile(tempFile);
        config.setIntValue(999);

        // Act
        config.saveDefaults();

        // Assert
        String content = Files.readString(tempFile);
        assertThat(content).isEqualTo("existing: content");
        assertThat(content).doesNotContain("intValue: 999");
    }

    @Test
    void testSave_CreatesParentDirectories() throws Exception {
        // Arrange
        Path tempDir = TestUtils.createTempTestDir();
        Path nestedFile = tempDir.resolve("nested/deep/config.yml");
        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());

        // Act
        config.save(nestedFile.toFile());

        // Assert
        assertThat(nestedFile).exists();
        assertThat(nestedFile.getParent()).exists();
        assertThat(nestedFile.getParent().getParent()).exists();
    }

    @Test
    void testSave_OverwritesExistingFile() throws Exception {
        // Arrange
        Path tempDir = TestUtils.createTempTestDir();
        Path tempFile = tempDir.resolve("overwrite.yml");
        Files.writeString(tempFile, "old: content\nstays: here");

        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.setIntValue(777);

        // Act
        config.save(tempFile.toFile());

        // Assert
        String content = Files.readString(tempFile);
        assertThat(content).contains("intValue: 777");
        assertThat(content).doesNotContain("old: content");
    }

    @Test
    void testSave_WithOrphanRemovalEnabled_RemovesOrphans() throws Exception {
        // Arrange
        Path tempDir = TestUtils.createTempTestDir();
        Path tempFile = tempDir.resolve("orphans.yml");

        // Create config with extra field
        String initialData = """
            boolValue: true
            intValue: 42
            orphanKey: should be removed
            """;
        Files.writeString(tempFile, initialData);

        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer())
            .withBindFile(tempFile)
            .withRemoveOrphans(true);
        config.load();

        // Act
        config.save();

        // Assert
        String content = Files.readString(tempFile);
        assertThat(content).doesNotContain("orphanKey");
    }

    @Test
    void testSave_WithOrphanRemovalDisabled_KeepsOrphans() throws Exception {
        // Arrange
        Path tempDir = TestUtils.createTempTestDir();
        Path tempFile = tempDir.resolve("keep-orphans.yml");

        // Create config with extra field
        String initialData = """
            boolValue: true
            intValue: 42
            orphanKey: should be kept
            """;
        Files.writeString(tempFile, initialData);

        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer())
            .withBindFile(tempFile)
            .withRemoveOrphans(false);
        config.load();

        // Act
        config.save();

        // Assert
        String content = Files.readString(tempFile);
        assertThat(content).contains("orphanKey");
    }

    @Test
    void testSave_WithoutConfigurer_ThrowsException() {
        // Arrange
        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        File tempFile = new File("/tmp/test.yml");

        // Act & Assert
        assertThatThrownBy(() -> config.save(tempFile))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("configurer cannot be null");
    }

    @Test
    void testSave_WithoutBindFile_ThrowsException() {
        // Arrange
        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());

        // Act & Assert
        assertThatThrownBy(() -> config.save())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("bindFile cannot be null");
    }

    @Test
    void testSaveDefaults_WithoutBindFile_ThrowsException() {
        // Arrange
        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());

        // Act & Assert
        assertThatThrownBy(() -> config.saveDefaults())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("bindFile cannot be null");
    }

    @Test
    void testSave_VerifiesFieldValuesAreWritten() throws Exception {
        // Arrange
        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.setBoolValue(false);
        config.setByteValue((byte) 100);
        config.setCharValue('Z');
        config.setDoubleValue(9.87654);
        config.setFloatValue(1.23f);
        config.setIntValue(555);
        config.setLongValue(999999L);
        config.setShortValue((short) 777);

        // Act
        String result = config.saveToString();

        // Assert
        assertThat(result).contains("boolValue: false");
        assertThat(result).contains("byteValue: 100");
        assertThat(result).contains("charValue: Z");
        assertThat(result).contains("doubleValue: 9.87654");
        assertThat(result).contains("floatValue: 1.23");
        assertThat(result).contains("intValue: 555");
        assertThat(result).contains("longValue: 999999");
        assertThat(result).contains("shortValue: 777");
    }

    @Test
    void testSave_SerializationError_PreservesOriginalFileContent() throws Exception {
        // Arrange
        Path tempDir = TestUtils.createTempTestDir();
        Path tempFile = tempDir.resolve("preserve-on-error.yml");

        // Create file with original content
        String originalContent = "original: content\nshould: be preserved\n";
        Files.writeString(tempFile, originalContent);

        // Create config with unserializable field
        ConfigWithUnserializableField config = ConfigManager.create(ConfigWithUnserializableField.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.setNormalField("updated");

        // Act & Assert - save should throw exception due to unserializable field
        assertThatThrownBy(() -> config.save(tempFile.toFile()))
            .isInstanceOf(OkaeriException.class)
            .hasMessageContaining("problematicField");

        // Assert - original file content should be preserved (not truncated)
        String fileContent = Files.readString(tempFile);
        assertThat(fileContent).isEqualTo(originalContent);
        assertThat(fileContent).contains("original: content");
        assertThat(fileContent).contains("should: be preserved");
        assertThat(fileContent).doesNotContain("normalField");
    }

    @Test
    void testSave_SerializationError_NewFileNotCreated() throws Exception {
        // Arrange
        Path tempDir = TestUtils.createTempTestDir();
        Path tempFile = tempDir.resolve("should-not-be-created.yml");

        // Verify file doesn't exist
        assertThat(tempFile).doesNotExist();

        // Create config with unserializable field
        ConfigWithUnserializableField config = ConfigManager.create(ConfigWithUnserializableField.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());

        // Act & Assert - save should throw exception
        assertThatThrownBy(() -> config.save(tempFile.toFile()))
            .isInstanceOf(OkaeriException.class);

        // Assert - file should not have been created
        assertThat(tempFile).doesNotExist();
    }
}
