package eu.okaeri.configs.format.toml;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.Header;
import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.toml.TomlJacksonConfigurer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Structure tests for TOML configurer implementations.
 * Tests TOML formatting output including sections, comments, and data structures.
 */
class TomlConfigurerStructureTest {

    static Stream<Arguments> tomlConfigurers() {
        return Stream.of(
            Arguments.of("TomlJackson", new TomlJacksonConfigurer())
        );
    }

    @ParameterizedTest(name = "{0}: Simple fields structure")
    @MethodSource("tomlConfigurers")
    void testSaveToString_SimpleFields(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with simple fields
        SimpleConfig config = ConfigManager.create(SimpleConfig.class);
        config.setConfigurer(configurer);

        // When: Save to TOML
        String toml = config.saveToString();

        // Then: Contains expected structure
        assertThat(toml).contains("simpleField = 'default'");
        assertThat(toml).contains("numberField = 42");
    }

    @ParameterizedTest(name = "{0}: Simple field comments")
    @MethodSource("tomlConfigurers")
    void testSaveToString_SimpleFieldComments(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with commented fields
        CommentedConfig config = ConfigManager.create(CommentedConfig.class);
        config.setConfigurer(configurer);

        // When: Save to TOML
        String toml = config.saveToString();

        // Then: Comments are present
        assertThat(toml).contains("# This is a simple field comment");
        assertThat(toml).contains("simpleField = 'default'");
        assertThat(toml).contains("# Multi-line comment");
        assertThat(toml).contains("# Line 2 of comment");
        assertThat(toml).contains("numberField = 42");
    }

    @ParameterizedTest(name = "{0}: Header annotation")
    @MethodSource("tomlConfigurers")
    void testSaveToString_HeaderAnnotation(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with header
        HeaderedConfig config = ConfigManager.create(HeaderedConfig.class);
        config.setConfigurer(configurer);

        // When: Save to TOML
        String toml = config.saveToString();

        // Then: Header is present as comments
        assertThat(toml).contains("# ===================");
        assertThat(toml).contains("# Test Configuration");
        assertThat(toml).contains("# Version 1.0");
        assertThat(toml).contains("field = 'value'");
    }

    @ParameterizedTest(name = "{0}: SubConfig uses section syntax")
    @MethodSource("tomlConfigurers")
    void testSaveToString_SubConfigUsesSection(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with SubConfig
        SubConfigConfig config = ConfigManager.create(SubConfigConfig.class);
        config.setConfigurer(configurer);

        // When: Save to TOML
        String toml = config.saveToString();

        // Then: SubConfig uses TOML section syntax
        assertThat(toml).contains("[subConfig]");
        assertThat(toml).contains("subField = 'default sub'");
        assertThat(toml).contains("subNumber = 42");
    }

    @ParameterizedTest(name = "{0}: SubConfig with comments")
    @MethodSource("tomlConfigurers")
    void testSaveToString_SubConfigWithComments(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with commented SubConfig
        CommentedSubConfigConfig config = ConfigManager.create(CommentedSubConfigConfig.class);
        config.setConfigurer(configurer);

        // When: Save to TOML
        String toml = config.saveToString();

        // Then: Section has comment before it
        assertThat(toml).contains("# Nested subconfig");
        assertThat(toml).contains("[subConfig]");
    }

    @ParameterizedTest(name = "{0}: Nested SubConfig uses dotted section")
    @MethodSource("tomlConfigurers")
    void testSaveToString_NestedSubConfigUsesDottedSection(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with nested SubConfig (2 levels)
        NestedSubConfigConfig config = ConfigManager.create(NestedSubConfigConfig.class);
        config.setConfigurer(configurer);

        // When: Save to TOML
        String toml = config.saveToString();

        // Then: Uses dotted section for nested config (no separate [outer] needed - valid TOML)
        assertThat(toml).contains("[outer.inner]");
        assertThat(toml).contains("value = 'nested'");
    }

