package eu.okaeri.configs.format.yaml;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.Header;
import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.yaml.bukkit.YamlBukkitConfigurer;
import eu.okaeri.configs.yaml.bungee.YamlBungeeConfigurer;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Generalized feature tests for all YAML configurer implementations.
 * Tests common functionality across SnakeYAML, Bukkit, and Bungee configurers.
 */
class YamlConfigurerFeaturesTest {

    static Stream<Arguments> yamlConfigurers() {
        return Stream.of(
            Arguments.of("SnakeYAML", new YamlSnakeYamlConfigurer()),
            Arguments.of("Bukkit", new YamlBukkitConfigurer()),
            Arguments.of("Bungee", new YamlBungeeConfigurer())
        );
    }

    @ParameterizedTest(name = "{0}: Load from string")
    @MethodSource("yamlConfigurers")
    void testLoad_FromString(String configurerName, Configurer configurer) throws Exception {
        // Given: YAML content as string
        String yaml = """
            name: Test Config
            value: 42
            enabled: true
            """;

        // When: Load into config
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(configurer);
        config.load(yaml);

        // Then: Values are loaded correctly
        assertThat(config.getName()).isEqualTo("Test Config");
        assertThat(config.getValue()).isEqualTo(42);
        assertThat(config.isEnabled()).isTrue();
    }

    @ParameterizedTest(name = "{0}: Comment preservation")
    @MethodSource("yamlConfigurers")
    void testWrite_CommentPreservation(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with comments
        CommentedConfig config = ConfigManager.create(CommentedConfig.class);
        config.withConfigurer(configurer);
        config.setSimpleField("test value");
        config.setNumberField(999);

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String yaml = output.toString();

        // Then: Comments are present in YAML output
        assertThat(yaml).contains("# This is a simple field comment");
        assertThat(yaml).contains("# Multi-line comment");
        assertThat(yaml).contains("# Line 2 of comment");
        assertThat(yaml).contains("simpleField:");
        assertThat(yaml).contains("numberField:");
    }

    @ParameterizedTest(name = "{0}: Header preservation")
    @MethodSource("yamlConfigurers")
    void testWrite_HeaderPreservation(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with header
        HeaderedConfig config = ConfigManager.create(HeaderedConfig.class);
        config.withConfigurer(configurer);

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String yaml = output.toString();

        // Then: Header is present at the top of YAML
        assertThat(yaml).contains("# ===================");
        assertThat(yaml).contains("# Test Configuration");
        assertThat(yaml).contains("# Version 1.0");
    }

    @ParameterizedTest(name = "{0}: Key ordering")
    @MethodSource("yamlConfigurers")
    void testWrite_KeyOrdering(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with multiple fields
        String yaml = """
            firstField: first
            secondField: second
            thirdField: third
            fourthField: fourth
            """;

        OrderedConfig config = ConfigManager.create(OrderedConfig.class);
        config.withConfigurer(configurer);
        config.load(yaml);

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String resultYaml = output.toString();

        // Then: Key ordering is preserved (LinkedHashMap behavior)
        int firstPos = resultYaml.indexOf("firstField");
        int secondPos = resultYaml.indexOf("secondField");
        int thirdPos = resultYaml.indexOf("thirdField");
        int fourthPos = resultYaml.indexOf("fourthField");

        assertThat(firstPos).isLessThan(secondPos);
        assertThat(secondPos).isLessThan(thirdPos);
        assertThat(thirdPos).isLessThan(fourthPos);
    }

    @ParameterizedTest(name = "{0}: Nested comments")
    @MethodSource("yamlConfigurers")
    void testWrite_NestedComments(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with nested subconfig that has comments
        NestedCommentConfig config = ConfigManager.create(NestedCommentConfig.class);
        config.withConfigurer(configurer);
        config.getNested().setNestedValue("nested test");

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String yaml = output.toString();

        // Then: Both top-level and nested comments are preserved
        assertThat(yaml).contains("# Top level field");
        assertThat(yaml).contains("# Nested configuration section");
        assertThat(yaml).contains("# This is inside the nested config");
    }

