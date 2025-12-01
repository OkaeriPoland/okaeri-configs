package eu.okaeri.configs.format.xml;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.test.GoldenFileAssertion;
import eu.okaeri.configs.test.MegaConfig;
import eu.okaeri.configs.xml.XmlBeanConfigurer;
import eu.okaeri.configs.xml.XmlSimpleConfigurer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Generalized E2E tests for all XML configurer implementations using MegaConfig.
 * Tests both XmlSimpleConfigurer and XmlBeanConfigurer with parameterized tests.
 * <p>
 * Note: XML format does NOT support comments or headers, so MegaConfig tests
 * focus on data integrity and structure preservation only.
 */
class XmlConfigurerMegaConfigTest {

    static Stream<Arguments> xmlConfigurers() {
        return Stream.of(
            Arguments.of("XML-Simple", new XmlSimpleConfigurer(), "../xml/src/test/resources/e2e-simple.xml"),
            Arguments.of("XML-Bean", new XmlBeanConfigurer(), "../xml/src/test/resources/e2e-bean.xml")
        );
    }

    @ParameterizedTest(name = "{0}: Save MegaConfig to string")
    @MethodSource("xmlConfigurers")
    void testMegaConfig_SaveToString(String configurerName, Configurer configurer, String goldenFilePath) throws Exception {
        // Given: MegaConfig with all features
        MegaConfig config = ConfigManager.create(MegaConfig.class);
        config.setConfigurer(configurer);
        config.populateNestedMegaConfig(); // Initialize 2-level deep nesting

        // When: Save to string
        String xml = config.saveToString();

        // Then: XML is generated successfully
        assertThat(xml).isNotNull();
        assertThat(xml).isNotEmpty();
        assertThat(xml).contains("primBool");
        assertThat(xml).contains("nestedMegaConfig");
    }

    @ParameterizedTest(name = "{0}: Round-trip MegaConfig")
    @MethodSource("xmlConfigurers")
    void testMegaConfig_RoundTrip(String configurerName, Configurer configurer, String goldenFilePath) throws Exception {
        // Given: MegaConfig saved to XML
        MegaConfig original = ConfigManager.create(MegaConfig.class);
        original.setConfigurer(configurer);
        original.populateNestedMegaConfig(); // Initialize 2-level deep nesting
        String xml = original.saveToString();

        // When: Load into new instance
        MegaConfig loaded = ConfigManager.create(MegaConfig.class);
        loaded.setConfigurer(configurer);
        loaded.load(xml);

        // Then: Both configs produce identical XML output
        String reserializedXml = loaded.saveToString();
        assertThat(reserializedXml).isEqualTo(xml);
    }

    @ParameterizedTest(name = "{0}: Load MegaConfig with nested structure")
    @MethodSource("xmlConfigurers")
    void testMegaConfig_LoadNestedStructure(String configurerName, Configurer configurer, String goldenFilePath) throws Exception {
        // Given: MegaConfig with nested structure saved to XML
        MegaConfig original = ConfigManager.create(MegaConfig.class);
        original.setConfigurer(configurer);
        original.populateNestedMegaConfig();
        String xml = original.saveToString();

        // When: Load into new instance
        MegaConfig loaded = ConfigManager.create(MegaConfig.class);
        loaded.setConfigurer(configurer);
        loaded.load(xml);

        // Then: Nested structure is loaded correctly
        assertThat(loaded.getNestedMegaConfig()).isNotNull();
        assertThat(loaded.getNestedMegaConfig().getWrapBool()).isFalse();
        assertThat(loaded.getNestedMegaConfig().getSimpleString()).isEqualTo("Hello, World!");
        // The nested config's nestedMegaConfig should be null (2 levels deep only)
        assertThat(loaded.getNestedMegaConfig().getNestedMegaConfig()).isNull();
    }

    @ParameterizedTest(name = "{0}: Load MegaConfig from golden file")
    @MethodSource("xmlConfigurers")
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
        // The fact that we got here without exception means the golden file is valid XML
        assertThat(config).isNotNull();
    }

    @ParameterizedTest(name = "{0}: Regression test with golden file")
    @MethodSource("xmlConfigurers")
    void testMegaConfig_RegressionTest(String configurerName, Configurer configurer, String goldenFilePath) throws Exception {
        // Given: MegaConfig with all features
        MegaConfig config = ConfigManager.create(MegaConfig.class);
        config.setConfigurer(configurer);
        config.populateNestedMegaConfig();

        // When: Save to string
        String currentXml = config.saveToString();

        // Then: Compare with golden file (or create it on first run)
        // Normalize Java version in XMLEncoder output (e.g., version="21.0.7" -> version="VERSION")
        GoldenFileAssertion.forFile(goldenFilePath)
            .withContent(currentXml)
            .withNormalizer(s -> s.replaceAll("version=\"[0-9._]+\"", "version=\"VERSION\""))
            .assertMatches();
    }
}
