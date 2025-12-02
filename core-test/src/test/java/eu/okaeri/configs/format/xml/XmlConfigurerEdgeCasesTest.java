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

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Generalized edge case tests for all XML configurer implementations.
 * Tests boundary conditions and error handling across XmlSimpleConfigurer and XmlBeanConfigurer.
 */
class XmlConfigurerEdgeCasesTest {

    static Stream<Arguments> xmlConfigurers() {
        return Stream.of(
            Arguments.of("XML-Simple", new XmlSimpleConfigurer()),
            Arguments.of("XML-Bean", new XmlBeanConfigurer())
        );
    }

    @ParameterizedTest(name = "{0}: Empty config")
    @MethodSource("xmlConfigurers")
    void testWrite_EmptyConfig(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with no fields
        EmptyConfig config = ConfigManager.create(EmptyConfig.class);
        config.setConfigurer(configurer);

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String xml = output.toString();

        // Then: XML output is valid (XmlSimple uses <config>, XmlBean uses <java>)
        assertThat(xml.contains("<config") || xml.contains("<java")).isTrue();
    }

    @ParameterizedTest(name = "{0}: Very large string values")
    @MethodSource("xmlConfigurers")
    void testWrite_VeryLargeValues(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with very large string value
        LargeValueConfig config = ConfigManager.create(LargeValueConfig.class);
        config.setConfigurer(configurer);
        config.setLargeValue("x".repeat(10000)); // 10k characters

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String xml = output.toString();

        // Then: Large value is written correctly
        assertThat(xml).contains("largeValue");
        assertThat(xml.length()).isGreaterThan(10000);
    }

    @ParameterizedTest(name = "{0}: Special characters in strings")
    @MethodSource("xmlConfigurers")
    void testWrite_SpecialCharactersInStrings(String configurerName, Configurer configurer, @TempDir Path tempDir) throws Exception {
        // Given: Config with special characters
        SpecialCharsConfig config = ConfigManager.create(SpecialCharsConfig.class);
        config.setConfigurer(configurer);
        config.setQuotes("She said: \"Hello!\"");
        config.setAmpersand("Tom & Jerry");
        config.setAngleBrackets("1 < 2 > 0");
        config.setUnicode("Emoji: 🎉 Japanese: こんにちは Polish: Łódź");

        // When: Save and load again (round-trip)
        Path file = tempDir.resolve("special.xml");
        config.save(file);

        SpecialCharsConfig loaded = ConfigManager.create(SpecialCharsConfig.class);
        loaded.setConfigurer(configurer);
        loaded.load(file);

        // Then: Special characters are preserved after round-trip
        assertThat(loaded.getQuotes()).isEqualTo("She said: \"Hello!\"");
        assertThat(loaded.getAmpersand()).isEqualTo("Tom & Jerry");
        assertThat(loaded.getAngleBrackets()).isEqualTo("1 < 2 > 0");
        assertThat(loaded.getUnicode()).isEqualTo("Emoji: 🎉 Japanese: こんにちは Polish: Łódź");
    }

    @ParameterizedTest(name = "{0}: Very deeply nested structure")
    @MethodSource("xmlConfigurers")
    void testWrite_VeryDeeplyNestedStructure(String configurerName, Configurer configurer, @TempDir Path tempDir) throws Exception {
        // Given: Config with deep nesting
        DeepNestedConfig config = ConfigManager.create(DeepNestedConfig.class);
        config.setConfigurer(configurer);
        config.setLevel1(Map.of(
            "level2", Map.of(
                "level3", Map.of(
                    "level4", Map.of(
                        "level5", Map.of("value", "deep")
                    )
                )
            )
        ));

        // When: Save and read as string
        Path file = tempDir.resolve("deep.xml");
        config.save(file);
        String xml = Files.readString(file);

        // Then: Nesting structure is maintained
        assertThat(xml).contains("level1");
        assertThat(xml).contains("level2");
        assertThat(xml).contains("level3");
        assertThat(xml).contains("level4");
        assertThat(xml).contains("level5");
    }

    @ParameterizedTest(name = "{0}: Very large collection")
    @MethodSource("xmlConfigurers")
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
        String xml = output.toString();

        // Then: All items are written
        assertThat(xml).contains("largeList");
        assertThat(xml).contains("item-0");
        assertThat(xml).contains("item-999");
    }

    @ParameterizedTest(name = "{0}: Null values handling")
    @MethodSource("xmlConfigurers")
    void testWrite_NullValues(String configurerName, Configurer configurer, @TempDir Path tempDir) throws Exception {
        // Given: Config with null values
        NullValueConfig config = ConfigManager.create(NullValueConfig.class);
        config.setConfigurer(configurer);
        config.setNullString(null);
        config.setNonNullString("present");

        // When: Save and load
        Path file = tempDir.resolve("nulls.xml");
        config.save(file);

        NullValueConfig loaded = ConfigManager.create(NullValueConfig.class);
        loaded.setConfigurer(configurer);
        loaded.load(file);

        // Then: Non-null value is preserved
        assertThat(loaded.getNonNullString()).isEqualTo("present");
    }

    @ParameterizedTest(name = "{0}: Round-trip empty strings")
    @MethodSource("xmlConfigurers")
    void testRoundTrip_EmptyStrings(String configurerName, Configurer configurer, @TempDir Path tempDir) throws Exception {
        // Given: Config with empty strings
        EmptyStringConfig config = ConfigManager.create(EmptyStringConfig.class);
        config.setConfigurer(configurer);
        config.setEmptyString("");
        config.setWhitespaceString("   ");

        // When: Save and load again
        Path file = tempDir.resolve("empty-strings.xml");
        config.save(file);

        EmptyStringConfig loaded = ConfigManager.create(EmptyStringConfig.class);
        loaded.setConfigurer(configurer);
        loaded.load(file);

        // Then: Whitespace string is preserved
        assertThat(loaded.getWhitespaceString()).isEqualTo("   ");
    }

    @ParameterizedTest(name = "{0}: Round-trip primitives")
    @MethodSource("xmlConfigurers")
    void testRoundTrip_Primitives(String configurerName, Configurer configurer, @TempDir Path tempDir) throws Exception {
        // Given: Config with all primitive types
        PrimitivesConfig config = ConfigManager.create(PrimitivesConfig.class);
        config.setConfigurer(configurer);
        config.setBoolValue(true);
        config.setIntValue(42);
        config.setLongValue(9876543210L);
        config.setDoubleValue(3.14159);

        // When: Save and load again
        Path file = tempDir.resolve("primitives.xml");
        config.save(file);

        PrimitivesConfig loaded = ConfigManager.create(PrimitivesConfig.class);
        loaded.setConfigurer(configurer);
        loaded.load(file);

        // Then: All primitives are preserved
        assertThat(loaded.isBoolValue()).isTrue();
        assertThat(loaded.getIntValue()).isEqualTo(42);
        assertThat(loaded.getLongValue()).isEqualTo(9876543210L);
        assertThat(loaded.getDoubleValue()).isEqualTo(3.14159);
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
        private String ampersand;
        private String angleBrackets;
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
        private String nonNullString;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class EmptyStringConfig extends OkaeriConfig {
        private String emptyString;
        private String whitespaceString;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class PrimitivesConfig extends OkaeriConfig {
        private boolean boolValue;
        private int intValue;
        private long longValue;
        private double doubleValue;
    }
}