    @ParameterizedTest(name = "{0}: Map uses dotted keys (not section)")
    @MethodSource("tomlConfigurers")
    void testSaveToString_MapUsesDottedKeys(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with Map (not OkaeriConfig)
        MapConfig config = ConfigManager.create(MapConfig.class);
        config.setConfigurer(configurer);

        // When: Save to TOML
        String toml = config.saveToString();

        // Then: Map uses dotted keys, not section syntax
        assertThat(toml).contains("settings.key1 = 'value1'");
        assertThat(toml).contains("settings.key2 = 'value2'");
        // Should NOT create [settings] section for plain Map
        assertThat(toml).doesNotContain("[settings]");
    }

    @ParameterizedTest(name = "{0}: List structure")
    @MethodSource("tomlConfigurers")
    void testSaveToString_ListStructure(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with list
        ListConfig config = ConfigManager.create(ListConfig.class);
        config.setConfigurer(configurer);

        // When: Save to TOML
        String toml = config.saveToString();

        // Then: List uses TOML array syntax
        assertThat(toml).contains("stringList = ['alpha', 'beta', 'gamma']");
    }

    @ParameterizedTest(name = "{0}: Unicode strings preserved")
    @MethodSource("tomlConfigurers")
    void testSaveToString_UnicodeStrings(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with unicode strings
        UnicodeConfig config = ConfigManager.create(UnicodeConfig.class);
        config.setConfigurer(configurer);

        // When: Save to TOML
        String toml = config.saveToString();

        // Then: Unicode is preserved
        assertThat(toml).contains("japanese");
        assertThat(toml).contains("russian");
        assertThat(toml).contains("polish");
    }

    @ParameterizedTest(name = "{0}: Null values use marker")
    @MethodSource("tomlConfigurers")
    void testSaveToString_NullValues(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with null value
        NullConfig config = ConfigManager.create(NullConfig.class);
        config.setConfigurer(configurer);
        config.setNullableField(null);

        // When: Save to TOML
        String toml = config.saveToString();

        // Then: Null uses __null__ marker
        assertThat(toml).contains("nullableField = '__null__'");
    }

    @ParameterizedTest(name = "{0}: Round-trip stability")
    @MethodSource("tomlConfigurers")
    void testSaveLoadCycles_RemainsStable(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with header and comments
        HeaderedCommentedConfig config = ConfigManager.create(HeaderedCommentedConfig.class);
        config.setConfigurer(configurer);

        // When: Initial save
        String firstToml = config.saveToString();

        // Then: Save/load cycles should produce identical output
        String currentToml = firstToml;
        for (int i = 0; i < 5; i++) {
            HeaderedCommentedConfig reloaded = ConfigManager.create(HeaderedCommentedConfig.class);
            reloaded.setConfigurer(configurer);
            reloaded.load(currentToml);
            currentToml = reloaded.saveToString();

            assertThat(currentToml)
                .as("Cycle %d: TOML should remain stable", i + 1)
                .isEqualTo(firstToml);
        }
    }

    @ParameterizedTest(name = "{0}: Large longs stored as strings")
    @MethodSource("tomlConfigurers")
    void testSaveToString_LargeLongsAsStrings(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with large long value
        LargeLongConfig config = ConfigManager.create(LargeLongConfig.class);
        config.setConfigurer(configurer);
        config.setMaxLong(Long.MAX_VALUE);

        // When: Save to TOML
        String toml = config.saveToString();

        // Then: Large long is stored as string (to preserve precision)
        assertThat(toml).contains("maxLong = '9223372036854775807'");
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
        private NestedConfig subConfig = new NestedConfig();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class CommentedSubConfigConfig extends OkaeriConfig {
        @Comment("Nested subconfig")
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
    public static class NestedSubConfigConfig extends OkaeriConfig {
        private OuterConfig outer = new OuterConfig();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class OuterConfig extends OkaeriConfig {
        private InnerConfig inner = new InnerConfig();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class InnerConfig extends OkaeriConfig {
        private String value = "nested";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class MapConfig extends OkaeriConfig {
        private java.util.Map<String, String> settings = new LinkedHashMap<String, String>() {{
            put("key1", "value1");
            put("key2", "value2");
        }};
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class ListConfig extends OkaeriConfig {
        private List<String> stringList = Arrays.asList("alpha", "beta", "gamma");
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

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class LargeLongConfig extends OkaeriConfig {
        private long maxLong;
    }
}
