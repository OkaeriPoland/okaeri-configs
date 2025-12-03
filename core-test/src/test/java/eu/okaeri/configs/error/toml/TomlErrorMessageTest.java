package eu.okaeri.configs.error.toml;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.CustomKey;
import eu.okaeri.configs.annotation.NameModifier;
import eu.okaeri.configs.annotation.NameStrategy;
import eu.okaeri.configs.annotation.Names;
import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.exception.OkaeriConfigException;
import eu.okaeri.configs.serdes.commons.SerdesCommons;
import eu.okaeri.configs.toml.TomlJacksonConfigurer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests error messages for TOML configuration loading failures.
 */
class TomlErrorMessageTest {

    static Stream<Arguments> tomlConfigurers() {
        return Stream.of(
            Arguments.of("TomlJackson", new TomlJacksonConfigurer())
        );
    }

    // ==================== Standard Types ====================

    @ParameterizedTest(name = "{0}: Invalid Integer")
    @MethodSource("tomlConfigurers")
    void testError_InvalidInteger(String name, Configurer configurer) {
        String toml = """
            value = 'not_a_number'
            """;

        assertThatThrownBy(() -> this.loadConfig(IntegerConfig.class, configurer, toml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("value");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'value' to Integer from String
                     --> 1:10
                      |
                    1 | value = 'not_a_number'
                      |          ^^^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    // ==================== Nested Paths (Sections) ====================

    @ParameterizedTest(name = "{0}: Invalid nested field (section)")
    @MethodSource("tomlConfigurers")
    void testError_NestedField_Section(String name, Configurer configurer) {
        String toml = """
            [database]
            host = 'localhost'
            port = 'not_a_port'
            """;

        assertThatThrownBy(() -> this.loadConfig(NestedConfig.class, configurer, toml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("database.port");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'database.port' to Integer from String
                     --> 3:9
                      |
                    3 | port = 'not_a_port'
                      |         ^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    @ParameterizedTest(name = "{0}: Invalid nested field (dotted key)")
    @MethodSource("tomlConfigurers")
    void testError_NestedField_DottedKey(String name, Configurer configurer) {
        String toml = """
            database.host = 'localhost'
            database.port = 'not_a_port'
            """;

        assertThatThrownBy(() -> this.loadConfig(NestedConfig.class, configurer, toml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("database.port");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'database.port' to Integer from String
                     --> 2:18
                      |
                    2 | database.port = 'not_a_port'
                      |                  ^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    // ==================== List Paths ====================

    @ParameterizedTest(name = "{0}: Invalid list element")
    @MethodSource("tomlConfigurers")
    void testError_ListElement(String name, Configurer configurer) {
        String toml = """
            numbers = [1, 2, 'not_a_number', 4]
            """;

        assertThatThrownBy(() -> this.loadConfig(ListConfig.class, configurer, toml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("numbers[2]");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'numbers[2]' to Integer from String
                     --> 1:19
                      |
                    1 | numbers = [1, 2, 'not_a_number', 4]
                      |                   ^^^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    @ParameterizedTest(name = "{0}: Invalid list element (first)")
    @MethodSource("tomlConfigurers")
    void testError_ListElement_First(String name, Configurer configurer) {
        String toml = """
            numbers = ['bad', 2, 3]
            """;

        assertThatThrownBy(() -> this.loadConfig(ListConfig.class, configurer, toml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("numbers[0]");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'numbers[0]' to Integer from String
                     --> 1:13
                      |
                    1 | numbers = ['bad', 2, 3]
                      |             ^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    // ==================== Map Paths ====================

    @ParameterizedTest(name = "{0}: Invalid map value (dotted)")
    @MethodSource("tomlConfigurers")
    void testError_MapValue_Dotted(String name, Configurer configurer) {
        String toml = """
            limits.daily = 100
            limits.weekly = 'not_a_number'
            """;

        assertThatThrownBy(() -> this.loadConfig(MapConfig.class, configurer, toml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                // Maps use KeyNode, so path shows as limits["weekly"]
                assertThat(e.getPath().toString()).isEqualTo("limits[\"weekly\"]");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'limits["weekly"]' to Integer from String
                     --> 2:18
                      |
                    2 | limits.weekly = 'not_a_number'
                      |                  ^^^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    // ==================== Enum Errors ====================

    @ParameterizedTest(name = "{0}: Invalid enum value")
    @MethodSource("tomlConfigurers")
    void testError_InvalidEnum(String name, Configurer configurer) {
        String toml = """
            level = 'MDIUM'
            """;

        assertThatThrownBy(() -> this.loadConfig(EnumConfig.class, configurer, toml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("level");
                assertThat(e.getMessage()).isEqualTo("""
                    Cannot resolve 'level' to Level from String
                     --> 1:10
                      |
                    1 | level = 'MDIUM'
                      |          ^^^^^ Expected MEDIUM, HIGH or LOW""");
            });
    }

    // ==================== Deeply Nested Configs ====================

    @ParameterizedTest(name = "{0}: Deeply nested field (multi-level section)")
    @MethodSource("tomlConfigurers")
    void testError_DeeplyNested_MultiLevelSection(String name, Configurer configurer) {
        String toml = """
            [level1]
            value = 10

            [level1.level2]
            value = 20

            [level1.level2.level3]
            value = 'not_an_int'
            """;

        assertThatThrownBy(() -> this.loadConfig(DeepNestedConfig.class, configurer, toml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("level1.level2.level3.value");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'level1.level2.level3.value' to Integer from String
                     --> 8:10
                      |
                    8 | value = 'not_an_int'
                      |          ^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    @ParameterizedTest(name = "{0}: Deeply nested field (dotted section)")
    @MethodSource("tomlConfigurers")
    void testError_DeeplyNested_DottedSection(String name, Configurer configurer) {
        String toml = """
            [level1.level2.level3]
            value = 'invalid'
            """;

        assertThatThrownBy(() -> this.loadConfig(DeepNestedConfig.class, configurer, toml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("level1.level2.level3.value");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'level1.level2.level3.value' to Integer from String
                     --> 2:10
                      |
                    2 | value = 'invalid'
                      |          ^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    // ==================== Inline Tables ====================

    @ParameterizedTest(name = "{0}: Inline table - invalid field")
    @MethodSource("tomlConfigurers")
    void testError_InlineTable_InvalidField(String name, Configurer configurer) {
        String toml = "database = {host = 'localhost', port = 'bad'}";

        assertThatThrownBy(() -> this.loadConfig(NestedConfig.class, configurer, toml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("database.port");
                // Inline tables are parsed as a single value, walker finds the whole inline table
                // The error still points to the correct path
            });
    }

    // ==================== List of Nested Configs ====================

    @ParameterizedTest(name = "{0}: List of inline tables - invalid field")
    @MethodSource("tomlConfigurers")
    void testError_ListOfInlineTables_InvalidField(String name, Configurer configurer) {
        String toml = "servers = [{host = 'server1', port = 8080}, {host = 'server2', port = 'bad'}]";

        assertThatThrownBy(() -> this.loadConfig(ServerListConfig.class, configurer, toml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("servers[1].port");
            });
    }

    @ParameterizedTest(name = "{0}: Array of tables - invalid field")
    @MethodSource("tomlConfigurers")
    void testError_ArrayOfTables_InvalidField(String name, Configurer configurer) {
        String toml = """
            [[servers]]
            host = 'server1'
            port = 8080

            [[servers]]
            host = 'server2'
            port = 'invalid_port'
            """;

        assertThatThrownBy(() -> this.loadConfig(ServerListConfig.class, configurer, toml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("servers[1].port");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'servers[1].port' to Integer from String
                     --> 7:9
                      |
                    7 | port = 'invalid_port'
                      |         ^^^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    // ==================== Nested Config Inside List ====================

    @ParameterizedTest(name = "{0}: Nested config list - deeply nested error")
    @MethodSource("tomlConfigurers")
    void testError_NestedConfigList_DeeplyNested(String name, Configurer configurer) {
        String toml = """
            [[items]]
            name = 'item1'
            settings.timeout = 100

            [[items]]
            name = 'item2'
            settings.timeout = 'not_a_number'
            """;

        assertThatThrownBy(() -> this.loadConfig(ItemListConfig.class, configurer, toml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("items[1].settings[\"timeout\"]");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'items[1].settings["timeout"]' to Integer from String
                     --> 7:21
                      |
                    7 | settings.timeout = 'not_a_number'
                      |                     ^^^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    // ==================== Inline Format ====================

    @ParameterizedTest(name = "{0}: Inline - Invalid Integer")
    @MethodSource("tomlConfigurers")
    void testError_Inline_InvalidInteger(String name, Configurer configurer) {
        String toml = "value = 'not_a_number'";

        assertThatThrownBy(() -> this.loadConfig(IntegerConfig.class, configurer, toml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("value");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'value' to Integer from String
                     --> 1:10
                      |
                    1 | value = 'not_a_number'
                      |          ^^^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    // ==================== Sibling Subconfig Accumulation Test ====================

    /**
     * Tests that error paths don't accumulate across sibling subconfigs.
     * This was a bug where processing subconfig A would pollute the parent's basePath,
     * causing subconfig B's error to show path "A.B.field" instead of just "B.field".
     */
    @ParameterizedTest(name = "{0}: Sibling subconfigs don't accumulate paths")
    @MethodSource("tomlConfigurers")
    void testError_SiblingSubconfigsDoNotAccumulatePaths(String name, Configurer configurer) {
        // Config with 3 sibling subconfigs, error in the last one
        String toml = """
            [sectionA]
            valueA = 100

            [sectionB]
            valueB = 200

            [sectionC]
            valueC = 'not_a_number'
            """;

        assertThatThrownBy(() -> this.loadConfig(SiblingSubconfigsConfig.class, configurer, toml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                // Path should be just "sectionC.valueC", NOT "sectionA.sectionB.sectionC.valueC"
                assertThat(e.getPath().toString()).isEqualTo("sectionC.valueC");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'sectionC.valueC' to Integer from String
                     --> 8:11
                      |
                    8 | valueC = 'not_a_number'
                      |           ^^^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    // ==================== @CustomKey Annotation Tests ====================

    /**
     * Tests that error messages work correctly with @CustomKey annotation.
     * The TOML uses the custom key name (e.g., "custom-value"), and the error
     * message should correctly locate and display the source line.
     */
    @ParameterizedTest(name = "{0}: @CustomKey simple field")
    @MethodSource("tomlConfigurers")
    void testError_CustomKey_SimpleField(String name, Configurer configurer) {
        String toml = "custom-value = 'not_a_number'";

        assertThatThrownBy(() -> this.loadConfig(CustomKeyConfig.class, configurer, toml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("custom-value");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'custom-value' to Integer from String
                     --> 1:17
                      |
                    1 | custom-value = 'not_a_number'
                      |                 ^^^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    /**
     * Tests that error messages work correctly with nested @CustomKey annotations.
     * Both the parent field and nested field use custom key names.
     */
    @ParameterizedTest(name = "{0}: @CustomKey nested field")
    @MethodSource("tomlConfigurers")
    void testError_CustomKey_NestedField(String name, Configurer configurer) {
        String toml = """
            [some.custom-key]
            nested-value = 'not_a_number'
            """;

        assertThatThrownBy(() -> this.loadConfig(NestedCustomKeyConfig.class, configurer, toml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("some.custom-key.nested-value");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'some.custom-key.nested-value' to Integer from String
                     --> 2:17
                      |
                    2 | nested-value = 'not_a_number'
                      |                 ^^^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    /**
     * Tests that error messages work correctly when only the parent has @CustomKey.
     */
    @ParameterizedTest(name = "{0}: @CustomKey on parent only")
    @MethodSource("tomlConfigurers")
    void testError_CustomKey_ParentOnly(String name, Configurer configurer) {
        String toml = """
            [custom-parent]
            value = 'not_a_number'
            """;

        assertThatThrownBy(() -> this.loadConfig(CustomKeyParentConfig.class, configurer, toml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("custom-parent.value");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'custom-parent.value' to Integer from String
                     --> 2:10
                      |
                    2 | value = 'not_a_number'
                      |          ^^^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    // ==================== @Names Annotation Tests (HYPHEN_CASE) ====================

    /**
     * Tests that error messages work correctly with @Names(strategy = HYPHEN_CASE).
     * This tests configs using @Names strategy where field names are transformed
     * to hyphen-case (e.g., updateRate -> update-rate).
     */
    @ParameterizedTest(name = "{0}: @Names HYPHEN_CASE nested field (scoreboard.dummy.update-rate pattern)")
    @MethodSource("tomlConfigurers")
    void testError_Names_HyphenCase_NestedField(String name, Configurer configurer) {
        String toml = """
            [scoreboard.dummy]
            update-rate = 'hello'
            """;

        assertThatThrownBy(() -> this.loadConfigWithCommons(ScoreboardConfig.class, configurer, toml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("scoreboard.dummy.update-rate");
                assertThat(e.getMessage()).isEqualTo("""
                    error[DurationTransformer]: Cannot transform 'scoreboard.dummy.update-rate' to Duration from String
                     --> 2:16
                      |
                    2 | update-rate = 'hello'
                      |                ^^^^^ Expected duration (e.g. 30s, 5m, 1h30m, 1d)""");
            });
    }

    /**
     * Tests @Names HYPHEN_CASE with a simple nested field.
     */
    @ParameterizedTest(name = "{0}: @Names HYPHEN_CASE simple nested")
    @MethodSource("tomlConfigurers")
    void testError_Names_HyphenCase_SimpleNested(String name, Configurer configurer) {
        String toml = """
            [my-section]
            my-value = 'not_a_number'
            """;

        assertThatThrownBy(() -> this.loadConfig(HyphenCaseConfig.class, configurer, toml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("my-section.my-value");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'my-section.my-value' to Integer from String
                     --> 2:13
                      |
                    2 | my-value = 'not_a_number'
                      |             ^^^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    // ==================== errorComments Option Tests ====================

    /**
     * Tests that errorComments(true) config option automatically includes
     * all consecutive comment lines above the error field.
     */
    @ParameterizedTest(name = "{0}: errorComments includes consecutive comments above field")
    @MethodSource("tomlConfigurers")
    void testError_ErrorCommentsOption_IncludesConsecutiveComments(String name, Configurer configurer) {
        String toml = """
            name = 'test'
            # This is a comment about the value
            # Value must be a valid integer
            value = 'invalid'
            debug = false
            """;

        assertThatThrownBy(() -> this.loadConfigWithErrorComments(IntegerConfig.class, configurer, toml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("value");
                // errorComments(true) should include both comment lines above the field
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'value' to Integer from String
                     --> 4:10
                      |
                    2 | # This is a comment about the value
                    3 | # Value must be a valid integer
                    4 | value = 'invalid'
                      |          ^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    /**
     * Tests that errorComments stops at non-comment lines (doesn't cross blank lines).
     */
    @ParameterizedTest(name = "{0}: errorComments stops at blank lines")
    @MethodSource("tomlConfigurers")
    void testError_ErrorCommentsOption_StopsAtBlankLines(String name, Configurer configurer) {
        String toml = """
            name = 'test'
            # This comment is separated by blank line

            # This is the only comment directly above
            value = 'invalid'
            """;

        assertThatThrownBy(() -> this.loadConfigWithErrorComments(IntegerConfig.class, configurer, toml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("value");
                // Only the comment directly above (no blank line) should be included
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'value' to Integer from String
                     --> 5:10
                      |
                    4 | # This is the only comment directly above
                    5 | value = 'invalid'
                      |          ^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    // ==================== Helper Methods ====================

    private <T extends OkaeriConfig> T loadConfig(Class<T> clazz, Configurer configurer, String toml) {
        T config = ConfigManager.create(clazz);
        config.setConfigurer(configurer);
        config.load(toml);
        return config;
    }

    private <T extends OkaeriConfig> T loadConfigWithErrorComments(Class<T> clazz, Configurer configurer, String toml) {
        T config = ConfigManager.create(clazz, it -> {
            it.configure(opt -> {
                opt.configurer(configurer);
                opt.errorComments(true);
            });
        });
        config.load(toml);
        return config;
    }

    private <T extends OkaeriConfig> T loadConfigWithCommons(Class<T> clazz, Configurer configurer, String toml) {
        T config = ConfigManager.create(clazz);
        configurer.register(new SerdesCommons());
        config.setConfigurer(configurer);
        config.load(toml);
        return config;
    }

    // ==================== Config Classes ====================

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class IntegerConfig extends OkaeriConfig {
        private int value;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class NestedConfig extends OkaeriConfig {
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
    public static class ListConfig extends OkaeriConfig {
        private List<Integer> numbers;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class MapConfig extends OkaeriConfig {
        private Map<String, Integer> limits;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class EnumConfig extends OkaeriConfig {
        private Level level = Level.MEDIUM;
    }

    public enum Level {
        LOW, MEDIUM, HIGH
    }

    // Deep nested config (3 levels): level1.level2.level3.value
    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class DeepNestedConfig extends OkaeriConfig {
        private Level1Config level1 = new Level1Config();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class Level1Config extends OkaeriConfig {
        private int value = 10;
        private Level2Config level2 = new Level2Config();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class Level2Config extends OkaeriConfig {
        private int value = 20;
        private Level3Config level3 = new Level3Config();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class Level3Config extends OkaeriConfig {
        private int value = 30;
    }

    // Server list config for array of tables tests
    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class ServerListConfig extends OkaeriConfig {
        private List<ServerConfig> servers;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class ServerConfig extends OkaeriConfig {
        private String host;
        private int port;
    }

    // Item list config for nested map in list tests
    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class ItemListConfig extends OkaeriConfig {
        private List<ItemConfig> items;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class ItemConfig extends OkaeriConfig {
        private String name;
        private Map<String, Integer> settings;
    }

    // Sibling subconfigs for accumulation test
    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class SiblingSubconfigsConfig extends OkaeriConfig {
        private SectionA sectionA = new SectionA();
        private SectionB sectionB = new SectionB();
        private SectionC sectionC = new SectionC();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class SectionA extends OkaeriConfig {
        private int valueA = 0;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class SectionB extends OkaeriConfig {
        private int valueB = 0;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class SectionC extends OkaeriConfig {
        private int valueC = 0;
    }

    // ==================== @CustomKey Config Classes ====================

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class CustomKeyConfig extends OkaeriConfig {
        @CustomKey("custom-value")
        private int myValue;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class NestedCustomKeyConfig extends OkaeriConfig {
        private SomeConfig some = new SomeConfig();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class SomeConfig extends OkaeriConfig {
        @CustomKey("custom-key")
        private InnerCustomKeyConfig customKey = new InnerCustomKeyConfig();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class InnerCustomKeyConfig extends OkaeriConfig {
        @CustomKey("nested-value")
        private int nestedValue;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class CustomKeyParentConfig extends OkaeriConfig {
        @CustomKey("custom-parent")
        private ChildConfig customParent = new ChildConfig();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class ChildConfig extends OkaeriConfig {
        private int value;
    }

    // ==================== @Names HYPHEN_CASE Config Classes ====================

    /**
     * Tests deeply nested config structure with @Names(strategy = HYPHEN_CASE)
     */
    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class ScoreboardConfig extends OkaeriConfig {
        private ScoreboardSection scoreboard = new ScoreboardSection();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @Names(strategy = NameStrategy.HYPHEN_CASE, modifier = NameModifier.TO_LOWER_CASE)
    public static class ScoreboardSection extends OkaeriConfig {
        private DummySection dummy = new DummySection();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @Names(strategy = NameStrategy.HYPHEN_CASE, modifier = NameModifier.TO_LOWER_CASE)
    public static class DummySection extends OkaeriConfig {
        private Duration updateRate = Duration.ofMinutes(1);
    }

    /**
     * Simple config with @Names HYPHEN_CASE for testing field name transformation.
     */
    @Data
    @EqualsAndHashCode(callSuper = false)
    @Names(strategy = NameStrategy.HYPHEN_CASE, modifier = NameModifier.TO_LOWER_CASE)
    public static class HyphenCaseConfig extends OkaeriConfig {
        private HyphenCaseSection mySection = new HyphenCaseSection();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @Names(strategy = NameStrategy.HYPHEN_CASE, modifier = NameModifier.TO_LOWER_CASE)
    public static class HyphenCaseSection extends OkaeriConfig {
        private int myValue;
    }
}
