package eu.okaeri.configs.format.ini;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.properties.IniConfigurer;
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

/**
 * Edge case tests for IniConfigurer.
 * Tests boundary conditions and error handling.
 */
class IniConfigurerEdgeCasesTest {

    static Stream<Arguments> iniConfigurers() {
        return Stream.of(
            Arguments.of("INI", new IniConfigurer())
        );
    }

    @ParameterizedTest(name = "{0}: Empty config")
    @MethodSource("iniConfigurers")
    void testWrite_EmptyConfig(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with no fields
        EmptyConfig config = ConfigManager.create(EmptyConfig.class);
        config.setConfigurer(configurer);

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String ini = output.toString();

        // Then: INI output is empty or minimal
        assertThat(ini.trim()).isEmpty();
    }

    @ParameterizedTest(name = "{0}: Very large string values")
    @MethodSource("iniConfigurers")
    void testWrite_VeryLargeValues(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with very large string value
        LargeValueConfig config = ConfigManager.create(LargeValueConfig.class);
        config.setConfigurer(configurer);
        config.setLargeValue("x".repeat(10000)); // 10k characters

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String ini = output.toString();

        // Then: Large value is written correctly
        assertThat(ini).contains("largeValue=");
        assertThat(ini.length()).isGreaterThan(10000);
    }

    @ParameterizedTest(name = "{0}: Special characters in strings")
    @MethodSource("iniConfigurers")
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
        String ini = output.toString();

        // Then: Special characters are properly escaped
        assertThat(ini).contains("quotes=");
        assertThat(ini).contains("backslash=");
        assertThat(ini).contains("\\n"); // Newlines escaped
        assertThat(ini).contains("\\t"); // Tabs escaped
    }

    @ParameterizedTest(name = "{0}: Special characters round-trip")
    @MethodSource("iniConfigurers")
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

    @ParameterizedTest(name = "{0}: Very large collection")
    @MethodSource("iniConfigurers")
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
        String ini = output.toString();

        // Then: All items are written with index notation
        assertThat(ini).contains("largeList.0=item-0");
        assertThat(ini).contains("largeList.999=item-999");
    }

    @ParameterizedTest(name = "{0}: Null values")
    @MethodSource("iniConfigurers")
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
        String ini = output.toString();

        // Then: Null values use __null__ marker
        assertThat(ini).contains("nullString=__null__");
        assertThat(ini).contains("nullList=__null__");
        assertThat(ini).contains("nullMap=__null__");
    }

    @ParameterizedTest(name = "{0}: Null values round-trip")
    @MethodSource("iniConfigurers")
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
    @MethodSource("iniConfigurers")
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

        // Then: Empty and whitespace strings are preserved
        assertThat(loaded.getEmptyString()).isEqualTo("");
        assertThat(loaded.getWhitespaceString()).isEqualTo("   ");
    }

    @ParameterizedTest(name = "{0}: Unicode round-trip")
    @MethodSource("iniConfigurers")
    void testRoundTrip_Unicode(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with unicode strings
        UnicodeConfig config = ConfigManager.create(UnicodeConfig.class);
        config.setConfigurer(configurer);
        config.setJapanese("こんにちは");
        config.setEmoji("🎉🌍");

        // When: Save and load
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);

        UnicodeConfig loaded = ConfigManager.create(UnicodeConfig.class);
        loaded.setConfigurer(configurer);
        loaded.load(new ByteArrayInputStream(output.toByteArray()));

        // Then: Unicode is preserved (INI uses UTF-8 directly)
        assertThat(loaded.getJapanese()).isEqualTo("こんにちは");
        assertThat(loaded.getEmoji()).isEqualTo("🎉🌍");
    }

    @ParameterizedTest(name = "{0}: Section parsing")
    @MethodSource("iniConfigurers")
    void testLoad_SectionParsing(String configurerName, Configurer configurer) throws Exception {
        // Given: INI with sections
        String ini = """
            appName=MyApp

            [database]
            host=localhost
            port=5432
            """;

        SectionedConfig config = ConfigManager.create(SectionedConfig.class);
        config.setConfigurer(configurer);
        config.load(ini);

        // Then: Section is parsed correctly
        assertThat(config.getAppName()).isEqualTo("MyApp");
        assertThat(config.getDatabase().getHost()).isEqualTo("localhost");
        assertThat(config.getDatabase().getPort()).isEqualTo(5432);
    }

    @ParameterizedTest(name = "{0}: Comment styles")
    @MethodSource("iniConfigurers")
    void testLoad_CommentStyles(String configurerName, Configurer configurer) throws Exception {
        // Given: INI with both ; and # comments
        String ini = """
            ; semicolon comment
            # hash comment
            value=test
            """;

        SimpleConfig config = ConfigManager.create(SimpleConfig.class);
        config.setConfigurer(configurer);
        config.load(ini);

        // Then: Comments are ignored, value is parsed
        assertThat(config.getValue()).isEqualTo("test");
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
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class SectionedConfig extends OkaeriConfig {
        private String appName = "App";
        private DatabaseConfig database = new DatabaseConfig();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class DatabaseConfig extends OkaeriConfig {
        private String host = "localhost";
        private int port = 5432;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class SimpleConfig extends OkaeriConfig {
        private String value;
    }
}
