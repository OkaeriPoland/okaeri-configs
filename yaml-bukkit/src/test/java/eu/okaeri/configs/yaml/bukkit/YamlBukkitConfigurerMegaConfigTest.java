package eu.okaeri.configs.yaml.bukkit;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.test.GoldenFileAssertion;
import eu.okaeri.configs.test.MegaConfig;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Backend-specific E2E tests for YamlBukkitConfigurer.
 * Parameterized MegaConfig tests are in core-test/format/yaml.
 */
class YamlBukkitConfigurerMegaConfigTest {

    private static final String GOLDEN_FILE_PATH = "src/test/resources/e2e.yml";

    @Test
    @Order(1)
    void testMegaConfig_LoadFromGoldenFile() throws Exception {
        // Given
        Path goldenFilePath = Paths.get(GOLDEN_FILE_PATH);

        // When: Load MegaConfig from golden file
        MegaConfig config = ConfigManager.create(MegaConfig.class);
        config.populateNestedMegaConfig();
        config.withConfigurer(new YamlBukkitConfigurer());
        config.withBindFile(goldenFilePath);
        config.saveDefaults();
        config.load();

        // Then: Config loads successfully without errors
        // The fact that we got here without exception means the golden file is valid YAML
        assertThat(config).isNotNull();
    }

    @Test
    @Order(2)
    void testMegaConfig_RegressionTest() throws Exception {
        // Given: MegaConfig with all features
        MegaConfig config = ConfigManager.create(MegaConfig.class);
        config.withConfigurer(new YamlBukkitConfigurer());
        config.populateNestedMegaConfig();

        // When: Save to string
        String currentYaml = config.saveToString();

        // Then: Compare with golden file (or create it on first run)
        GoldenFileAssertion.forFile(GOLDEN_FILE_PATH)
            .withContent(currentYaml)
            .assertMatches();
    }
}