    @ParameterizedTest(name = "{0}: Load from InputStream")
    @MethodSource("yamlConfigurers")
    void testLoadFromInputStream_PopulatesInternalMap(String configurerName, Configurer configurer) throws Exception {
        // Given: YAML content as InputStream
        String yaml = """
            name: Test Config
            value: 42
            enabled: true
            """;

        // When: Load from InputStream
        TestConfig config = ConfigManager.create(TestConfig.class);
        configurer.load(new ByteArrayInputStream(yaml.getBytes()), config.getDeclaration());

        // Then: Internal map is populated correctly
        assertThat(configurer.getValue("name")).isEqualTo("Test Config");
        assertThat(configurer.getValue("value")).isEqualTo(42);
        assertThat(configurer.getValue("enabled")).isEqualTo(true);
        assertThat(configurer.getAllKeys()).contains("name", "value", "enabled");
    }

    @ParameterizedTest(name = "{0}: Set/get value operations")
    @MethodSource("yamlConfigurers")
    void testSetValueGetValue_InternalMapOperations(String configurerName, Configurer configurer) {
        // Given: Fresh configurer
        TestConfig config = ConfigManager.create(TestConfig.class);

        // When: Set values using configurer API
        configurer.setValue("key1", "value1", null, null);
        configurer.setValue("key2", 123, null, null);
        configurer.setValueUnsafe("key3", true);

        // Then: Values are retrievable from internal map
        assertThat(configurer.getValue("key1")).isEqualTo("value1");
        assertThat(configurer.getValue("key2")).isEqualTo(123);
        assertThat(configurer.getValue("key3")).isEqualTo(true);
        assertThat(configurer.keyExists("key1")).isTrue();
        assertThat(configurer.keyExists("nonexistent")).isFalse();
    }

    @ParameterizedTest(name = "{0}: Remove key operation")
    @MethodSource("yamlConfigurers")
    void testRemoveKey_ModifiesInternalMap(String configurerName, Configurer configurer) {
        // Given: Configurer with some keys
        configurer.setValueUnsafe("key1", "value1");
        configurer.setValueUnsafe("key2", "value2");

        // When: Remove a key
        Object removed = configurer.remove("key1");

        // Then: Key is removed from internal map
        assertThat(removed).isEqualTo("value1");
        assertThat(configurer.keyExists("key1")).isFalse();
        assertThat(configurer.keyExists("key2")).isTrue();
        assertThat(configurer.getAllKeys()).contains("key2");
    }

    @ParameterizedTest(name = "{0}: Round-trip structure maintenance")
    @MethodSource("yamlConfigurers")
    void testRoundTrip_MaintainsStructure(String configurerName, Configurer configurer, @TempDir Path tempDir) throws Exception {
        // Given: Config with various field types
        String originalYaml = """
            name: Test Config
            enabled: true
            count: 42
            items:
            - alpha
            - beta
            - gamma
            settings:
              timeout: 30
              retries: 3
            """;

        TestConfigWithStructure config = ConfigManager.create(TestConfigWithStructure.class);
        config.withConfigurer(configurer);
        config.load(originalYaml);

        // When: Save and load again
        Path file = tempDir.resolve("test.yml");
        config.save(file);
        String savedYaml = Files.readString(file);

        // Then: Structure is maintained
        assertThat(savedYaml).contains("name:");
        assertThat(savedYaml).contains("enabled:");
        assertThat(savedYaml).contains("count:");
        assertThat(savedYaml).contains("items:");
        assertThat(savedYaml).contains("- alpha");
        assertThat(savedYaml).contains("- beta");
        assertThat(savedYaml).contains("- gamma");
        assertThat(savedYaml).contains("settings:");
    }

    // Test config classes

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class TestConfig extends OkaeriConfig {
        private String name = "default";
        private int value = 0;
        private boolean enabled = false;
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
    public static class OrderedConfig extends OkaeriConfig {
        private String firstField = "first";
        private String secondField = "second";
        private String thirdField = "third";
        private String fourthField = "fourth";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class NestedCommentConfig extends OkaeriConfig {
        @Comment("Top level field")
        private String topLevel = "top";

        @Comment("Nested configuration section")
        private NestedPart nested = new NestedPart();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class NestedPart extends OkaeriConfig {
        @Comment("This is inside the nested config")
        private String nestedValue = "nested";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class TestConfigWithStructure extends OkaeriConfig {
        private String name;
        private boolean enabled;
        private int count;
        private List<String> items;
        private Map<String, Integer> settings;
    }
}
