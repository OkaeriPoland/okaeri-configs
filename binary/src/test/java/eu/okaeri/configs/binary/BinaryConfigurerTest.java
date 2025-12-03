package eu.okaeri.configs.binary;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests BinaryConfigurer-specific features.
 * Only tests for backend-specific functionality not covered by parameterized tests in core-test.
 */
class BinaryConfigurerTest {

    // ==================== Constructor Tests ====================

    @Test
    void testDefaultConstructor() {
        BinaryConfigurer configurer = new BinaryConfigurer();
        assertThat(configurer).isNotNull();
    }


    // ==================== getExtensions Tests ====================

    @Test
    void testGetExtensions() {
        BinaryConfigurer configurer = new BinaryConfigurer();
        assertThat(configurer.getExtensions()).containsExactly("bin");
    }

    // ==================== Round-trip Tests ====================

    @Test
    void testRoundTrip(@TempDir Path tempDir) throws Exception {
        // Given: Config with various types
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.setConfigurer(new BinaryConfigurer());
        config.setName("Test");
        config.setValue(42);
        config.setItems(List.of("a", "b", "c"));

        // When: Save to file and reload
        Path file = tempDir.resolve("test.bin");
        config.save(file);

        TestConfig loaded = ConfigManager.create(TestConfig.class);
        loaded.setConfigurer(new BinaryConfigurer());
        loaded.load(file);

        // Then: Values are preserved
        assertThat(loaded.getName()).isEqualTo("Test");
        assertThat(loaded.getValue()).isEqualTo(42);
        assertThat(loaded.getItems()).containsExactly("a", "b", "c");
    }

    @Test
    void testBinaryFileIsNotEmpty(@TempDir Path tempDir) throws Exception {
        // Given: Config with data
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.setConfigurer(new BinaryConfigurer());
        config.setName("Test");
        config.setValue(42);

        // When: Save to file
        Path file = tempDir.resolve("test.bin");
        config.save(file);

        // Then: File contains binary data
        byte[] bytes = Files.readAllBytes(file);
        assertThat(bytes.length).isGreaterThan(0);
    }

    @Test
    void testSaveToBytes() {
        // Given: Config with data
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.setConfigurer(new BinaryConfigurer());
        config.setName("Test");
        config.setValue(42);

        // When: Save to bytes
        byte[] bytes = config.saveToBytes();

        // Then: Bytes are not empty
        assertThat(bytes).isNotNull();
        assertThat(bytes.length).isGreaterThan(0);
    }

    @Test
    void testLoadFromBytes() {
        // Given: Config saved to bytes
        TestConfig original = ConfigManager.create(TestConfig.class);
        original.setConfigurer(new BinaryConfigurer());
        original.setName("Test");
        original.setValue(42);
        byte[] bytes = original.saveToBytes();

        // When: Load from bytes
        TestConfig loaded = ConfigManager.create(TestConfig.class);
        loaded.setConfigurer(new BinaryConfigurer());
        loaded.load(bytes);

        // Then: Values are preserved
        assertThat(loaded.getName()).isEqualTo("Test");
        assertThat(loaded.getValue()).isEqualTo(42);
    }

    // ==================== Test Config Classes ====================

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class TestConfig extends OkaeriConfig {
        private String name = "default";
        private int value = 0;
        private List<String> items = List.of();
    }
}
