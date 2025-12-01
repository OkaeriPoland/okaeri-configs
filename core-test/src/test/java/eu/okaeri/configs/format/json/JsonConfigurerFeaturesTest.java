package eu.okaeri.configs.format.json;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.json.gson.JsonGsonConfigurer;
import eu.okaeri.configs.json.jackson.JsonJacksonConfigurer;
import eu.okaeri.configs.json.simple.JsonSimpleConfigurer;
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
 * Generalized feature tests for all JSON configurer implementations.
 * Tests common functionality across JSON-GSON and JSON-Simple configurers.
 * 
 * Note: JSON format does NOT support comments or headers, so those tests are excluded.
 */
class JsonConfigurerFeaturesTest {

    static Stream<Arguments> jsonConfigurers() {
        return Stream.of(
            Arguments.of("JSON-GSON", new JsonGsonConfigurer()),
            Arguments.of("JSON-Jackson", new JsonJacksonConfigurer()),
            Arguments.of("JSON-Simple", new JsonSimpleConfigurer())
        );
    }

    @ParameterizedTest(name = "{0}: Load from string")
    @MethodSource("jsonConfigurers")
    void testLoad_FromString(String configurerName, Configurer configurer) throws Exception {
        // Given: JSON content as string
        String json = """
            {
              "name": "Test Config",
              "value": 42,
              "enabled": true
            }
            """;

        // When: Load into config
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.setConfigurer(configurer);
        config.load(json);

        // Then: Values are loaded correctly
        assertThat(config.getName()).isEqualTo("Test Config");
        assertThat(config.getValue()).isEqualTo(42);
        assertThat(config.isEnabled()).isTrue();
    }

    @ParameterizedTest(name = "{0}: Key ordering")
    @MethodSource("jsonConfigurers")
    void testWrite_KeyOrdering(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with multiple fields
        String json = """
            {
              "firstField": "first",
              "secondField": "second",
              "thirdField": "third",
              "fourthField": "fourth"
            }
            """;

        OrderedConfig config = ConfigManager.create(OrderedConfig.class);
        config.setConfigurer(configurer);
        config.load(json);

        // When: Write to string
        String resultJson = config.saveToString();

        // Then: Key ordering is preserved (LinkedHashMap behavior)
        int firstPos = resultJson.indexOf("\"firstField\"");
        int secondPos = resultJson.indexOf("\"secondField\"");
        int thirdPos = resultJson.indexOf("\"thirdField\"");
        int fourthPos = resultJson.indexOf("\"fourthField\"");

        assertThat(firstPos).isLessThan(secondPos);
        assertThat(secondPos).isLessThan(thirdPos);
        assertThat(thirdPos).isLessThan(fourthPos);
    }

    @ParameterizedTest(name = "{0}: Load from InputStream")
    @MethodSource("jsonConfigurers")
    void testLoadFromInputStream_PopulatesInternalMap(String configurerName, Configurer configurer) throws Exception {
        // Given: JSON content as InputStream
        String json = """
            {
              "name": "Test Config",
              "value": 42,
              "enabled": true
            }
            """;

        // When: Load from InputStream
        TestConfig config = ConfigManager.create(TestConfig.class);
        configurer.load(new ByteArrayInputStream(json.getBytes()), config.getDeclaration());

        // Then: Internal map is populated correctly
        assertThat(configurer.getValue("name")).isEqualTo("Test Config");
        assertThat(configurer.getValue("value")).isIn(42, 42.0, 42L);
        assertThat(configurer.getValue("enabled")).isEqualTo(true);
        assertThat(configurer.getAllKeys()).contains("name", "value", "enabled");
    }

    @ParameterizedTest(name = "{0}: Set/get value operations")
    @MethodSource("jsonConfigurers")
    void testSetValueGetValue_InternalMapOperations(String configurerName, Configurer configurer) {
        // Given: Fresh configurer
        TestConfig config = ConfigManager.create(TestConfig.class);

        // When: Set values using configurer API
        configurer.setValue("key1", "value1", null, null);
        configurer.setValue("key2", 123, null, null);
        configurer.setValueUnsafe("key3", true);

        // Then: Values are retrievable from internal map
        assertThat(configurer.getValue("key1")).isEqualTo("value1");
        assertThat(configurer.getValue("key2")).isEqualTo(123);
        assertThat(configurer.getValue("key3")).isEqualTo(true);
        assertThat(configurer.keyExists("key1")).isTrue();
        assertThat(configurer.keyExists("nonexistent")).isFalse();
    }

    @ParameterizedTest(name = "{0}: Remove key operation")
    @MethodSource("jsonConfigurers")
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
    @MethodSource("jsonConfigurers")
    void testRoundTrip_MaintainsStructure(String configurerName, Configurer configurer, @TempDir Path tempDir) throws Exception {
        // Given: Config with various field types
        String originalJson = """
            {
              "name": "Test Config",
              "enabled": true,
              "count": 42,
              "items": ["alpha", "beta", "gamma"],
              "settings": {
                "timeout": 30,
                "retries": 3
              }
            }
            """;

        TestConfigWithStructure config = ConfigManager.create(TestConfigWithStructure.class);
        config.setConfigurer(configurer);
        config.load(originalJson);

        // When: Save and load again
        Path file = tempDir.resolve("test.json");
        config.save(file);
        String savedJson = Files.readString(file);

        // Then: Structure is maintained
        assertThat(savedJson).contains("\"name\":");
        assertThat(savedJson).contains("\"enabled\":");
        assertThat(savedJson).contains("\"count\":");
        assertThat(savedJson).contains("\"items\":");
        assertThat(savedJson).contains("\"alpha\"");
        assertThat(savedJson).contains("\"beta\"");
        assertThat(savedJson).contains("\"gamma\"");
        assertThat(savedJson).contains("\"settings\":");
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
