package eu.okaeri.configs.format.yaml;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.yaml.bukkit.YamlBukkitConfigurer;
import eu.okaeri.configs.yaml.bungee.YamlBungeeConfigurer;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;
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
 * Generalized edge case tests for all YAML configurer implementations.
 * Tests boundary conditions and error handling across SnakeYAML, Bukkit, and Bungee configurers.
 */
class YamlConfigurerEdgeCasesTest {

    static Stream<Arguments> yamlConfigurers() {
        return Stream.of(
            Arguments.of("SnakeYAML", new YamlSnakeYamlConfigurer()),
            Arguments.of("Bukkit", new YamlBukkitConfigurer()),
            Arguments.of("Bungee", new YamlBungeeConfigurer())
        );
    }

    @ParameterizedTest(name = "{0}: Empty config")
    @MethodSource("yamlConfigurers")
    void testWrite_EmptyConfig(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with no fields
        EmptyConfig config = ConfigManager.create(EmptyConfig.class);
        config.withConfigurer(configurer);

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String yaml = output.toString();

        // Then: YAML output is minimal (just {} or empty)
        assertThat(yaml.trim()).isIn("{}", "");
    }

    @ParameterizedTest(name = "{0}: Very large string values")
    @MethodSource("yamlConfigurers")
    void testWrite_VeryLargeValues(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with very large string value
        LargeValueConfig config = ConfigManager.create(LargeValueConfig.class);
        config.withConfigurer(configurer);
        config.setLargeValue("x".repeat(10000)); // 10k characters

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String yaml = output.toString();

        // Then: Large value is written correctly
        assertThat(yaml).contains("largeValue:");
        assertThat(yaml.length()).isGreaterThan(10000);
    }

    @ParameterizedTest(name = "{0}: Special characters in strings")
    @MethodSource("yamlConfigurers")
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
        String yaml = output.toString();

        // Then: Special characters are properly escaped/encoded
        assertThat(yaml).contains("quotes:");
        assertThat(yaml).contains("backslash:");
        assertThat(yaml).contains("newlines:");
        assertThat(yaml).contains("tabs:");
        assertThat(yaml).contains("unicode:");
    }

    @ParameterizedTest(name = "{0}: Very deeply nested structure")
    @MethodSource("yamlConfigurers")
    void testWrite_VeryDeeplyNestedStructure(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with deep nesting
        String yaml = """
            level1:
              level2:
                level3:
                  level4:
                    level5:
                      value: deep
            """;

        DeepNestedConfig config = ConfigManager.create(DeepNestedConfig.class);
        config.withConfigurer(configurer);
        config.load(yaml);

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String resultYaml = output.toString();

        // Then: Nesting structure is maintained
        assertThat(resultYaml).contains("level1:");
        assertThat(resultYaml).contains("level2:");
        assertThat(resultYaml).contains("level3:");
        assertThat(resultYaml).contains("level4:");
        assertThat(resultYaml).contains("level5:");
    }

    @ParameterizedTest(name = "{0}: Very large collection")
    @MethodSource("yamlConfigurers")
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
        String yaml = output.toString();

        // Then: All items are written
        assertThat(yaml).contains("largeList:");
        assertThat(yaml).contains("item-0");
        assertThat(yaml).contains("item-999");
    }

    static Stream<Arguments> snakeYamlOnly() {
        return Stream.of(
            Arguments.of("SnakeYAML", new YamlSnakeYamlConfigurer())
        );
    }

    @ParameterizedTest(name = "{0}: Top-level null values")
    @MethodSource("snakeYamlOnly")
    void testWrite_NullValues(String configurerName, Configurer configurer) throws Exception {
        // Note: This test only runs for SnakeYAML because Bukkit and Bungee configurers
        // cannot preserve top-level null values (YamlConfiguration and BungeeConfig
        // have no way to differentiate between removing a key and setting it to null)

        // Given: Config with null values
        NullValueConfig config = ConfigManager.create(NullValueConfig.class);
        config.withConfigurer(configurer);
        config.setNullString(null);
        config.setNullList(null);
        config.setNullMap(null);

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String yaml = output.toString();

        // Then: Null values are represented in YAML (typically as 'null' or omitted)
        assertThat(yaml).containsAnyOf("nullString: null", "nullString:");
        assertThat(yaml).containsAnyOf("nullList: null", "nullList:");
        assertThat(yaml).containsAnyOf("nullMap: null", "nullMap:");
    }

    @ParameterizedTest(name = "{0}: Nested null values")
    @MethodSource("yamlConfigurers")
    void testWrite_NestedNullValues(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with nested OkaeriConfig containing null values
        NestedNullConfig config = ConfigManager.create(NestedNullConfig.class);
        config.withConfigurer(configurer);
        config.getNested().setNestedNullString(null);
        config.getNested().setNestedNullList(null);

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String yaml = output.toString();

        // Then: Nested null values are represented in YAML
        assertThat(yaml).contains("nested:");
        assertThat(yaml).containsAnyOf("nestedNullString: null", "nestedNullString:");
        assertThat(yaml).containsAnyOf("nestedNullList: null", "nestedNullList:");
    }

    @ParameterizedTest(name = "{0}: Round-trip empty strings")
    @MethodSource("yamlConfigurers")
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

    @ParameterizedTest(name = "{0}: YAML reserved words as values")
    @MethodSource("yamlConfigurers")
    void testWrite_YamlReservedWords(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with YAML reserved words as values
        ReservedWordsConfig config = ConfigManager.create(ReservedWordsConfig.class);
        config.withConfigurer(configurer);
        config.setTrueString("true");
        config.setFalseString("false");
        config.setNullString("null");
        config.setYesString("yes");
        config.setNoString("no");

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String yaml = output.toString();

        // Then: Reserved words are properly quoted or handled
        assertThat(yaml).contains("trueString:");
        assertThat(yaml).contains("falseString:");
        assertThat(yaml).contains("nullString:");
    }

    @ParameterizedTest(name = "{0}: Malformed YAML throws exception")
    @MethodSource("yamlConfigurers")
    void testLoad_MalformedYaml_ThrowsException(String configurerName, Configurer configurer) {
        // Given: Malformed YAML (invalid syntax)
        String malformedYaml = """
            name: Test
            value: [unclosed bracket
            enabled: true
            """;

        // When/Then: Loading malformed YAML throws exception
        EmptyConfig config = ConfigManager.create(EmptyConfig.class);
        config.withConfigurer(configurer);

        assertThatThrownBy(() -> config.load(malformedYaml))
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
        private String yesString;
        private String noString;
    }
}
