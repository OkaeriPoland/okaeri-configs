package eu.okaeri.configs.format.yaml;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.test.GoldenFileAssertion;
import eu.okaeri.configs.test.MegaConfig;
import eu.okaeri.configs.yaml.bukkit.YamlBukkitConfigurer;
import eu.okaeri.configs.yaml.bungee.YamlBungeeConfigurer;
import eu.okaeri.configs.yaml.jackson.YamlJacksonConfigurer;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Generalized E2E tests for all YAML configurer implementations using MegaConfig.
 * Tests all three YAML configurers (SnakeYAML, Bukkit, Bungee) with parameterized tests.
 */
class YamlConfigurerMegaConfigTest {

    static Stream<Arguments> yamlConfigurers() {
        return Stream.of(
            Arguments.of("SnakeYAML", new YamlSnakeYamlConfigurer(), "../yaml-snakeyaml/src/test/resources/e2e.yml"),
            Arguments.of("Jackson", new YamlJacksonConfigurer(), "../yaml-jackson/src/test/resources/e2e.yml"),
            Arguments.of("Bukkit", new YamlBukkitConfigurer(), "../yaml-bukkit/src/test/resources/e2e.yml"),
            Arguments.of("Bungee", new YamlBungeeConfigurer(), "../yaml-bungee/src/test/resources/e2e.yml")
        );
    }

    @ParameterizedTest(name = "{0}: Save MegaConfig to string")
    @MethodSource("yamlConfigurers")
    void testMegaConfig_SaveToString(String configurerName, Configurer configurer, String goldenFilePath) throws Exception {
        // Given: MegaConfig with all features
        MegaConfig config = ConfigManager.create(MegaConfig.class);
        config.withConfigurer(configurer);
        config.populateNestedMegaConfig(); // Initialize 2-level deep nesting

        // When: Save to string
        String yaml = config.saveToString();

        // Then: YAML is generated successfully
        assertThat(yaml).isNotNull();
        assertThat(yaml).isNotEmpty();
        assertThat(yaml).contains("primBool:");
        assertThat(yaml).contains("nestedMegaConfig:");
    }

    @ParameterizedTest(name = "{0}: Round-trip MegaConfig")
    @MethodSource("yamlConfigurers")
    void testMegaConfig_RoundTrip(String configurerName, Configurer configurer, String goldenFilePath) throws Exception {
        // Given: MegaConfig saved to YAML
        MegaConfig original = ConfigManager.create(MegaConfig.class);
        original.withConfigurer(configurer);
        original.populateNestedMegaConfig(); // Initialize 2-level deep nesting
        String yaml = original.saveToString();

        // When: Load into new instance
        MegaConfig loaded = ConfigManager.create(MegaConfig.class);
        loaded.withConfigurer(configurer);
        loaded.load(yaml);

        // Then: Both configs produce identical YAML output
        String reserializedYaml = loaded.saveToString();
        assertThat(reserializedYaml).isEqualTo(yaml);
    }

    @ParameterizedTest(name = "{0}: Load MegaConfig with nested structure")
    @MethodSource("yamlConfigurers")
    void testMegaConfig_LoadNestedStructure(String configurerName, Configurer configurer, String goldenFilePath) throws Exception {
        // Given: MegaConfig with nested structure saved to YAML
        MegaConfig original = ConfigManager.create(MegaConfig.class);
        original.withConfigurer(configurer);
        original.populateNestedMegaConfig();
        String yaml = original.saveToString();

        // When: Load into new instance
        MegaConfig loaded = ConfigManager.create(MegaConfig.class);
        loaded.withConfigurer(configurer);
        loaded.load(yaml);

        // Then: Nested structure is loaded correctly
        assertThat(loaded.getNestedMegaConfig()).isNotNull();
        assertThat(loaded.getNestedMegaConfig().getWrapBool()).isFalse();
        assertThat(loaded.getNestedMegaConfig().getSimpleString()).isEqualTo("Hello, World!");
        // The nested config's nestedMegaConfig should be null (2 levels deep only)
        assertThat(loaded.getNestedMegaConfig().getNestedMegaConfig()).isNull();
    }

    @ParameterizedTest(name = "{0}: Load MegaConfig from golden file")
    @MethodSource("yamlConfigurers")
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
        // The fact that we got here without exception means the golden file is valid YAML
        assertThat(config).isNotNull();
    }

    @ParameterizedTest(name = "{0}: Regression test with golden file")
    @MethodSource("yamlConfigurers")
    void testMegaConfig_RegressionTest(String configurerName, Configurer configurer, String goldenFilePath) throws Exception {
        // Given: MegaConfig with all features
        MegaConfig config = ConfigManager.create(MegaConfig.class);
        config.withConfigurer(configurer);
        config.populateNestedMegaConfig();

        // When: Save to string
        String currentYaml = config.saveToString();

        // Then: Compare with golden file (or create it on first run)
        GoldenFileAssertion.forFile(goldenFilePath)
            .withContent(currentYaml)
            .assertMatches();
    }
}
