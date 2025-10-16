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

    @Test
    void testMegaConfig_CommentPreservation() throws Exception {
        // Given: MegaConfig with comments
        MegaConfig config = ConfigManager.create(MegaConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        
        // When: Save to YAML
        String yaml = config.saveToString();
        
        // Then: Verify key comments are present
        assertThat(yaml).contains("# Boolean primitive and wrapper");
        assertThat(yaml).contains("# Numeric primitives");
        assertThat(yaml).contains("# Wrapper types");
        assertThat(yaml).contains("# String tests");
        assertThat(yaml).contains("# including unicode and special chars");
        assertThat(yaml).contains("# Math types for precision");
        assertThat(yaml).contains("# List of strings");
        assertThat(yaml).contains("# Simple string-to-string map");
    }

    @Test
    void testMegaConfig_HeaderPreservation() throws Exception {
        // Given: MegaConfig with header
        MegaConfig config = ConfigManager.create(MegaConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        
        // When: Save to YAML
        String yaml = config.saveToString();
        
        // Then: Verify header is present at the top
        String[] lines = yaml.split("\n");
        assertThat(lines[0]).contains("# ==========================================");
        assertThat(yaml).contains("#   Okaeri Configs - Mega Test Config");
        assertThat(yaml).contains("#   Tests ALL features comprehensively");
    }

    @Test
    void testMegaConfig_UnicodePreservation() throws Exception {
        // Given: MegaConfig with unicode strings
        MegaConfig config = ConfigManager.create(MegaConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        
        // When: Save to YAML
        String yaml = config.saveToString();
        
        // Then: Unicode content is present (verifies encoding handling)
        assertThat(yaml).contains("こんにちは世界"); // Japanese
        assertThat(yaml).contains("Привет мир"); // Russian
        assertThat(yaml).contains("Część świecie"); // Polish
        assertThat(yaml).contains("Łódź"); // Polish L with stroke
        assertThat(yaml).contains("Gdańsk"); // Polish a with ogonek
        assertThat(yaml).contains("🌍"); // Emoji
    }

    @Test
    void testMegaConfig_StructureIntegrity() throws Exception {
        // Given: MegaConfig saved to YAML
        MegaConfig config = ConfigManager.create(MegaConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        String yaml = config.saveToString();
        
        // Then: Verify YAML contains expected structural elements
        assertThat(yaml).contains("primBool:");
        assertThat(yaml).contains("wrapBool:");
        assertThat(yaml).contains("simpleString:");
        assertThat(yaml).contains("unicodeJapanese:");
        assertThat(yaml).contains("bigInt:");
        assertThat(yaml).contains("stringList:");
        assertThat(yaml).contains("- alpha");
        assertThat(yaml).contains("simpleMap:");
        assertThat(yaml).contains("key1:");
        assertThat(yaml).contains("subConfig:");
        assertThat(yaml).contains("custom-key-field:"); // Note: uses custom key, not field name
        assertThat(yaml).doesNotContain("excludedField"); // Excluded field should not appear
    }
}
