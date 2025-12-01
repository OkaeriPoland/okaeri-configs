package eu.okaeri.configs.format.toml;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.toml.TomlJacksonConfigurer;
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
 * Edge case tests for TOML configurer implementations.
 * Tests boundary conditions and error handling.
 */
class TomlConfigurerEdgeCasesTest {

    static Stream<Arguments> tomlConfigurers() {
        return Stream.of(
            Arguments.of("TomlJackson", new TomlJacksonConfigurer())
        );
    }

    @ParameterizedTest(name = "{0}: Empty config")
    @MethodSource("tomlConfigurers")
    void testWrite_EmptyConfig(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with no fields
        EmptyConfig config = ConfigManager.create(EmptyConfig.class);
        config.setConfigurer(configurer);

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String toml = output.toString();

        // Then: TOML output is minimal (Jackson produces "= {}" for empty object)
        assertThat(toml.trim()).isIn("", "= {}");
    }

    @ParameterizedTest(name = "{0}: Very large string values")
    @MethodSource("tomlConfigurers")
    void testWrite_VeryLargeValues(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with very large string value
        LargeValueConfig config = ConfigManager.create(LargeValueConfig.class);
        config.setConfigurer(configurer);
        config.setLargeValue("x".repeat(10000)); // 10k characters

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String toml = output.toString();

        // Then: Large value is written correctly
        assertThat(toml).contains("largeValue");
        assertThat(toml.length()).isGreaterThan(10000);
    }

    @ParameterizedTest(name = "{0}: Special characters in strings")
    @MethodSource("tomlConfigurers")
    void testWrite_SpecialCharactersInStrings(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with special characters
        SpecialCharsConfig config = ConfigManager.create(SpecialCharsConfig.class);
        config.setConfigurer(configurer);
        config.setQuotes("She said: \"Hello!\"");
        config.setBackslash("C:\\Users\\test\\path");
        config.setNewlines("Line 1\nLine 2\nLine 3");
        config.setTabs("Column1\tColumn2\tColumn3");

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String toml = output.toString();

        // Then: Special characters are properly escaped
        assertThat(toml).contains("quotes");
        assertThat(toml).contains("backslash");
        assertThat(toml).contains("newlines");
        assertThat(toml).contains("tabs");
    }

    @ParameterizedTest(name = "{0}: Special characters round-trip")
    @MethodSource("tomlConfigurers")
    void testRoundTrip_SpecialCharacters(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with special characters
        SpecialCharsConfig config = ConfigManager.create(SpecialCharsConfig.class);
        config.setConfigurer(configurer);
        config.setQuotes("She said: \"Hello!\"");
        config.setBackslash("C:\\Users\\test\\path");
        config.setNewlines("Line 1\nLine 2\nLine 3");
        config.setTabs("Column1\tColumn2\tColumn3");

        // When: Save and load
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);

        SpecialCharsConfig loaded = ConfigManager.create(SpecialCharsConfig.class);
        loaded.setConfigurer(configurer);
        loaded.load(new ByteArrayInputStream(output.toByteArray()));

