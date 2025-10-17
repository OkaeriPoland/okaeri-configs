package eu.okaeri.configs.format.hjson;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.hjson.HjsonConfigurer;
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
 * Generalized edge case tests for all HJSON configurer implementations.
 * Tests boundary conditions and error handling across HJSON configurers.
 * <p>
 * Currently only HjsonConfigurer exists, but structured to support future implementations.
 */
class HjsonConfigurerEdgeCasesTest {

    static Stream<Arguments> hjsonConfigurers() {
        return Stream.of(
            Arguments.of("Hjson", new HjsonConfigurer())
            // Future implementations can be added here
        );
    }

    @ParameterizedTest(name = "{0}: Empty config")
    @MethodSource("hjsonConfigurers")
    void testWrite_EmptyConfig(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with no fields
        EmptyConfig config = ConfigManager.create(EmptyConfig.class);
        config.withConfigurer(configurer);

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String hjson = output.toString();

        // Then: HJSON output is minimal (just {} or empty)
        assertThat(hjson.trim()).isIn("{}", "{\n}");
    }

    @ParameterizedTest(name = "{0}: Very large string values")
    @MethodSource("hjsonConfigurers")
    void testWrite_VeryLargeValues(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with very large string value
        LargeValueConfig config = ConfigManager.create(LargeValueConfig.class);
        config.withConfigurer(configurer);
        config.setLargeValue("x".repeat(10000)); // 10k characters

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String hjson = output.toString();

        // Then: Large value is written correctly
        assertThat(hjson).contains("largeValue:");
        assertThat(hjson.length()).isGreaterThan(10000);
    }

    @ParameterizedTest(name = "{0}: Special characters in strings")
    @MethodSource("hjsonConfigurers")
    void testWrite_SpecialCharactersInStrings(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with special characters
        SpecialCharsConfig config = ConfigManager.create(SpecialCharsConfig.class);
        config.withConfigurer(configurer);
        config.setQuotes("She said: \"Hello!\"");
        config.setBackslash("C:\\Users\\test\\path");
        config.setNewlines("Line 1\nLine 2\nLine 3");

        // When: Save and reload
        String hjson = config.saveToString();
        SpecialCharsConfig loaded = ConfigManager.create(SpecialCharsConfig.class);
        loaded.withConfigurer(configurer);
        loaded.load(hjson);

        // Then: Special characters are preserved
        assertThat(loaded.getQuotes()).isEqualTo("She said: \"Hello!\"");
        assertThat(loaded.getBackslash()).isEqualTo("C:\\Users\\test\\path");
        assertThat(loaded.getNewlines()).isEqualTo("Line 1\nLine 2\nLine 3");
    }

    @ParameterizedTest(name = "{0}: Unicode characters")
    @MethodSource("hjsonConfigurers")
    void testWrite_UnicodeCharacters(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with unicode characters
        UnicodeConfig config = ConfigManager.create(UnicodeConfig.class);
        config.withConfigurer(configurer);
        config.setJapanese("こんにちは世界 🌍");
        config.setRussian("Привет мир!");
        config.setPolish("Łódź, Gdańsk");

        // When: Save and reload
        String hjson = config.saveToString();
        UnicodeConfig loaded = ConfigManager.create(UnicodeConfig.class);
        loaded.withConfigurer(configurer);
        loaded.load(hjson);

        // Then: Unicode is preserved
        assertThat(loaded.getJapanese()).isEqualTo("こんにちは世界 🌍");
        assertThat(loaded.getRussian()).isEqualTo("Привет мир!");
        assertThat(loaded.getPolish()).isEqualTo("Łódź, Gdańsk");
    }

    @ParameterizedTest(name = "{0}: Null values are stored")
    @MethodSource("hjsonConfigurers")
    void testWrite_NullValuesStored(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with null value
        NullConfig config = ConfigManager.create(NullConfig.class);
        config.withConfigurer(configurer);
        config.setNullableField(null);

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String hjson = output.toString();

        // Then: HJSON stores null values (unlike JSON which omits them)
        assertThat(hjson).contains("nullableField: null");
    }

