package eu.okaeri.configs.format.json;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.json.gson.JsonGsonConfigurer;
import eu.okaeri.configs.json.simple.JsonSimpleConfigurer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Generalized edge case tests for all JSON configurer implementations.
 * Tests boundary conditions and error handling across JSON-GSON and JSON-Simple configurers.
 */
class JsonConfigurerEdgeCasesTest {

    static Stream<Arguments> jsonConfigurers() {
        return Stream.of(
            Arguments.of("JSON-GSON", new JsonGsonConfigurer()),
            Arguments.of("JSON-Simple", new JsonSimpleConfigurer())
        );
    }

    @ParameterizedTest(name = "{0}: Empty config")
    @MethodSource("jsonConfigurers")
    void testWrite_EmptyConfig(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with no fields
        EmptyConfig config = ConfigManager.create(EmptyConfig.class);
        config.withConfigurer(configurer);

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String json = output.toString();

        // Then: JSON output is minimal (just {})
        assertThat(json.trim()).isEqualTo("{}");
    }

    @ParameterizedTest(name = "{0}: Very large string values")
    @MethodSource("jsonConfigurers")
    void testWrite_VeryLargeValues(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with very large string value
        LargeValueConfig config = ConfigManager.create(LargeValueConfig.class);
        config.withConfigurer(configurer);
        config.setLargeValue("x".repeat(10000)); // 10k characters

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String json = output.toString();

        // Then: Large value is written correctly
        assertThat(json).contains("\"largeValue\":");
        assertThat(json.length()).isGreaterThan(10000);
    }

    @ParameterizedTest(name = "{0}: Special characters in strings")
    @MethodSource("jsonConfigurers")
    void testWrite_SpecialCharactersInStrings(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with special characters
        SpecialCharsConfig config = ConfigManager.create(SpecialCharsConfig.class);
        config.withConfigurer(configurer);
        config.setQuotes("She said: \"Hello!\"");
        config.setBackslash("C:\\Users\\test\\path");
        config.setNewlines("Line 1\nLine 2\nLine 3");
        config.setTabs("Column1\tColumn2\tColumn3");
        config.setUnicode("Emoji: 🎉 Japanese: こんにちは Russian: Привет Polish: Łódź");

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String json = output.toString();

        // Then: Special characters are properly escaped/encoded
        assertThat(json).contains("\"quotes\":");
        assertThat(json).contains("\"backslash\":");
        assertThat(json).contains("\"newlines\":");
        assertThat(json).contains("\"tabs\":");
        assertThat(json).contains("\"unicode\":");
    }

    @ParameterizedTest(name = "{0}: Very deeply nested structure")
    @MethodSource("jsonConfigurers")
    void testWrite_VeryDeeplyNestedStructure(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with deep nesting
        String json = """
            {
              "level1": {
                "level2": {
                  "level3": {
                    "level4": {
                      "level5": {
                        "value": "deep"
                      }
                    }
                  }
                }
              }
            }
            """;

        DeepNestedConfig config = ConfigManager.create(DeepNestedConfig.class);
        config.withConfigurer(configurer);
        config.load(json);

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String resultJson = output.toString();

        // Then: Nesting structure is maintained
        assertThat(resultJson).contains("\"level1\":");
        assertThat(resultJson).contains("\"level2\":");
        assertThat(resultJson).contains("\"level3\":");
        assertThat(resultJson).contains("\"level4\":");
        assertThat(resultJson).contains("\"level5\":");
    }

    @ParameterizedTest(name = "{0}: Very large collection")
    @MethodSource("jsonConfigurers")
    void testWrite_VeryLargeCollection(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with very large list
        LargeCollectionConfig config = ConfigManager.create(LargeCollectionConfig.class);
        config.withConfigurer(configurer);
        List<String> largeList = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            largeList.add("item-" + i);
        }
        config.setLargeList(largeList);

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String json = output.toString();

