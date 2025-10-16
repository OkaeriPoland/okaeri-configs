package eu.okaeri.configs.format.yaml;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.Header;
import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.yaml.bukkit.YamlBukkitConfigurer;
import eu.okaeri.configs.yaml.bungee.YamlBungeeConfigurer;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Disabled;
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
 * Generalized structure tests for all YAML configurer implementations.
 * Tests exact YAML formatting output with text block comparisons.
 */
class YamlConfigurerStructureTest {

    static Stream<Arguments> yamlConfigurers() {
        return Stream.of(
            Arguments.of("SnakeYAML", new YamlSnakeYamlConfigurer()),
            Arguments.of("Bukkit", new YamlBukkitConfigurer()),
            Arguments.of("Bungee", new YamlBungeeConfigurer())
        );
    }

    @ParameterizedTest(name = "{0}: Simple field comments")
    @MethodSource("yamlConfigurers")
    void testSaveToString_SimpleFieldComments_MatchesExpectedYaml(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with simple commented fields
        CommentedConfig config = ConfigManager.create(CommentedConfig.class);
        config.withConfigurer(configurer);

        // When: Save to YAML
        String yaml = config.saveToString();

        // Then: Matches expected output
        String expected = """
            # This is a simple field comment
            simpleField: default
            # Multi-line comment
            # Line 2 of comment
            numberField: 42
            """;
        
        assertThat(yaml).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0}: Header annotation")
    @MethodSource("yamlConfigurers")
    void testSaveToString_HeaderAnnotation_MatchesExpectedYaml(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with header
        HeaderedConfig config = ConfigManager.create(HeaderedConfig.class);
        config.withConfigurer(configurer);

        // When: Save to YAML
        String yaml = config.saveToString();

        // Then: Matches expected output
        String expected = """
            # ===================
            # Test Configuration
            # Version 1.0
            # ===================
            field: value
            """;
        
        assertThat(yaml).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0}: Serializable with comments")
    @MethodSource("yamlConfigurers")
    @Disabled("@Comment is not implemented for Serializable")
    void testSaveToString_SerializableWithComments_MatchesExpectedYaml(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with Serializable object with commented fields
        SerializableConfig config = ConfigManager.create(SerializableConfig.class);
        config.withConfigurer(configurer);

        // When: Save to YAML
        String yaml = config.saveToString();

        // Then: Matches expected output
        String expected = """
            # Serializable custom object
            customObj:
              # Name field in serializable object
              name: test
              # ID field in serializable object
              id: 999
            """;
        
        assertThat(yaml).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0}: SubConfig with comments")
    @MethodSource("yamlConfigurers")
    void testSaveToString_SubConfigWithComments_MatchesExpectedYaml(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with SubConfig with commented fields
        SubConfigConfig config = ConfigManager.create(SubConfigConfig.class);
        config.withConfigurer(configurer);

        // When: Save to YAML
        String yaml = config.saveToString();

        // Then: Matches expected output
        String expected = """
            # Nested subconfig
            subConfig:
              # Subconfig field
              subField: default sub
              subNumber: 42
            """;
        
        assertThat(yaml).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0}: SubConfig list comments")
    @MethodSource("yamlConfigurers")
    @Disabled("@Comment is not implemented for lists")
    void testSaveToString_SubConfigList_OnlyFirstItemHasComments(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with List<SubConfig> where SubConfig has commented fields
        SubConfigListConfig config = ConfigManager.create(SubConfigListConfig.class);
        config.withConfigurer(configurer);

        // When: Save to YAML
        String yaml = config.saveToString();

        // Then: Matches expected output (comments only on first item)
        String expected = """
            # List of nested configs
            subConfigList:
            - # Subconfig field
              subField: sub1
              subNumber: 10
            - subField: sub2
              subNumber: 20
            """;
        
        assertThat(yaml).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0}: Unicode strings preserved")
    @MethodSource("yamlConfigurers")
    void testSaveToString_UnicodeStrings_PreservedInYaml(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with unicode strings
        UnicodeConfig config = ConfigManager.create(UnicodeConfig.class);
        config.withConfigurer(configurer);

        // When: Save to YAML
        String yaml = config.saveToString();

        // Then: Matches expected output with unicode preserved
        String expected = """
            japanese: こんにちは世界 🌍
            russian: Привет мир! Тестирование кириллицы
            polish: Część świecie! Łódź, Gdańsk
            """;
        
        assertThat(yaml).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0}: Nested collections structure")
    @MethodSource("yamlConfigurers")
    void testSaveToString_NestedCollections_MatchesExpectedStructure(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with nested structures
        NestedStructureConfig config = ConfigManager.create(NestedStructureConfig.class);
        config.withConfigurer(configurer);

        // When: Save to YAML
        String yaml = config.saveToString();

        // Then: Matches expected output with proper indentation
        String expected = """
            stringList:
            - alpha
            - beta
            - gamma
            simpleMap:
              key1: value1
              key2: value2
            """;
        
        assertThat(yaml).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0}: Header and comments stability")
    @MethodSource("yamlConfigurers")
    void testSaveLoadCycles_HeaderAndComments_RemainsStable(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with header and comments
        HeaderedCommentedConfig config = ConfigManager.create(HeaderedCommentedConfig.class);
        config.withConfigurer(configurer);

        // When: Initial save
        String firstYaml = config.saveToString();

        // Then: Save/load cycles should produce identical output (no extra newlines)
        String currentYaml = firstYaml;
        for (int i = 0; i < 5; i++) {
            HeaderedCommentedConfig reloaded = ConfigManager.create(HeaderedCommentedConfig.class);
            reloaded.withConfigurer(configurer);
            reloaded.load(currentYaml);
            currentYaml = reloaded.saveToString();
            
            assertThat(currentYaml)
                .as("Cycle %d: YAML should remain stable", i + 1)
                .isEqualTo(firstYaml);
        }

        // And: Verify expected structure (e.g. no extra newlines in header/comments)
        String expected = """
            # ===================
            # Test Header
            # ===================
            # Comment on field1
            field1: value1
            # Comment on field2
            field2: value2
            """;
        
        assertThat(firstYaml).isEqualTo(expected);
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
    public static class SerializableConfig extends OkaeriConfig {
        @Comment("Serializable custom object")
        private CustomSerializable customObj = new CustomSerializable("test", 999);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomSerializable implements Serializable {
        private static final long serialVersionUID = 1L;

        @Comment("Name field in serializable object")
        private String name;

        @Comment("ID field in serializable object")
        private int id;
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
        @Comment("Subconfig field")
        private String subField = "default sub";
        private int subNumber = 42;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class SubConfigListConfig extends OkaeriConfig {
        @Comment("List of nested configs")
        private List<CommentedSubConfig> subConfigList = Arrays.asList(
            new CommentedSubConfig("sub1", 10),
            new CommentedSubConfig("sub2", 20)
        );
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = false)
    public static class CommentedSubConfig extends OkaeriConfig {
        @Comment("Subconfig field")
        private String subField;

        private int subNumber;
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
            put("key1", "value1");
            put("key2", "value2");
        }};
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
