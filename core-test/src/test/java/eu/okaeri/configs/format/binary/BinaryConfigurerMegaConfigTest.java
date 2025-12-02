package eu.okaeri.configs.format.binary;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.binary.BinaryConfigurer;
import eu.okaeri.configs.test.MegaConfig;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E tests for BinaryConfigurer using MegaConfig.
 * <p>
 * Note: Binary format does NOT support comments or headers, and output
 * is not human-readable. Golden file comparison is not used because binary
 * serialization output may vary across JVM implementations.
 */
class BinaryConfigurerMegaConfigTest {

    private static final String GOLDEN_FILE_PATH = "../binary/src/test/resources/e2e.bin";

    @Test
    void testMegaConfig_SaveToBytes() {
        // Given: MegaConfig with all features
        MegaConfig config = ConfigManager.create(MegaConfig.class);
        config.setConfigurer(new BinaryConfigurer());
        config.populateNestedMegaConfig();

        // When: Save to bytes
        byte[] bytes = config.saveToBytes();

        // Then: Binary data is generated successfully
        assertThat(bytes).isNotNull();
        assertThat(bytes.length).isGreaterThan(0);
    }

    @Test
    void testMegaConfig_RoundTrip() {
        // Given: MegaConfig saved to binary
        MegaConfig original = ConfigManager.create(MegaConfig.class);
        original.setConfigurer(new BinaryConfigurer());
        original.populateNestedMegaConfig();
        byte[] bytes = original.saveToBytes();

        // When: Load into new instance
        MegaConfig loaded = ConfigManager.create(MegaConfig.class);
        loaded.setConfigurer(new BinaryConfigurer());
        loaded.load(bytes);

        // Then: Both configs produce identical binary output
        byte[] reserializedBytes = loaded.saveToBytes();
        assertThat(reserializedBytes).isEqualTo(bytes);
    }

    @Test
    void testMegaConfig_LoadNestedStructure() {
        // Given: MegaConfig with nested structure saved to binary
        MegaConfig original = ConfigManager.create(MegaConfig.class);
        original.setConfigurer(new BinaryConfigurer());
        original.populateNestedMegaConfig();
        byte[] bytes = original.saveToBytes();

        // When: Load into new instance
        MegaConfig loaded = ConfigManager.create(MegaConfig.class);
        loaded.setConfigurer(new BinaryConfigurer());
        loaded.load(bytes);

        // Then: Nested structure is loaded correctly
        assertThat(loaded.getNestedMegaConfig()).isNotNull();
        assertThat(loaded.getNestedMegaConfig().getWrapBool()).isFalse();
        assertThat(loaded.getNestedMegaConfig().getSimpleString()).isEqualTo("Hello, World!");
        assertThat(loaded.getNestedMegaConfig().getNestedMegaConfig()).isNull();
    }

    @Test
    void testMegaConfig_LoadFromGoldenFile() throws Exception {
        // Given
        Path goldenFile = Paths.get(GOLDEN_FILE_PATH);

        // When: Load MegaConfig from golden file
        MegaConfig config = ConfigManager.create(MegaConfig.class);
        config.populateNestedMegaConfig();
        config.setConfigurer(new BinaryConfigurer());
        config.withBindFile(goldenFile);
        config.saveDefaults();
        config.load();

        // Then: Config loads successfully without errors
        assertThat(config).isNotNull();
    }

    @Test
    void testMegaConfig_RegressionTest() throws Exception {
        // Given: MegaConfig with all features
        MegaConfig config = ConfigManager.create(MegaConfig.class);
        config.setConfigurer(new BinaryConfigurer());
        config.populateNestedMegaConfig();

        // When: Save to bytes
        byte[] currentBytes = config.saveToBytes();

        // Then: Compare with golden file (create on first run)
        // Note: Binary format comparison - exact byte match required
        Path goldenFile = Paths.get(GOLDEN_FILE_PATH);
        if (!Files.exists(goldenFile)) {
            Files.createDirectories(goldenFile.getParent());
            Files.write(goldenFile, currentBytes);
            System.out.println("Created golden file: " + goldenFile.toAbsolutePath());
        }

        byte[] expectedBytes = Files.readAllBytes(goldenFile);
        assertThat(currentBytes).isEqualTo(expectedBytes);
    }

    @Test
    void testMegaConfig_PreservesAllFieldTypes() {
        // Given: MegaConfig with all field types
        MegaConfig original = ConfigManager.create(MegaConfig.class);
        original.setConfigurer(new BinaryConfigurer());
        original.populateNestedMegaConfig();

        // When: Round-trip through binary
        byte[] bytes = original.saveToBytes();

        MegaConfig loaded = ConfigManager.create(MegaConfig.class);
        loaded.setConfigurer(new BinaryConfigurer());
        loaded.load(bytes);

        // Then: All field types are preserved
        assertThat(loaded.isPrimBool()).isEqualTo(original.isPrimBool());
        assertThat(loaded.getPrimByte()).isEqualTo(original.getPrimByte());
        assertThat(loaded.getPrimChar()).isEqualTo(original.getPrimChar());
        assertThat(loaded.getPrimDouble()).isEqualTo(original.getPrimDouble());
        assertThat(loaded.getPrimFloat()).isEqualTo(original.getPrimFloat());
        assertThat(loaded.getPrimInt()).isEqualTo(original.getPrimInt());
        assertThat(loaded.getPrimLong()).isEqualTo(original.getPrimLong());
        assertThat(loaded.getPrimShort()).isEqualTo(original.getPrimShort());

        assertThat(loaded.getWrapBool()).isEqualTo(original.getWrapBool());
        assertThat(loaded.getSimpleString()).isEqualTo(original.getSimpleString());
        assertThat(loaded.getStringList()).isEqualTo(original.getStringList());
        assertThat(loaded.getStringSet()).isEqualTo(original.getStringSet());
        assertThat(loaded.getSimpleMap()).isEqualTo(original.getSimpleMap());

        assertThat(loaded.getNestedMegaConfig()).isNotNull();
        assertThat(loaded.getNestedMegaConfig().getSimpleString())
            .isEqualTo(original.getNestedMegaConfig().getSimpleString());
    }
}
