package eu.okaeri.configs.format.hocon;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.hocon.lightbend.HoconLightbendConfigurer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Generalized edge case tests for all HOCON configurer implementations.
 * Tests boundary conditions and error handling across HOCON configurers.
 * <p>
 * Currently only HoconLightbendConfigurer exists, but structured to support future implementations.
 */
class HoconConfigurerEdgeCasesTest {

    static Stream<Arguments> hoconConfigurers() {
        return Stream.of(
            Arguments.of("Lightbend", new HoconLightbendConfigurer())
            // Future implementations can be added here
        );
    }

    @ParameterizedTest(name = "{0}: Empty config")
    @MethodSource("hoconConfigurers")
    void testWrite_EmptyConfig(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with no fields
        EmptyConfig config = ConfigManager.create(EmptyConfig.class);
        config.withConfigurer(configurer);

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String hocon = output.toString();

        // Then: HOCON output is minimal
        assertThat(hocon.trim()).isIn("{}", "{\n}", "");
    }

    @ParameterizedTest(name = "{0}: Very large string values")
    @MethodSource("hoconConfigurers")
    void testWrite_VeryLargeValues(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with very large string value
        LargeValueConfig config = ConfigManager.create(LargeValueConfig.class);
        config.withConfigurer(configurer);
        config.setLargeValue("x".repeat(10000)); // 10k characters

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String hocon = output.toString();

        // Then: Large value is written correctly
        assertThat(hocon).contains("largeValue");
        assertThat(hocon.length()).isGreaterThan(10000);
    }

    @ParameterizedTest(name = "{0}: Special characters in strings")
    @MethodSource("hoconConfigurers")
    void testWrite_SpecialCharactersInStrings(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with special characters
        SpecialCharsConfig config = ConfigManager.create(SpecialCharsConfig.class);
        config.withConfigurer(configurer);
        config.setQuotes("She said: \"Hello!\"");
        config.setBackslash("C:\\Users\\test\\path");
        config.setNewlines("Line 1\nLine 2\nLine 3");

        // When: Save and reload
        String hocon = config.saveToString();
        SpecialCharsConfig loaded = ConfigManager.create(SpecialCharsConfig.class);
        loaded.withConfigurer(configurer);
        loaded.load(hocon);

        // Then: Special characters are preserved
        assertThat(loaded.getQuotes()).isEqualTo("She said: \"Hello!\"");
        assertThat(loaded.getBackslash()).isEqualTo("C:\\Users\\test\\path");
        assertThat(loaded.getNewlines()).isEqualTo("Line 1\nLine 2\nLine 3");
    }

    @ParameterizedTest(name = "{0}: Unicode characters")
    @MethodSource("hoconConfigurers")
    void testWrite_UnicodeCharacters(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with unicode characters
        UnicodeConfig config = ConfigManager.create(UnicodeConfig.class);
        config.withConfigurer(configurer);
        config.setJapanese("こんにちは世界 🌍");
        config.setRussian("Привет мир!");
        config.setPolish("Łódź, Gdańsk");

        // When: Save and reload
        String hocon = config.saveToString();
        UnicodeConfig loaded = ConfigManager.create(UnicodeConfig.class);
        loaded.withConfigurer(configurer);
        loaded.load(hocon);

        // Then: Unicode is preserved
        assertThat(loaded.getJapanese()).isEqualTo("こんにちは世界 🌍");
        assertThat(loaded.getRussian()).isEqualTo("Привет мир!");
        assertThat(loaded.getPolish()).isEqualTo("Łódź, Gdańsk");
    }

    @ParameterizedTest(name = "{0}: Null values handling")
    @MethodSource("hoconConfigurers")
    void testWrite_NullValues(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with null value
        NullConfig config = ConfigManager.create(NullConfig.class);
        config.withConfigurer(configurer);
        config.setNullableField(null);

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String hocon = output.toString();

        // Then: Check how HOCON handles nulls
        assertThat(hocon).contains("nullableField");
    }

