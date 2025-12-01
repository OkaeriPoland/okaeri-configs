package eu.okaeri.configs.format.properties;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.Header;
import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.properties.PropertiesConfigurer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Structure tests for PropertiesConfigurer.
 * Tests exact Properties formatting output with text block comparisons.
 * <p>
 * Note: Properties format has write-only comments (lost on load) and
 * escapes non-ASCII as \\uXXXX.
 */
class PropertiesConfigurerStructureTest {

    static Stream<Arguments> propertiesConfigurers() {
        return Stream.of(
            Arguments.of("Properties", new PropertiesConfigurer())
        );
    }

    @ParameterizedTest(name = "{0}: Simple field comments")
    @MethodSource("propertiesConfigurers")
    void testSaveToString_SimpleFieldComments_MatchesExpectedProperties(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with simple commented fields
        CommentedConfig config = ConfigManager.create(CommentedConfig.class);
        config.setConfigurer(configurer);

        // When: Save to Properties
        String properties = config.saveToString();

        // Then: Matches expected output (comments are write-only)
        String expected = """
            # This is a simple field comment
            simpleField=default
            # Multi-line comment
            # Line 2 of comment
            numberField=42
            """;

        assertThat(properties).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0}: Header annotation")
    @MethodSource("propertiesConfigurers")
    void testSaveToString_HeaderAnnotation_MatchesExpectedProperties(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with header
        HeaderedConfig config = ConfigManager.create(HeaderedConfig.class);
        config.setConfigurer(configurer);

        // When: Save to Properties
        String properties = config.saveToString();

        // Then: Matches expected output (header as comment)
        String expected = """
            # ===================
            # Test Configuration
            # Version 1.0
            # ===================

            field=value
            """;

        assertThat(properties).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0}: SubConfig with comments")
    @MethodSource("propertiesConfigurers")
    void testSaveToString_SubConfigWithComments_MatchesExpectedProperties(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with SubConfig with commented fields
        SubConfigConfig config = ConfigManager.create(SubConfigConfig.class);
        config.setConfigurer(configurer);

        // When: Save to Properties
        String properties = config.saveToString();

        // Then: Matches expected output (dot notation for nesting, nested comments supported)
        String expected = """
            # Nested subconfig
            # Field inside subconfig
            subConfig.subField=default sub
            subConfig.subNumber=42
            """;

        assertThat(properties).isEqualTo(expected);
    }

    @Test
    void testSaveToString_UnicodeStrings_EscapedWhenEnabled() throws Exception {
        // Given: Config with unicode strings and escapeUnicode enabled
        UnicodeConfig config = ConfigManager.create(UnicodeConfig.class);
        config.setConfigurer(new PropertiesConfigurer().setEscapeUnicode(true));

        // When: Save to Properties
        String properties = config.saveToString();

        // Then: Non-ASCII is escaped as \\uXXXX
        assertThat(properties).contains("japanese=\\u3053\\u3093\\u306B\\u3061\\u306F");
        assertThat(properties).contains("russian=\\u041F\\u0440\\u0438\\u0432\\u0435\\u0442");
        assertThat(properties).contains("polish=Cz\\u0119\\u015B\\u0107");
    }

    @Test
    void testSaveToString_UnicodeStrings_Utf8ByDefault() throws Exception {
        // Given: Config with unicode strings (default escapeUnicode=false)
        UnicodeConfig config = ConfigManager.create(UnicodeConfig.class);
        config.setConfigurer(new PropertiesConfigurer());

        // When: Save to Properties
        String properties = config.saveToString();

        // Then: Unicode is preserved as UTF-8
        assertThat(properties).contains("japanese=こんにちは世界");
        assertThat(properties).contains("russian=Привет мир!");
        assertThat(properties).contains("polish=Część świecie!");
    }

    @ParameterizedTest(name = "{0}: Nested collections structure")
    @MethodSource("propertiesConfigurers")
    void testSaveToString_NestedCollections_MatchesExpectedStructure(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with nested structures
        NestedStructureConfig config = ConfigManager.create(NestedStructureConfig.class);
        config.setConfigurer(configurer);

        // When: Save to Properties
        String properties = config.saveToString();

        // Then: Matches expected output (comma notation for short lists)
        String expected = """
            stringList=alpha,beta,gamma
            simpleMap.key1=value1
            simpleMap.key2=value2
            """;

        assertThat(properties).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0}: Null values use marker")
    @MethodSource("propertiesConfigurers")
    void testSaveToString_NullValues_UseMarker(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with null value
        NullConfig config = ConfigManager.create(NullConfig.class);
        config.setConfigurer(configurer);
        config.setNullableField(null);

        // When: Save to Properties
        String properties = config.saveToString();

        // Then: Null uses __null__ marker
        assertThat(properties).contains("nullableField=__null__");
    }

    @ParameterizedTest(name = "{0}: Round-trip stability")
    @MethodSource("propertiesConfigurers")
    void testSaveLoadCycles_RemainsStable(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with header and comments
        HeaderedCommentedConfig config = ConfigManager.create(HeaderedCommentedConfig.class);
        config.setConfigurer(configurer);

        // When: Initial save
        String firstProperties = config.saveToString();

        // Then: Save/load cycles should produce identical output
        // Note: Comments are lost on load, so we verify just data stability
        String currentProperties = firstProperties;
        for (int i = 0; i < 5; i++) {
            HeaderedCommentedConfig reloaded = ConfigManager.create(HeaderedCommentedConfig.class);
            reloaded.setConfigurer(configurer);
            reloaded.load(currentProperties);
            currentProperties = reloaded.saveToString();

            assertThat(currentProperties)
                .as("Cycle %d: Properties should remain stable", i + 1)
                .isEqualTo(firstProperties);
        }
    }

    // Test config classes

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class CommentedConfig extends OkaeriConfig {
        @Comment("This is a simple field comment")
        private String simpleField = "default";

        @Comment({"Multi-line comment", "Line 2 of comment"})
        private int numberField = 42;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @Header("===================")
    @Header("Test Configuration")
    @Header("Version 1.0")
    @Header("===================")
    public static class HeaderedConfig extends OkaeriConfig {
        private String field = "value";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class SubConfigConfig extends OkaeriConfig {
        @Comment("Nested subconfig")
        private NestedConfig subConfig = new NestedConfig();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class NestedConfig extends OkaeriConfig {
        @Comment("Field inside subconfig")
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
    public static class NullConfig extends OkaeriConfig {
        private String nullableField = "default";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @Header("===================")
    @Header("Test Header")
    @Header("===================")
    public static class HeaderedCommentedConfig extends OkaeriConfig {
        @Comment("Comment on field1")
        private String field1 = "value1";

        @Comment("Comment on field2")
        private String field2 = "value2";
    }
}
