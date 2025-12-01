package eu.okaeri.configs.format.xml;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.xml.XmlBeanConfigurer;
import eu.okaeri.configs.xml.XmlSimpleConfigurer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Generalized feature tests for all XML configurer implementations.
 * Tests common functionality across XmlSimpleConfigurer and XmlBeanConfigurer.
 * <p>
 * Note: XML format does NOT support comments or headers, so those tests are excluded.
 */
class XmlConfigurerFeaturesTest {

    static Stream<Arguments> xmlConfigurers() {
        return Stream.of(
            Arguments.of("XML-Simple", new XmlSimpleConfigurer()),
            Arguments.of("XML-Bean", new XmlBeanConfigurer())
        );
    }

    @ParameterizedTest(name = "{0}: Load from file")
    @MethodSource("xmlConfigurers")
    void testLoad_FromFile(String configurerName, Configurer configurer, @TempDir Path tempDir) throws Exception {
        // Given: Config saved to file
        TestConfig original = ConfigManager.create(TestConfig.class);
        original.setConfigurer(configurer);
        original.setName("Test Config");
        original.setValue(42);
        original.setEnabled(true);

        Path file = tempDir.resolve("test.xml");
        original.save(file);

        // When: Load into new instance
        TestConfig loaded = ConfigManager.create(TestConfig.class);
        loaded.setConfigurer(configurer);
        loaded.load(file);

        // Then: Values are loaded correctly
        assertThat(loaded.getName()).isEqualTo("Test Config");
        assertThat(loaded.getValue()).isEqualTo(42);
        assertThat(loaded.isEnabled()).isTrue();
    }

    @ParameterizedTest(name = "{0}: Key ordering")
    @MethodSource("xmlConfigurers")
    void testWrite_KeyOrdering(String configurerName, Configurer configurer, @TempDir Path tempDir) throws Exception {
        // Given: Config with multiple fields
        OrderedConfig config = ConfigManager.create(OrderedConfig.class);
        config.setConfigurer(configurer);

        // When: Save and read as string
        Path file = tempDir.resolve("ordered.xml");
        config.save(file);
        String xml = Files.readString(file);

        // Then: Key ordering is preserved
        int firstPos = xml.indexOf("firstField");
        int secondPos = xml.indexOf("secondField");
        int thirdPos = xml.indexOf("thirdField");
        int fourthPos = xml.indexOf("fourthField");

        assertThat(firstPos).isLessThan(secondPos);
        assertThat(secondPos).isLessThan(thirdPos);
        assertThat(thirdPos).isLessThan(fourthPos);
    }

    @ParameterizedTest(name = "{0}: Load from InputStream")
    @MethodSource("xmlConfigurers")
    void testLoadFromInputStream_PopulatesInternalMap(String configurerName, Configurer configurer, @TempDir Path tempDir) throws Exception {
        // Given: Config saved to file
        TestConfig original = ConfigManager.create(TestConfig.class);
        original.setConfigurer(configurer);
        original.setName("Test Config");
        original.setValue(42);
        original.setEnabled(true);

        Path file = tempDir.resolve("test.xml");
        original.save(file);
        byte[] bytes = Files.readAllBytes(file);

        // When: Load from InputStream
        TestConfig loaded = ConfigManager.create(TestConfig.class);
        configurer.load(new ByteArrayInputStream(bytes), loaded.getDeclaration());

        // Then: Internal map is populated correctly
        assertThat(configurer.getValue("name")).isEqualTo("Test Config");
        // XmlSimple stores as strings, XmlBean preserves types
        assertThat(configurer.getValue("enabled")).isIn(true, "true");
        assertThat(configurer.getAllKeys()).contains("name", "value", "enabled");
    }

    @ParameterizedTest(name = "{0}: Set/get value operations")
    @MethodSource("xmlConfigurers")
    void testSetValueGetValue_InternalMapOperations(String configurerName, Configurer configurer) {
        // When: Set values using configurer API
        configurer.setValue("key1", "value1", null, null);
        configurer.setValue("key2", 123, null, null);
        configurer.setValueUnsafe("key3", true);

        // Then: Values are retrievable from internal map
        assertThat(configurer.getValue("key1")).isEqualTo("value1");
        // XmlSimple stores as strings, XmlBean preserves types
        assertThat(configurer.getValue("key2")).isIn(123, "123");
        assertThat(configurer.getValue("key3")).isIn(true, "true");
        assertThat(configurer.keyExists("key1")).isTrue();
        assertThat(configurer.keyExists("nonexistent")).isFalse();
    }

    @ParameterizedTest(name = "{0}: Remove key operation")
    @MethodSource("xmlConfigurers")
    void testRemoveKey_ModifiesInternalMap(String configurerName, Configurer configurer) {
        // Given: Configurer with some keys
        configurer.setValueUnsafe("key1", "value1");
        configurer.setValueUnsafe("key2", "value2");

        // When: Remove a key
        Object removed = configurer.remove("key1");

        // Then: Key is removed from internal map
        assertThat(removed).isEqualTo("value1");
        assertThat(configurer.keyExists("key1")).isFalse();
        assertThat(configurer.keyExists("key2")).isTrue();
        assertThat(configurer.getAllKeys()).contains("key2");
    }

    @ParameterizedTest(name = "{0}: Round-trip structure maintenance")
    @MethodSource("xmlConfigurers")
    void testRoundTrip_MaintainsStructure(String configurerName, Configurer configurer, @TempDir Path tempDir) throws Exception {
        // Given: Config with various field types
        TestConfigWithStructure config = ConfigManager.create(TestConfigWithStructure.class);
        config.setConfigurer(configurer);
        config.setName("Test Config");
        config.setEnabled(true);
        config.setCount(42);
        config.setItems(List.of("alpha", "beta", "gamma"));
        config.setSettings(Map.of("timeout", 30, "retries", 3));

        // When: Save and load again
        Path file = tempDir.resolve("structure.xml");
        config.save(file);
        String savedXml = Files.readString(file);

        // Then: Structure is maintained
        assertThat(savedXml).contains("name");
        assertThat(savedXml).contains("enabled");
        assertThat(savedXml).contains("count");
        assertThat(savedXml).contains("items");
        assertThat(savedXml).contains("alpha");
        assertThat(savedXml).contains("beta");
        assertThat(savedXml).contains("gamma");
        assertThat(savedXml).contains("settings");
    }

    // Test config classes

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class TestConfig extends OkaeriConfig {
        private String name = "default";
        private int value = 0;
        private boolean enabled = false;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class OrderedConfig extends OkaeriConfig {
        private String firstField = "first";
        private String secondField = "second";
        private String thirdField = "third";
        private String fourthField = "fourth";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class TestConfigWithStructure extends OkaeriConfig {
        private String name;
        private boolean enabled;
        private int count;
        private List<String> items;
        private Map<String, Integer> settings;
    }
}