        // Then: Special characters are preserved
        assertThat(loaded.getQuotes()).isEqualTo("She said: \"Hello!\"");
        assertThat(loaded.getBackslash()).isEqualTo("C:\\Users\\test\\path");
        assertThat(loaded.getNewlines()).isEqualTo("Line 1\nLine 2\nLine 3");
        assertThat(loaded.getTabs()).isEqualTo("Column1\tColumn2\tColumn3");
    }

    @ParameterizedTest(name = "{0}: Very deeply nested structure")
    @MethodSource("tomlConfigurers")
    void testWrite_VeryDeeplyNestedStructure(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with deep nesting
        DeepNestedConfig config = ConfigManager.create(DeepNestedConfig.class);
        config.setConfigurer(configurer);

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String toml = output.toString();

        // Then: Deep nesting is handled (sections + dotted keys)
        assertThat(toml).contains("level1");
        assertThat(toml).contains("level2");
    }

    @ParameterizedTest(name = "{0}: Very large collection")
    @MethodSource("tomlConfigurers")
    void testWrite_VeryLargeCollection(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with very large list
        LargeCollectionConfig config = ConfigManager.create(LargeCollectionConfig.class);
        config.setConfigurer(configurer);
        List<String> largeList = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            largeList.add("item-" + i);
        }
        config.setLargeList(largeList);

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String toml = output.toString();

        // Then: All items are written
        assertThat(toml).contains("largeList");
        assertThat(toml).contains("item-0");
        assertThat(toml).contains("item-999");
    }

    @ParameterizedTest(name = "{0}: Null values use marker")
    @MethodSource("tomlConfigurers")
    void testWrite_NullValues(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with null values
        NullValueConfig config = ConfigManager.create(NullValueConfig.class);
        config.setConfigurer(configurer);
        config.setNullString(null);
        config.setNullList(null);
        config.setNullMap(null);

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String toml = output.toString();

        // Then: Null values use __null__ marker
        assertThat(toml).contains("nullString = '__null__'");
        assertThat(toml).contains("nullList = '__null__'");
        assertThat(toml).contains("nullMap = '__null__'");
    }

    @ParameterizedTest(name = "{0}: Null values round-trip")
    @MethodSource("tomlConfigurers")
    void testRoundTrip_NullValues(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with null values
        NullValueConfig config = ConfigManager.create(NullValueConfig.class);
        config.setConfigurer(configurer);
        config.setNullString(null);

        // When: Save and load
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);

        NullValueConfig loaded = ConfigManager.create(NullValueConfig.class);
        loaded.setConfigurer(configurer);
        loaded.load(new ByteArrayInputStream(output.toByteArray()));

        // Then: Null is preserved
        assertThat(loaded.getNullString()).isNull();
    }

    @ParameterizedTest(name = "{0}: Round-trip empty strings")
    @MethodSource("tomlConfigurers")
    void testRoundTrip_EmptyStrings(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with empty strings
        EmptyStringConfig config = ConfigManager.create(EmptyStringConfig.class);
        config.setConfigurer(configurer);
        config.setEmptyString("");
        config.setWhitespaceString("   ");

        // When: Save and load again
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);

        EmptyStringConfig loaded = ConfigManager.create(EmptyStringConfig.class);
        loaded.setConfigurer(configurer);
        loaded.load(new ByteArrayInputStream(output.toByteArray()));

        // Then: Empty strings are preserved
        assertThat(loaded.getEmptyString()).isEqualTo("");
        assertThat(loaded.getWhitespaceString()).isEqualTo("   ");
    }

    @ParameterizedTest(name = "{0}: Unicode round-trip")
    @MethodSource("tomlConfigurers")
    void testRoundTrip_Unicode(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with unicode strings
        UnicodeConfig config = ConfigManager.create(UnicodeConfig.class);
        config.setConfigurer(configurer);
        config.setJapanese("こんにちは");
        config.setEmoji("🎉🌍");
        config.setPolish("Łódź");

        // When: Save and load
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);

        UnicodeConfig loaded = ConfigManager.create(UnicodeConfig.class);
        loaded.setConfigurer(configurer);
        loaded.load(new ByteArrayInputStream(output.toByteArray()));

        // Then: Unicode is preserved
        assertThat(loaded.getJapanese()).isEqualTo("こんにちは");
        assertThat(loaded.getEmoji()).isEqualTo("🎉🌍");
        assertThat(loaded.getPolish()).isEqualTo("Łódź");
    }

    @ParameterizedTest(name = "{0}: Malformed TOML throws exception")
    @MethodSource("tomlConfigurers")
    void testLoad_MalformedToml_ThrowsException(String configurerName, Configurer configurer) {
        // Given: Malformed TOML (invalid syntax)
        String malformedToml = """
            name = "Test"
            value = [unclosed bracket
            enabled = true
            """;

        // When/Then: Loading malformed TOML throws exception
        EmptyConfig config = ConfigManager.create(EmptyConfig.class);
        config.setConfigurer(configurer);

        assertThatThrownBy(() -> config.load(malformedToml))
            .isInstanceOf(Exception.class);
    }

    @ParameterizedTest(name = "{0}: Large long values preserved as strings")
    @MethodSource("tomlConfigurers")
    void testRoundTrip_LargeLongValues(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with large long values (outside double precision range)
        LargeLongConfig config = ConfigManager.create(LargeLongConfig.class);
        config.setConfigurer(configurer);
        config.setMaxLong(Long.MAX_VALUE);
        config.setMinLong(Long.MIN_VALUE);

        // When: Save and load
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);

        LargeLongConfig loaded = ConfigManager.create(LargeLongConfig.class);
        loaded.setConfigurer(configurer);
        loaded.load(new ByteArrayInputStream(output.toByteArray()));

        // Then: Large longs are preserved (via string conversion)
        assertThat(loaded.getMaxLong()).isEqualTo(Long.MAX_VALUE);
        assertThat(loaded.getMinLong()).isEqualTo(Long.MIN_VALUE);
    }

    @ParameterizedTest(name = "{0}: List with nulls round-trip")
    @MethodSource("tomlConfigurers")
    void testRoundTrip_ListWithNulls(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with list containing nulls
        NullListConfig config = ConfigManager.create(NullListConfig.class);
        config.setConfigurer(configurer);
        List<String> listWithNulls = new ArrayList<>();
        listWithNulls.add("first");
        listWithNulls.add(null);
        listWithNulls.add("third");
        config.setItems(listWithNulls);

        // When: Save and load
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);

        NullListConfig loaded = ConfigManager.create(NullListConfig.class);
        loaded.setConfigurer(configurer);
        loaded.load(new ByteArrayInputStream(output.toByteArray()));

        // Then: Nulls are preserved
        assertThat(loaded.getItems()).containsExactly("first", null, "third");
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
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class DeepNestedConfig extends OkaeriConfig {
        private Level1 level1 = new Level1();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class Level1 extends OkaeriConfig {
        private Level2 level2 = new Level2();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class Level2 extends OkaeriConfig {
        private String value = "deep";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class LargeCollectionConfig extends OkaeriConfig {
        private List<String> largeList;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class NullValueConfig extends OkaeriConfig {
        private String nullString = "default";
        private List<String> nullList = new ArrayList<>();
        private Map<String, String> nullMap;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class EmptyStringConfig extends OkaeriConfig {
        private String emptyString;
        private String whitespaceString;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class UnicodeConfig extends OkaeriConfig {
        private String japanese;
        private String emoji;
        private String polish;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class LargeLongConfig extends OkaeriConfig {
        private long maxLong;
        private long minLong;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class NullListConfig extends OkaeriConfig {
        private List<String> items;
    }
}
