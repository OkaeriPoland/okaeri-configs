package eu.okaeri.configs.yaml.snakeyaml;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.test.GoldenFileAssertion;
import eu.okaeri.configs.test.MegaConfig;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end regression test for YamlSnakeYamlConfigurer using MegaConfig.
 * 
 * This test uses a golden file approach via GoldenFileAssertion:
 * - On first run: Creates e2e.yml (commit this file)
 * - Subsequent runs: Compares current output against e2e.yml
 * 
 * Any differences indicate potential unintended changes to YAML formatting
 * and should be manually validated before updating the golden file.
 */
class YamlSnakeYamlConfigurerMegaConfigTest {

    private static final String GOLDEN_FILE_PATH = "src/test/resources/e2e.yml";

    @Test
    void testMegaConfig_RegressionTest() throws Exception {
        // Given: MegaConfig with all features
        MegaConfig config = ConfigManager.create(MegaConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        
        // When: Save to string
        String currentYaml = config.saveToString();
        
        // Then: Compare with golden file (or create it on first run)
        GoldenFileAssertion.forFile(GOLDEN_FILE_PATH)
            .withContent(currentYaml)
            .assertMatches();
    }

    @Test
    void testMegaConfig_LoadFromGoldenFile() throws Exception {
        // Given: Golden file exists (skip if first run)
        Path goldenFilePath = Paths.get(GOLDEN_FILE_PATH);
        if (!Files.exists(goldenFilePath)) {
            System.out.println("Skipping load test - golden file does not exist yet");
            return;
        }
        
        // When: Load MegaConfig from golden file
        MegaConfig config = ConfigManager.create(MegaConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.load(goldenFilePath);
        
        // Then: Config loads successfully without errors
        // The fact that we got here without exception means the golden file is valid YAML
        assertThat(config).isNotNull();
    }

    @Test
    void testMegaConfig_RoundTrip() throws Exception {
        // Given: MegaConfig saved to YAML
        MegaConfig original = ConfigManager.create(MegaConfig.class);
        original.withConfigurer(new YamlSnakeYamlConfigurer());
        String yaml = original.saveToString();
        
        // When: Load into new instance
        MegaConfig loaded = ConfigManager.create(MegaConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer());
        loaded.load(yaml);
        
        // Then: Both configs produce identical YAML output
        String reserializedYaml = loaded.saveToString();
        assertThat(reserializedYaml).isEqualTo(yaml);
    }
}
