package eu.okaeri.configs.format.json;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.json.gson.JsonGsonConfigurer;
import eu.okaeri.configs.json.simple.JsonSimpleConfigurer;
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
 * Generalized E2E tests for all JSON configurer implementations using MegaConfig.
 * Tests both JSON-GSON and JSON-Simple configurers with parameterized tests.
 * 
 * Note: JSON format does NOT support comments or headers, so MegaConfig tests
 * focus on data integrity and structure preservation only.
 */
class JsonConfigurerMegaConfigTest {

    static Stream<Arguments> jsonConfigurers() {
        return Stream.of(
            Arguments.of("JSON-GSON", new JsonGsonConfigurer(), "../json-gson/src/test/resources/e2e.json"),
            Arguments.of("JSON-Simple", new JsonSimpleConfigurer(), "../json-simple/src/test/resources/e2e.json")
        );
    }

    @ParameterizedTest(name = "{0}: Save MegaConfig to string")
    @MethodSource("jsonConfigurers")
    void testMegaConfig_SaveToString(String configurerName, Configurer configurer, String goldenFilePath) throws Exception {
        // Given: MegaConfig with all features
        MegaConfig config = ConfigManager.create(MegaConfig.class);
        config.withConfigurer(configurer);
        config.populateNestedMegaConfig(); // Initialize 2-level deep nesting

        // When: Save to string
        String json = config.saveToString();

        // Then: JSON is generated successfully
        assertThat(json).isNotNull();
        assertThat(json).isNotEmpty();
        assertThat(json).contains("\"primBool\":");
        assertThat(json).contains("\"nestedMegaConfig\":");
    }

    @ParameterizedTest(name = "{0}: Round-trip MegaConfig")
    @MethodSource("jsonConfigurers")
    void testMegaConfig_RoundTrip(String configurerName, Configurer configurer, String goldenFilePath) throws Exception {
        // Given: MegaConfig saved to JSON
        MegaConfig original = ConfigManager.create(MegaConfig.class);
        original.withConfigurer(configurer);
        original.populateNestedMegaConfig(); // Initialize 2-level deep nesting
        String json = original.saveToString();

        // When: Load into new instance
        MegaConfig loaded = ConfigManager.create(MegaConfig.class);
        loaded.withConfigurer(configurer);
        loaded.load(json);

        // Then: Both configs produce identical JSON output
        String reserializedJson = loaded.saveToString();
        assertThat(reserializedJson).isEqualTo(json);
    }

    @ParameterizedTest(name = "{0}: Load MegaConfig with nested structure")
    @MethodSource("jsonConfigurers")
    void testMegaConfig_LoadNestedStructure(String configurerName, Configurer configurer, String goldenFilePath) throws Exception {
        // Given: MegaConfig with nested structure saved to JSON
        MegaConfig original = ConfigManager.create(MegaConfig.class);
        original.withConfigurer(configurer);
        original.populateNestedMegaConfig();
        String json = original.saveToString();

        // When: Load into new instance
        MegaConfig loaded = ConfigManager.create(MegaConfig.class);
        loaded.withConfigurer(configurer);
        loaded.load(json);

        // Then: Nested structure is loaded correctly
        assertThat(loaded.getNestedMegaConfig()).isNotNull();
        assertThat(loaded.getNestedMegaConfig().getWrapBool()).isFalse();
        assertThat(loaded.getNestedMegaConfig().getSimpleString()).isEqualTo("Hello, World!");
        // The nested config's nestedMegaConfig should be null (2 levels deep only)
        assertThat(loaded.getNestedMegaConfig().getNestedMegaConfig()).isNull();
    }

    @ParameterizedTest(name = "{0}: Load MegaConfig from golden file")
    @MethodSource("jsonConfigurers")
    void testMegaConfig_LoadFromGoldenFile(String configurerName, Configurer configurer, String goldenFilePath) throws Exception {
        // Given
        Path goldenFile = Paths.get(goldenFilePath);

        // When: Load MegaConfig from golden file
        MegaConfig config = ConfigManager.create(MegaConfig.class);
        config.populateNestedMegaConfig();
        config.withConfigurer(configurer);
        config.withBindFile(goldenFile);
        config.saveDefaults();
        config.load();

        // Then: Config loads successfully without errors
        // The fact that we got here without exception means the golden file is valid JSON
        assertThat(config).isNotNull();
    }

    @ParameterizedTest(name = "{0}: Regression test with golden file")
    @MethodSource("jsonConfigurers")
    void testMegaConfig_RegressionTest(String configurerName, Configurer configurer, String goldenFilePath) throws Exception {
        // Given: MegaConfig with all features
        MegaConfig config = ConfigManager.create(MegaConfig.class);
        config.withConfigurer(configurer);
        config.populateNestedMegaConfig();

        // When: Save to string
        String currentJson = config.saveToString();

        // Then: Compare with golden file (or create it on first run)
        GoldenFileAssertion.forFile(goldenFilePath)
            .withContent(currentJson)
            .assertMatches();
    }
}