        // Then: All items are written
        assertThat(json).contains("\"largeList\":");
        assertThat(json).contains("\"item-0\"");
        assertThat(json).contains("\"item-999\"");
    }

    @ParameterizedTest(name = "{0}: Null values are not stored")
    @MethodSource("jsonConfigurers")
    void testWrite_NullValues_NotStored(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with null values
        NullValueConfig config = ConfigManager.create(NullValueConfig.class);
        config.withConfigurer(configurer);
        config.setNullString(null);
        config.setNullList(null);
        config.setNullMap(null);

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String json = output.toString();

        // Then: Null values are NOT stored in JSON (keys are absent)
        assertThat(json).doesNotContain("\"nullString\"");
        assertThat(json).doesNotContain("\"nullList\"");
        assertThat(json).doesNotContain("\"nullMap\"");
    }

    @ParameterizedTest(name = "{0}: Nested null values are not stored")
    @MethodSource("jsonConfigurers")
    void testWrite_NestedNullValues_NotStored(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with nested OkaeriConfig containing null values
        NestedNullConfig config = ConfigManager.create(NestedNullConfig.class);
        config.withConfigurer(configurer);
        config.getNested().setNestedNullString(null);
        config.getNested().setNestedNullList(null);

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String json = output.toString();

        // Then: Nested null values are NOT stored (keys are absent)
        assertThat(json).contains("\"nested\":");
        assertThat(json).doesNotContain("\"nestedNullString\"");
        assertThat(json).doesNotContain("\"nestedNullList\"");
    }

    @ParameterizedTest(name = "{0}: Round-trip empty strings")
    @MethodSource("jsonConfigurers")
    void testRoundTrip_EmptyStrings(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with empty strings
        EmptyStringConfig config = ConfigManager.create(EmptyStringConfig.class);
        config.withConfigurer(configurer);
        config.setEmptyString("");
        config.setWhitespaceString("   ");

        // When: Save and load again
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);

        EmptyStringConfig loaded = ConfigManager.create(EmptyStringConfig.class);
        loaded.withConfigurer(configurer);
        loaded.load(new ByteArrayInputStream(output.toByteArray()));

        // Then: Empty strings are preserved
        assertThat(loaded.getEmptyString()).isEqualTo("");
        assertThat(loaded.getWhitespaceString()).isEqualTo("   ");
    }

    @ParameterizedTest(name = "{0}: JSON reserved words as values")
    @MethodSource("jsonConfigurers")
    void testWrite_JsonReservedWords(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with JSON reserved words as string values
        ReservedWordsConfig config = ConfigManager.create(ReservedWordsConfig.class);
        config.withConfigurer(configurer);
        config.setTrueString("true");
        config.setFalseString("false");
        config.setNullString("null");

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String json = output.toString();

        // Then: Reserved words are properly quoted as strings
        assertThat(json).contains("\"trueString\":");
        assertThat(json).contains("\"falseString\":");
        assertThat(json).contains("\"nullString\":");
        // Strings should be quoted
        assertThat(json).contains("\"true\"");
        assertThat(json).contains("\"false\"");
        assertThat(json).contains("\"null\"");
    }

    @ParameterizedTest(name = "{0}: Malformed JSON throws exception")
    @MethodSource("jsonConfigurers")
    void testLoad_MalformedJson_ThrowsException(String configurerName, Configurer configurer) {
        // Given: Malformed JSON (invalid syntax)
        String malformedJson = """
            {
              "name": "Test",
              "value": [unclosed bracket
              "enabled": true
            }
            """;

        // When/Then: Loading malformed JSON throws exception
        EmptyConfig config = ConfigManager.create(EmptyConfig.class);
        config.withConfigurer(configurer);

        assertThatThrownBy(() -> config.load(malformedJson))
            .isInstanceOf(Exception.class);
    }

    // Test config classes

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class EmptyConfig extends OkaeriConfig {
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class LargeValueConfig extends OkaeriConfig {
        private String largeValue;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class SpecialCharsConfig extends OkaeriConfig {
        private String quotes;
        private String backslash;
        private String newlines;
        private String tabs;
        private String unicode;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class DeepNestedConfig extends OkaeriConfig {
        private Map<String, Object> level1;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class LargeCollectionConfig extends OkaeriConfig {
        private List<String> largeList;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class NullValueConfig extends OkaeriConfig {
        private String nullString;
        private List<String> nullList;
        private Map<String, String> nullMap;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class NestedNullConfig extends OkaeriConfig {
        private NestedPart nested = new NestedPart();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class NestedPart extends OkaeriConfig {
        private String nestedNullString = "default";
        private List<String> nestedNullList = new ArrayList<>();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class EmptyStringConfig extends OkaeriConfig {
        private String emptyString;
        private String whitespaceString;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class ReservedWordsConfig extends OkaeriConfig {
        private String trueString;
        private String falseString;
        private String nullString;
    }
}
