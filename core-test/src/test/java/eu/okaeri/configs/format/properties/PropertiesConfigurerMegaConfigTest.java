package eu.okaeri.configs.format.properties;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.properties.PropertiesConfigurer;
import eu.okaeri.configs.test.GoldenFileAssertion;
import eu.okaeri.configs.test.MegaConfig;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E tests for PropertiesConfigurer using MegaConfig.
 * <p>
 * Note: Properties format has write-only comments (comments are not preserved on load).
 */
class PropertiesConfigurerMegaConfigTest {

    static Stream<Arguments> propertiesConfigurers() {
        return Stream.of(
            Arguments.of("Properties", new PropertiesConfigurer(), "../properties/src/test/resources/e2e.properties")
        );
    }

    @ParameterizedTest(name = "{0}: Save MegaConfig to string")
    @MethodSource("propertiesConfigurers")
    void testMegaConfig_SaveToString(String configurerName, Configurer configurer, String goldenFilePath) throws Exception {
        // Given: MegaConfig with all features
        MegaConfig config = ConfigManager.create(MegaConfig.class);
        config.setConfigurer(configurer);
        config.populateNestedMegaConfig();

        // When: Save to string
        String output = config.saveToString();

        // Then: Properties are generated successfully
        assertThat(output).isNotNull();
        assertThat(output).isNotEmpty();
        assertThat(output).contains("primBool=true");
        assertThat(output).contains("nestedMegaConfig.");
    }

    @ParameterizedTest(name = "{0}: Round-trip MegaConfig")
    @MethodSource("propertiesConfigurers")
    void testMegaConfig_RoundTrip(String configurerName, Configurer configurer, String goldenFilePath) throws Exception {
        // Given: MegaConfig saved to Properties
        MegaConfig original = ConfigManager.create(MegaConfig.class);
        original.setConfigurer(configurer);
        original.populateNestedMegaConfig();
        String output = original.saveToString();

        // When: Load into new instance
        MegaConfig loaded = ConfigManager.create(MegaConfig.class);
        loaded.setConfigurer(configurer);
        loaded.load(output);

        // Then: Both configs produce identical output
        String reserialized = loaded.saveToString();
        assertThat(reserialized).isEqualTo(output);
    }

    @ParameterizedTest(name = "{0}: Load MegaConfig with nested structure")
    @MethodSource("propertiesConfigurers")
    void testMegaConfig_LoadNestedStructure(String configurerName, Configurer configurer, String goldenFilePath) throws Exception {
        // Given: MegaConfig with nested structure saved to Properties
        MegaConfig original = ConfigManager.create(MegaConfig.class);
        original.setConfigurer(configurer);
        original.populateNestedMegaConfig();
        String output = original.saveToString();

        // When: Load into new instance
        MegaConfig loaded = ConfigManager.create(MegaConfig.class);
        loaded.setConfigurer(configurer);
        loaded.load(output);

        // Then: Nested structure is loaded correctly
        assertThat(loaded.getNestedMegaConfig()).isNotNull();
        assertThat(loaded.getNestedMegaConfig().getWrapBool()).isFalse();
        assertThat(loaded.getNestedMegaConfig().getSimpleString()).isEqualTo("Hello, World!");
        // The nested config's nestedMegaConfig should be null (2 levels deep only)
        assertThat(loaded.getNestedMegaConfig().getNestedMegaConfig()).isNull();
    }

    @ParameterizedTest(name = "{0}: Load MegaConfig from golden file")
    @MethodSource("propertiesConfigurers")
    void testMegaConfig_LoadFromGoldenFile(String configurerName, Configurer configurer, String goldenFilePath) throws Exception {
        // Given
        Path goldenFile = Paths.get(goldenFilePath);

        // When: Load MegaConfig from golden file
        MegaConfig config = ConfigManager.create(MegaConfig.class);
        config.populateNestedMegaConfig();
        config.setConfigurer(configurer);
        config.withBindFile(goldenFile);
        config.saveDefaults();
        config.load();

        // Then: Config loads successfully without errors
        assertThat(config).isNotNull();
    }

    @ParameterizedTest(name = "{0}: Regression test with golden file")
    @MethodSource("propertiesConfigurers")
    void testMegaConfig_RegressionTest(String configurerName, Configurer configurer, String goldenFilePath) throws Exception {
        // Given: MegaConfig with all features
        MegaConfig config = ConfigManager.create(MegaConfig.class);
        config.setConfigurer(configurer);
        config.populateNestedMegaConfig();

        // When: Save to string
        String current = config.saveToString();

        // Then: Compare with golden file (or create it on first run)
        GoldenFileAssertion.forFile(goldenFilePath)
            .withContent(current)
            .assertMatches();
    }
}
