package eu.okaeri.configs.format.toml;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.test.GoldenFileAssertion;
import eu.okaeri.configs.test.MegaConfig;
import eu.okaeri.configs.toml.TomlJacksonConfigurer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Generalized E2E tests for all TOML configurer implementations using MegaConfig.
 * Tests all TOML configurers with parameterized tests.
 */
class TomlConfigurerMegaConfigTest {

    static Stream<Arguments> tomlConfigurers() {
        return Stream.of(
            Arguments.of("TomlJackson", new TomlJacksonConfigurer(), "../toml-jackson/src/test/resources/e2e.toml")
            // Future implementations can be added here
        );
    }

    @ParameterizedTest(name = "{0}: Save MegaConfig to string")
    @MethodSource("tomlConfigurers")
    void testMegaConfig_SaveToString(String configurerName, Configurer configurer, String goldenFilePath) throws Exception {
        // Given: MegaConfig with all features
        MegaConfig config = ConfigManager.create(MegaConfig.class);
        config.setConfigurer(configurer);
        config.populateNestedMegaConfig(); // Initialize 2-level deep nesting

        // When: Save to string
        String toml = config.saveToString();

        // Then: TOML is generated successfully
        assertThat(toml).isNotNull();
        assertThat(toml).isNotEmpty();
        assertThat(toml).contains("primBool");
        assertThat(toml).contains("nestedMegaConfig");
    }

    @ParameterizedTest(name = "{0}: Round-trip MegaConfig")
    @MethodSource("tomlConfigurers")
    void testMegaConfig_RoundTrip(String configurerName, Configurer configurer, String goldenFilePath) throws Exception {
        // Given: MegaConfig saved to TOML
        MegaConfig original = ConfigManager.create(MegaConfig.class);
        original.setConfigurer(configurer);
        original.populateNestedMegaConfig(); // Initialize 2-level deep nesting
        String toml = original.saveToString();

        // When: Load into new instance
        MegaConfig loaded = ConfigManager.create(MegaConfig.class);
        loaded.setConfigurer(configurer);
        loaded.load(toml);

        // Then: Both configs produce identical TOML output
        String reserializedToml = loaded.saveToString();
        assertThat(reserializedToml).isEqualTo(toml);
    }

    @ParameterizedTest(name = "{0}: Load MegaConfig with nested structure")
    @MethodSource("tomlConfigurers")
    void testMegaConfig_LoadNestedStructure(String configurerName, Configurer configurer, String goldenFilePath) throws Exception {
        // Given: MegaConfig with nested structure saved to TOML
        MegaConfig original = ConfigManager.create(MegaConfig.class);
        original.setConfigurer(configurer);
        original.populateNestedMegaConfig();
        String toml = original.saveToString();

        // When: Load into new instance
        MegaConfig loaded = ConfigManager.create(MegaConfig.class);
        loaded.setConfigurer(configurer);
        loaded.load(toml);

        // Then: Nested structure is loaded correctly
        assertThat(loaded.getNestedMegaConfig()).isNotNull();
        assertThat(loaded.getNestedMegaConfig().getWrapBool()).isFalse();
        assertThat(loaded.getNestedMegaConfig().getSimpleString()).isEqualTo("Hello, World!");
        // The nested config's nestedMegaConfig should be null (2 levels deep only)
        assertThat(loaded.getNestedMegaConfig().getNestedMegaConfig()).isNull();
    }

    @ParameterizedTest(name = "{0}: Load MegaConfig from golden file")
    @MethodSource("tomlConfigurers")
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
        // The fact that we got here without exception means the golden file is valid TOML
        assertThat(config).isNotNull();
    }

    @ParameterizedTest(name = "{0}: Regression test with golden file")
    @MethodSource("tomlConfigurers")
    void testMegaConfig_RegressionTest(String configurerName, Configurer configurer, String goldenFilePath) throws Exception {
        // Given: MegaConfig with all features
        MegaConfig config = ConfigManager.create(MegaConfig.class);
        config.setConfigurer(configurer);
        config.populateNestedMegaConfig();

        // When: Save to string
        String currentToml = config.saveToString();

        // Then: Compare with golden file (or create it on first run)
        GoldenFileAssertion.forFile(goldenFilePath)
            .withContent(currentToml)
            .assertMatches();
    }
}