    @ParameterizedTest(name = "{0}: Empty collections")
    @MethodSource("hjsonConfigurers")
    void testWrite_EmptyCollections(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with empty collections
        CollectionsConfig config = ConfigManager.create(CollectionsConfig.class);
        config.withConfigurer(configurer);
        config.setEmptyList(new ArrayList<>());
        config.setEmptyMap(new LinkedHashMap<>());

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String hjson = output.toString();

        // Then: Empty collections are written (HJSON uses newlines before brackets)
        assertThat(hjson).contains("emptyList:");
        assertThat(hjson).contains("[]");
        assertThat(hjson).contains("emptyMap:");
        assertThat(hjson).contains("{}");
    }

    @ParameterizedTest(name = "{0}: Nested empty structures")
    @MethodSource("hjsonConfigurers")
    void testWrite_NestedEmptyStructures(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with nested empty structures
        NestedConfig config = ConfigManager.create(NestedConfig.class);
        config.withConfigurer(configurer);

        Map<String, List<String>> nestedMap = new LinkedHashMap<>();
        nestedMap.put("key1", new ArrayList<>());
        config.setNestedMap(nestedMap);

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String hjson = output.toString();

        // Then: Nested empty structures are written (HJSON uses newlines)
        assertThat(hjson).contains("nestedMap:");
        assertThat(hjson).contains("key1:");
        assertThat(hjson).contains("[]");
    }

    @ParameterizedTest(name = "{0}: Comments with HJSON syntax")
    @MethodSource("hjsonConfigurers")
    void testLoad_CommentsInInput(String configurerName, Configurer configurer) throws Exception {
        // Given: HJSON with comments (HJSON feature)
        // Note: Inline comments are not stripped by the library and become part of values
        String hjson = """
            {
              # This is a comment
              name: Test
              value: 42
              /* multiline
                 comment */
              enabled: true
            }
            """;

        // When: Load into config
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(configurer);
        config.load(hjson);

        // Then: Values are loaded correctly (standalone comments are ignored)
        assertThat(config.getName()).isEqualTo("Test");
        assertThat(config.getValue()).isEqualTo(42);
        assertThat(config.isEnabled()).isTrue();
    }

    @ParameterizedTest(name = "{0}: Unquoted keys")
    @MethodSource("hjsonConfigurers")
    void testLoad_UnquotedKeys(String configurerName, Configurer configurer) throws Exception {
        // Given: HJSON with unquoted keys (HJSON feature)
        String hjson = """
            {
              name: Test Value
              value: 999
              enabled: false
            }
            """;

        // When: Load into config
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(configurer);
        config.load(hjson);

        // Then: Values are loaded correctly
        assertThat(config.getName()).isEqualTo("Test Value");
        assertThat(config.getValue()).isEqualTo(999);
        assertThat(config.isEnabled()).isFalse();
    }

    @ParameterizedTest(name = "{0}: Very deep nesting")
    @MethodSource("hjsonConfigurers")
    void testWrite_VeryDeepNesting(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with very deep nesting
        DeepNestingConfig config = ConfigManager.create(DeepNestingConfig.class);
        config.withConfigurer(configurer);

        Map<String, Object> deep = new LinkedHashMap<>();
        Map<String, Object> current = deep;
        for (int i = 0; i < 10; i++) {
            Map<String, Object> next = new LinkedHashMap<>();
            current.put("level" + i, next);
            current = next;
        }
        current.put("value", "deeply nested");
        config.setDeepMap(deep);

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String hjson = output.toString();

        // Then: Deep nesting is written correctly
        assertThat(hjson).contains("deepMap:");
        assertThat(hjson).contains("level0:");
        assertThat(hjson).contains("deeply nested");
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
    public static class DeepNestingConfig extends OkaeriConfig {
        private Map<String, Object> deepMap = new LinkedHashMap<>();
    }
}
