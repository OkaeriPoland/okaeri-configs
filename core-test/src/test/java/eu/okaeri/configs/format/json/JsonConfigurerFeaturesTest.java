package eu.okaeri.configs.format.json;

import com.google.gson.Gson;
import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.json.gson.JsonGsonConfigurer;
import eu.okaeri.configs.json.jackson.JsonJacksonConfigurer;
import eu.okaeri.configs.json.simple.JsonSimpleConfigurer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.json.simple.parser.JSONParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
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

    // ==================== JsonGsonConfigurer Constructor Tests ====================

    @Test
    void testJsonGson_DefaultConstructor() {
        JsonGsonConfigurer configurer = new JsonGsonConfigurer();
        assertThat(configurer).isNotNull();
    }

    @Test
    void testJsonGson_ConstructorWithGson() {
        Gson gson = new Gson();
        JsonGsonConfigurer configurer = new JsonGsonConfigurer(gson);
        assertThat(configurer).isNotNull();
    }


    @Test
    void testJsonGson_GetExtensions() {
        JsonGsonConfigurer configurer = new JsonGsonConfigurer();
        assertThat(configurer.getExtensions()).containsExactly("json");
    }

    // ==================== JsonSimpleConfigurer Constructor Tests ====================

    @Test
    void testJsonSimple_DefaultConstructor() {
        JsonSimpleConfigurer configurer = new JsonSimpleConfigurer();
        assertThat(configurer).isNotNull();
    }

    @Test
    void testJsonSimple_ConstructorWithParser() {
        JSONParser parser = new JSONParser();
        JsonSimpleConfigurer configurer = new JsonSimpleConfigurer(parser);
        assertThat(configurer).isNotNull();
    }


    @Test
    void testJsonSimple_GetExtensions() {
        JsonSimpleConfigurer configurer = new JsonSimpleConfigurer();
        assertThat(configurer.getExtensions()).containsExactly("json");
    }

    // ==================== Test Config Classes ====================

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
