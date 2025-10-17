package eu.okaeri.configs.format.hocon;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.hocon.lightbend.HoconLightbendConfigurer;
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
 * Generalized E2E tests for all HOCON configurer implementations using MegaConfig.
 * Tests all HOCON configurers with parameterized tests.
 * <p>
 * Currently only HoconLightbendConfigurer exists, but structured to support future implementations.
 */
class HoconConfigurerMegaConfigTest {

    static Stream<Arguments> hoconConfigurers() {
        return Stream.of(
            Arguments.of("Lightbend", new HoconLightbendConfigurer(), "../hocon-lightbend/src/test/resources/e2e.conf")
            // Future implementations can be added here
        );
    }

    @ParameterizedTest(name = "{0}: Save MegaConfig to string")
    @MethodSource("hoconConfigurers")
    void testMegaConfig_SaveToString(String configurerName, Configurer configurer, String goldenFilePath) throws Exception {
        // Given: MegaConfig with all features
        MegaConfig config = ConfigManager.create(MegaConfig.class);
        config.withConfigurer(configurer);
        config.populateNestedMegaConfig(); // Initialize 2-level deep nesting

        // When: Save to string
        String hocon = config.saveToString();

        // Then: HOCON is generated successfully
        assertThat(hocon).isNotNull();
        assertThat(hocon).isNotEmpty();
        assertThat(hocon).contains("primBool");
        assertThat(hocon).contains("nestedMegaConfig");
    }

    @ParameterizedTest(name = "{0}: Round-trip MegaConfig")
    @MethodSource("hoconConfigurers")
    void testMegaConfig_RoundTrip(String configurerName, Configurer configurer, String goldenFilePath) throws Exception {
        // Given: MegaConfig saved to HOCON
        MegaConfig original = ConfigManager.create(MegaConfig.class);
        original.withConfigurer(configurer);
        original.populateNestedMegaConfig(); // Initialize 2-level deep nesting
        String hocon = original.saveToString();

        // When: Load into new instance
        MegaConfig loaded = ConfigManager.create(MegaConfig.class);
        loaded.withConfigurer(configurer);
        loaded.load(hocon);

        // Then: Both configs produce identical HOCON output
        String reserializedHocon = loaded.saveToString();
        assertThat(reserializedHocon).isEqualTo(hocon);
    }

    @ParameterizedTest(name = "{0}: Load MegaConfig with nested structure")
    @MethodSource("hoconConfigurers")
    void testMegaConfig_LoadNestedStructure(String configurerName, Configurer configurer, String goldenFilePath) throws Exception {
        // Given: MegaConfig with nested structure saved to HOCON
        MegaConfig original = ConfigManager.create(MegaConfig.class);
        original.withConfigurer(configurer);
        original.populateNestedMegaConfig();
        String hocon = original.saveToString();

        // When: Load into new instance
        MegaConfig loaded = ConfigManager.create(MegaConfig.class);
        loaded.withConfigurer(configurer);
        loaded.load(hocon);

        // Then: Nested structure is loaded correctly
        assertThat(loaded.getNestedMegaConfig()).isNotNull();
        assertThat(loaded.getNestedMegaConfig().getWrapBool()).isFalse();
        assertThat(loaded.getNestedMegaConfig().getSimpleString()).isEqualTo("Hello, World!");
        // The nested config's nestedMegaConfig should be null (2 levels deep only)
        assertThat(loaded.getNestedMegaConfig().getNestedMegaConfig()).isNull();
    }

    @ParameterizedTest(name = "{0}: Load MegaConfig from golden file")
    @MethodSource("hoconConfigurers")
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
        // The fact that we got here without exception means the golden file is valid HOCON
        assertThat(config).isNotNull();
    }

    @ParameterizedTest(name = "{0}: Regression test with golden file")
    @MethodSource("hoconConfigurers")
    void testMegaConfig_RegressionTest(String configurerName, Configurer configurer, String goldenFilePath) throws Exception {
        // Given: MegaConfig with all features
        MegaConfig config = ConfigManager.create(MegaConfig.class);
        config.withConfigurer(configurer);
        config.populateNestedMegaConfig();

        // When: Save to string
        String currentHocon = config.saveToString();

        // Then: Compare with golden file (or create it on first run)
        GoldenFileAssertion.forFile(goldenFilePath)
            .withContent(currentHocon)
            .assertMatches();
    }
}
