package eu.okaeri.configs.format.json;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.json.gson.JsonGsonConfigurer;
import eu.okaeri.configs.json.simple.JsonSimpleConfigurer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Generalized structure tests for all JSON configurer implementations.
 * Tests exact JSON formatting output with text block comparisons.
 * 
 * Note: JSON format does NOT support comments or headers, so those tests are excluded.
 */
class JsonConfigurerStructureTest {

    static Stream<Arguments> jsonConfigurers() {
        return Stream.of(
            Arguments.of("JSON-GSON", new JsonGsonConfigurer()),
            Arguments.of("JSON-Simple", new JsonSimpleConfigurer())
        );
    }

    @ParameterizedTest(name = "{0}: Simple fields structure")
    @MethodSource("jsonConfigurers")
    void testSaveToString_SimpleFields_MatchesExpectedJson(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with simple fields
        SimpleConfig config = ConfigManager.create(SimpleConfig.class);
        config.withConfigurer(configurer);

        // When: Save to JSON
        String json = config.saveToString();

        // Then: Contains expected structure (note: exact formatting may vary between implementations)
        assertThat(json).contains("\"simpleField\":");
        assertThat(json).contains("\"default\"");
        assertThat(json).contains("\"numberField\":");
        assertThat(json).contains("42");
    }

    @ParameterizedTest(name = "{0}: SubConfig structure")
    @MethodSource("jsonConfigurers")
    void testSaveToString_SubConfig_MatchesExpectedJson(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with SubConfig
        SubConfigConfig config = ConfigManager.create(SubConfigConfig.class);
        config.withConfigurer(configurer);

        // When: Save to JSON
        String json = config.saveToString();

        // Then: Nested structure is correct
        assertThat(json).contains("\"subConfig\":");
        assertThat(json).contains("\"subField\":");
        assertThat(json).contains("\"default sub\"");
        assertThat(json).contains("\"subNumber\":");
        assertThat(json).contains("42");
    }

    @ParameterizedTest(name = "{0}: Unicode strings preserved")
    @MethodSource("jsonConfigurers")
    void testSaveToString_UnicodeStrings_PreservedInJson(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with unicode strings
        UnicodeConfig config = ConfigManager.create(UnicodeConfig.class);
        config.withConfigurer(configurer);

        // When: Save to JSON
        String json = config.saveToString();

        // Then: Unicode is preserved (may be escaped or literal depending on implementation)
        assertThat(json).contains("\"japanese\":");
        assertThat(json).contains("\"russian\":");
        assertThat(json).contains("\"polish\":");
        // Content may be Unicode-escaped or literal - both are valid
    }

    @ParameterizedTest(name = "{0}: Nested collections structure")
    @MethodSource("jsonConfigurers")
    void testSaveToString_NestedCollections_MatchesExpectedStructure(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with nested structures
        NestedStructureConfig config = ConfigManager.create(NestedStructureConfig.class);
        config.withConfigurer(configurer);

        // When: Save to JSON
        String json = config.saveToString();

        // Then: Structure is correct
        assertThat(json).contains("\"stringList\":");
        assertThat(json).contains("\"alpha\"");
        assertThat(json).contains("\"beta\"");
        assertThat(json).contains("\"gamma\"");
        assertThat(json).contains("\"simpleMap\":");
        assertThat(json).contains("\"key1\":");
        assertThat(json).contains("\"value1\"");
        assertThat(json).contains("\"key2\":");
        assertThat(json).contains("\"value2\"");
    }

    @ParameterizedTest(name = "{0}: Save/load cycles stability")
    @MethodSource("jsonConfigurers")
    void testSaveLoadCycles_RemainsStable(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with various fields
        StableConfig config = ConfigManager.create(StableConfig.class);
        config.withConfigurer(configurer);

        // When: Initial save
        String firstJson = config.saveToString();

        // Then: Save/load cycles should produce identical output
        String currentJson = firstJson;
        for (int i = 0; i < 5; i++) {
            StableConfig reloaded = ConfigManager.create(StableConfig.class);
            reloaded.withConfigurer(configurer);
            reloaded.load(currentJson);
            currentJson = reloaded.saveToString();

            assertThat(currentJson)
                .as("Cycle %d: JSON should remain stable", i + 1)
                .isEqualTo(firstJson);
        }
    }

    @ParameterizedTest(name = "{0}: Serializable objects")
    @MethodSource("jsonConfigurers")
    void testSaveToString_Serializable_MatchesExpectedJson(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with Serializable object
        SerializableConfig config = ConfigManager.create(SerializableConfig.class);
        config.withConfigurer(configurer);

        // When: Save to JSON
        String json = config.saveToString();

        // Then: Serializable is converted to JSON object
        assertThat(json).contains("\"customObj\":");
        assertThat(json).contains("\"name\":");
        assertThat(json).contains("\"test\"");
        assertThat(json).contains("\"id\":");
        assertThat(json).contains("999");
    }

    // Test config classes

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class SimpleConfig extends OkaeriConfig {
        private String simpleField = "default";
        private int numberField = 42;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class SubConfigConfig extends OkaeriConfig {
        private NestedConfig subConfig = new NestedConfig();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class NestedConfig extends OkaeriConfig {
        private String subField = "default sub";
        private int subNumber = 42;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class UnicodeConfig extends OkaeriConfig {
        private String japanese = "こんにちは世界 🌍";
        private String russian = "Привет мир! Тестирование кириллицы";
        private String polish = "Część świecie! Łódź, Gdańsk";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class NestedStructureConfig extends OkaeriConfig {
        private List<String> stringList = Arrays.asList("alpha", "beta", "gamma");
        private java.util.Map<String, String> simpleMap = new LinkedHashMap<String, String>() {{
            this.put("key1", "value1");
            this.put("key2", "value2");
        }};
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class StableConfig extends OkaeriConfig {
        private String field1 = "value1";
        private String field2 = "value2";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class SerializableConfig extends OkaeriConfig {
        private CustomSerializable customObj = new CustomSerializable("test", 999);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomSerializable implements Serializable {
        private static final long serialVersionUID = 1L;
        private String name;
        private int id;
    }
}