    @ParameterizedTest(name = "{0}: Empty collections")
    @MethodSource("hoconConfigurers")
    void testWrite_EmptyCollections(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with empty collections
        CollectionsConfig config = ConfigManager.create(CollectionsConfig.class);
        config.withConfigurer(configurer);
        config.setEmptyList(new ArrayList<>());
        config.setEmptyMap(new LinkedHashMap<>());

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String hocon = output.toString();

        // Then: Empty collections are written
        assertThat(hocon).contains("emptyList");
        assertThat(hocon).contains("emptyMap");
    }

    @ParameterizedTest(name = "{0}: Nested structures")
    @MethodSource("hoconConfigurers")
    void testWrite_NestedStructures(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with nested structures
        NestedConfig config = ConfigManager.create(NestedConfig.class);
        config.withConfigurer(configurer);

        Map<String, List<String>> nestedMap = new LinkedHashMap<>();
        List<String> list = new ArrayList<>();
        list.add("item1");
        list.add("item2");
        nestedMap.put("key1", list);
        config.setNestedMap(nestedMap);

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String hocon = output.toString();

        // Then: Nested structures are written
        assertThat(hocon).contains("nestedMap");
    }

    @ParameterizedTest(name = "{0}: Comments with HOCON syntax")
    @MethodSource("hoconConfigurers")
    void testLoad_CommentsInInput(String configurerName, Configurer configurer) throws Exception {
        // Given: HOCON with comments (# and //)
        String hocon = """
            # This is a comment
            name = "Test"
            value = 42
            // Another comment style
            enabled = true
            """;

        // When: Load into config
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(configurer);
        config.load(hocon);

        // Then: Values are loaded correctly (comments ignored)
        assertThat(config.getName()).isEqualTo("Test");
        assertThat(config.getValue()).isEqualTo(42);
        assertThat(config.isEnabled()).isTrue();
    }

    @ParameterizedTest(name = "{0}: Unquoted keys")
    @MethodSource("hoconConfigurers")
    void testLoad_UnquotedKeys(String configurerName, Configurer configurer) throws Exception {
        // Given: HOCON with unquoted keys (HOCON feature)
        String hocon = """
            name = "Test Value"
            value = 999
            enabled = false
            """;

        // When: Load into config
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(configurer);
        config.load(hocon);

        // Then: Values are loaded correctly
        assertThat(config.getName()).isEqualTo("Test Value");
        assertThat(config.getValue()).isEqualTo(999);
        assertThat(config.isEnabled()).isFalse();
    }

    @ParameterizedTest(name = "{0}: Dot notation")
    @MethodSource("hoconConfigurers")
    void testLoad_DotNotation(String configurerName, Configurer configurer) throws Exception {
        // Given: HOCON with dot notation (HOCON feature)
        String hocon = """
            subConfig.field1 = "value1"
            subConfig.field2 = "value2"
            """;

        // When: Load into config
        DotNotationConfig config = ConfigManager.create(DotNotationConfig.class);
        config.withConfigurer(configurer);
        config.load(hocon);

        // Then: Nested values are loaded correctly
        assertThat(config.getSubConfig()).isNotNull();
        assertThat(config.getSubConfig().getField1()).isEqualTo("value1");
        assertThat(config.getSubConfig().getField2()).isEqualTo("value2");
    }

    // Test config classes

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class EmptyConfig extends OkaeriConfig {
        // No fields
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class LargeValueConfig extends OkaeriConfig {
        private String largeValue = "";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class SpecialCharsConfig extends OkaeriConfig {
        private String quotes = "";
        private String backslash = "";
        private String newlines = "";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class UnicodeConfig extends OkaeriConfig {
        private String japanese = "";
        private String russian = "";
        private String polish = "";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class NullConfig extends OkaeriConfig {
        private String nullableField = "default";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class CollectionsConfig extends OkaeriConfig {
        private List<String> emptyList = new ArrayList<>();
        private Map<String, String> emptyMap = new LinkedHashMap<>();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class NestedConfig extends OkaeriConfig {
        private Map<String, List<String>> nestedMap = new LinkedHashMap<>();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class TestConfig extends OkaeriConfig {
        private String name = "default";
        private int value = 0;
        private boolean enabled = false;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class DotNotationConfig extends OkaeriConfig {
        private SubConfigData subConfig = new SubConfigData();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class SubConfigData extends OkaeriConfig {
        private String field1 = "default1";
        private String field2 = "default2";
    }
}
