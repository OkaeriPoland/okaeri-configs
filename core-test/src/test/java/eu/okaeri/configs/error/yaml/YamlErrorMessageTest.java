package eu.okaeri.configs.error.yaml;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.exception.OkaeriConfigException;
import eu.okaeri.configs.serdes.commons.SerdesCommons;
import eu.okaeri.configs.yaml.bukkit.YamlBukkitConfigurer;
import eu.okaeri.configs.yaml.bungee.YamlBungeeConfigurer;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests error messages for YAML configuration loading failures.
 * Parametrized across all YAML backends to ensure consistent error reporting.
 */
class YamlErrorMessageTest {

    static Stream<Arguments> yamlConfigurers() {
        return Stream.of(
            Arguments.of("SnakeYAML", new YamlSnakeYamlConfigurer()),
            Arguments.of("Bukkit", new YamlBukkitConfigurer()),
            Arguments.of("Bungee", new YamlBungeeConfigurer())
        );
    }

    // ==================== Standard Types (StandardSerdes) ====================

    @ParameterizedTest(name = "{0}: Invalid Integer")
    @MethodSource("yamlConfigurers")
    void testError_InvalidInteger(String name, Configurer configurer) {
        String yaml = "value: not_a_number";

        assertThatThrownBy(() -> this.loadConfig(IntegerConfig.class, configurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("value");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'value' to Integer from String
                     --> 1:8
                      |
                    1 | value: not_a_number
                      |        ^^^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    @ParameterizedTest(name = "{0}: Invalid Long")
    @MethodSource("yamlConfigurers")
    void testError_InvalidLong(String name, Configurer configurer) {
        String yaml = "value: not_a_long";

        assertThatThrownBy(() -> this.loadConfig(LongConfig.class, configurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("value");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToLongTransformer]: Cannot transform 'value' to Long from String
                     --> 1:8
                      |
                    1 | value: not_a_long
                      |        ^^^^^^^^^^ Expected long number (e.g. 42, -10, 9999999999)""");
            });
    }

    @ParameterizedTest(name = "{0}: Invalid Double")
    @MethodSource("yamlConfigurers")
    void testError_InvalidDouble(String name, Configurer configurer) {
        String yaml = "value: not_a_double";

        assertThatThrownBy(() -> this.loadConfig(DoubleConfig.class, configurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("value");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToDoubleTransformer]: Cannot transform 'value' to Double from String
                     --> 1:8
                      |
                    1 | value: not_a_double
                      |        ^^^^^^^^^^^^ Expected decimal number (e.g. 3.14, -0.5, 100.0)""");
            });
    }

    @ParameterizedTest(name = "{0}: Invalid Float")
    @MethodSource("yamlConfigurers")
    void testError_InvalidFloat(String name, Configurer configurer) {
        String yaml = "value: not_a_float";

        assertThatThrownBy(() -> this.loadConfig(FloatConfig.class, configurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("value");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToFloatTransformer]: Cannot transform 'value' to Float from String
                     --> 1:8
                      |
                    1 | value: not_a_float
                      |        ^^^^^^^^^^^ Expected float number (e.g. 3.14, -0.5, 100.0)""");
            });
    }

    @ParameterizedTest(name = "{0}: Invalid Boolean")
    @MethodSource("yamlConfigurers")
    void testError_InvalidBoolean(String name, Configurer configurer) {
        String yaml = "value: not_a_boolean";

        assertThatThrownBy(() -> this.loadConfig(BooleanConfig.class, configurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("value");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToBooleanTransformer]: Cannot transform 'value' to Boolean from String
                     --> 1:8
                      |
                    1 | value: not_a_boolean
                      |        ^^^^^^^^^^^^^ Expected true or false""");
            });
    }

    @ParameterizedTest(name = "{0}: Invalid UUID")
    @MethodSource("yamlConfigurers")
    void testError_InvalidUuid(String name, Configurer configurer) {
        String yaml = "value: not-a-valid-uuid";

        assertThatThrownBy(() -> this.loadConfig(UuidConfig.class, configurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("value");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToUuidTransformer]: Cannot transform 'value' to UUID from String
                     --> 1:8
                      |
                    1 | value: not-a-valid-uuid
                      |        ^^^^^^^^^^^^^^^^ Expected UUID (e.g. 550e8400-e29b-41d4-a716-446655440000)""");
            });
    }

    @ParameterizedTest(name = "{0}: Invalid BigDecimal")
    @MethodSource("yamlConfigurers")
    void testError_InvalidBigDecimal(String name, Configurer configurer) {
        String yaml = "value: not_a_decimal";

        assertThatThrownBy(() -> this.loadConfig(BigDecimalConfig.class, configurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("value");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToBigDecimalTransformer]: Cannot transform 'value' to BigDecimal from String
                     --> 1:8
                      |
                    1 | value: not_a_decimal
                      |        ^^^^^^^^^^^^^ Expected precise decimal (e.g. 3.14, -100.50, 0)""");
            });
    }

    @ParameterizedTest(name = "{0}: Invalid BigInteger")
    @MethodSource("yamlConfigurers")
    void testError_InvalidBigInteger(String name, Configurer configurer) {
        String yaml = "value: not_an_integer";

        assertThatThrownBy(() -> this.loadConfig(BigIntegerConfig.class, configurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("value");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToBigIntegerTransformer]: Cannot transform 'value' to BigInteger from String
                     --> 1:8
                      |
                    1 | value: not_an_integer
                      |        ^^^^^^^^^^^^^^ Expected precise integer (e.g. 42, -10, 0)""");
            });
    }

    // ==================== Serdes Commons Types ====================

    @ParameterizedTest(name = "{0}: Invalid Duration")
    @MethodSource("yamlConfigurers")
    void testError_InvalidDuration(String name, Configurer configurer) {
        String yaml = "value: not_a_duration";

        assertThatThrownBy(() -> this.loadConfigWithCommons(DurationConfig.class, configurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("value");
                assertThat(e.getMessage()).isEqualTo("""
                    error[DurationTransformer]: Cannot transform 'value' to Duration from String
                     --> 1:8
                      |
                    1 | value: not_a_duration
                      |        ^^^^^^^^^^^^^^ Text cannot be parsed to a Duration""");
            });
    }

    @ParameterizedTest(name = "{0}: Invalid Instant")
    @MethodSource("yamlConfigurers")
    void testError_InvalidInstant(String name, Configurer configurer) {
        String yaml = "value: not_an_instant";

        assertThatThrownBy(() -> this.loadConfigWithCommons(InstantConfig.class, configurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("value");
                assertThat(e.getMessage()).isEqualTo("""
                    error[InstantSerializer]: Cannot deserialize 'value' to Instant from String
                     --> 1:8
                      |
                    1 | value: not_an_instant
                      |        ^^^^^^^^^^^^^^ Text 'not_an_instant' could not be parsed at index 0""");
            });
    }

    @ParameterizedTest(name = "{0}: Invalid Locale")
    @MethodSource("yamlConfigurers")
    void testError_InvalidLocale(String name, Configurer configurer) {
        String yaml = "value: 12345";

        assertThatThrownBy(() -> this.loadConfigWithCommons(LocaleConfig.class, configurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("value");
                assertThat(e.getMessage()).isEqualTo("""
                    error[LocaleTransformer]: Cannot transform 'value' to Locale from Integer
                     --> 1:8
                      |
                    1 | value: 12345
                      |        ^^^^^ Expected locale (e.g. en, en-US, pl-PL)""");
            });
    }

    @ParameterizedTest(name = "{0}: Invalid Pattern")
    @MethodSource("yamlConfigurers")
    void testError_InvalidPattern(String name, Configurer configurer) {
        String yaml = """
            value: "[invalid(regex\"""";

        assertThatThrownBy(() -> this.loadConfigWithCommons(PatternConfig.class, configurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("value");
                assertThat(e.getMessage()).isEqualTo("""
                    error[PatternTransformer]: Cannot transform 'value' to Pattern from String
                     --> 1:22
                      |
                    1 | value: "[invalid(regex"
                      |                      ^ Unclosed character class""");
            });
    }

    // ==================== Nested Paths ====================

    @ParameterizedTest(name = "{0}: Invalid nested field")
    @MethodSource("yamlConfigurers")
    void testError_NestedField(String name, Configurer configurer) {
        String yaml = """
            database:
              host: localhost
              port: not_a_port
            """;

        assertThatThrownBy(() -> this.loadConfig(NestedConfig.class, configurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("database.port");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'database.port' to Integer from String
                     --> 3:9
                      |
                    3 |   port: not_a_port
                      |         ^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    @ParameterizedTest(name = "{0}: Invalid deeply nested field")
    @MethodSource("yamlConfigurers")
    void testError_DeeplyNestedField(String name, Configurer configurer) {
        String yaml = """
            level1:
              level2:
                level3:
                  value: not_an_int
            """;

        assertThatThrownBy(() -> this.loadConfig(DeepNestedConfig.class, configurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("level1.level2.level3.value");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'level1.level2.level3.value' to Integer from String
                     --> 4:14
                      |
                    4 |       value: not_an_int
                      |              ^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    // ==================== List Paths ====================

    @ParameterizedTest(name = "{0}: Invalid list element")
    @MethodSource("yamlConfigurers")
    void testError_ListElement(String name, Configurer configurer) {
        String yaml = """
            numbers:
              - 1
              - 2
              - not_a_number
              - 4
            """;

        assertThatThrownBy(() -> this.loadConfig(ListConfig.class, configurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("numbers[2]");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'numbers[2]' to Integer from String
                     --> 4:5
                      |
                    4 |   - not_a_number
                      |     ^^^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    @ParameterizedTest(name = "{0}: Invalid nested list element field")
    @MethodSource("yamlConfigurers")
    void testError_NestedListElement(String name, Configurer configurer) {
        String yaml = """
            servers:
              - host: server1.com
                port: 8080
              - host: server2.com
                port: invalid
            """;

        assertThatThrownBy(() -> this.loadConfig(ServerListConfig.class, configurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("servers[1].port");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'servers[1].port' to Integer from String
                     --> 5:11
                      |
                    5 |     port: invalid
                      |           ^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    // ==================== Map Paths ====================

    @ParameterizedTest(name = "{0}: Invalid map value")
    @MethodSource("yamlConfigurers")
    void testError_MapValue(String name, Configurer configurer) {
        String yaml = """
            limits:
              daily: 100
              weekly: not_a_number
            """;

        assertThatThrownBy(() -> this.loadConfig(MapConfig.class, configurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("limits[\"weekly\"]");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'limits["weekly"]' to Integer from String
                     --> 3:11
                      |
                    3 |   weekly: not_a_number
                      |           ^^^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    // ==================== Enum Errors ====================

    @ParameterizedTest(name = "{0}: Invalid enum value")
    @MethodSource("yamlConfigurers")
    void testError_InvalidEnum(String name, Configurer configurer) {
        String yaml = "level: MDIUM";

        assertThatThrownBy(() -> this.loadConfig(EnumConfig.class, configurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("level");
                assertThat(e.getMessage()).isEqualTo("""
                    Cannot resolve 'level' to Level from String
                     --> 1:8
                      |
                    1 | level: MDIUM
                      |        ^^^^^ Expected MEDIUM, HIGH or LOW""");
            });
    }

    // ==================== Quoted Values ====================

    @ParameterizedTest(name = "{0}: Invalid Integer (single quoted)")
    @MethodSource("yamlConfigurers")
    void testError_InvalidInteger_SingleQuoted(String name, Configurer configurer) {
        String yaml = "value: 'not_a_number'";

        assertThatThrownBy(() -> this.loadConfig(IntegerConfig.class, configurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("value");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'value' to Integer from String
                     --> 1:9
                      |
                    1 | value: 'not_a_number'
                      |         ^^^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    @ParameterizedTest(name = "{0}: Invalid Integer (double quoted)")
    @MethodSource("yamlConfigurers")
    void testError_InvalidInteger_DoubleQuoted(String name, Configurer configurer) {
        String yaml = "value: \"not_a_number\"";

        assertThatThrownBy(() -> this.loadConfig(IntegerConfig.class, configurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("value");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'value' to Integer from String
                     --> 1:9
                      |
                    1 | value: "not_a_number"
                      |         ^^^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    @ParameterizedTest(name = "{0}: Invalid enum (single quoted)")
    @MethodSource("yamlConfigurers")
    void testError_InvalidEnum_SingleQuoted(String name, Configurer configurer) {
        String yaml = "level: 'MDIUM'";

        assertThatThrownBy(() -> this.loadConfig(EnumConfig.class, configurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("level");
                assertThat(e.getMessage()).isEqualTo("""
                    Cannot resolve 'level' to Level from String
                     --> 1:9
                      |
                    1 | level: 'MDIUM'
                      |         ^^^^^ Expected MEDIUM, HIGH or LOW""");
            });
    }

    @ParameterizedTest(name = "{0}: Invalid nested field (single quoted)")
    @MethodSource("yamlConfigurers")
    void testError_NestedField_SingleQuoted(String name, Configurer configurer) {
        String yaml = """
            database:
              host: localhost
              port: 'not_a_port'
            """;

        assertThatThrownBy(() -> this.loadConfig(NestedConfig.class, configurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("database.port");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'database.port' to Integer from String
                     --> 3:10
                      |
                    3 |   port: 'not_a_port'
                      |          ^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    @ParameterizedTest(name = "{0}: Invalid list element (single quoted)")
    @MethodSource("yamlConfigurers")
    void testError_ListElement_SingleQuoted(String name, Configurer configurer) {
        String yaml = """
            numbers:
              - 1
              - 2
              - 'not_a_number'
              - 4
            """;

        assertThatThrownBy(() -> this.loadConfig(ListConfig.class, configurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("numbers[2]");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'numbers[2]' to Integer from String
                     --> 4:6
                      |
                    4 |   - 'not_a_number'
                      |      ^^^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    @ParameterizedTest(name = "{0}: Invalid map value (single quoted)")
    @MethodSource("yamlConfigurers")
    void testError_MapValue_SingleQuoted(String name, Configurer configurer) {
        String yaml = """
            limits:
              daily: 100
              weekly: 'not_a_number'
            """;

        assertThatThrownBy(() -> this.loadConfig(MapConfig.class, configurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("limits[\"weekly\"]");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'limits["weekly"]' to Integer from String
                     --> 3:12
                      |
                    3 |   weekly: 'not_a_number'
                      |            ^^^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    // ==================== Complex Nested Paths ====================

    @ParameterizedTest(name = "{0}: Map with nested object value")
    @MethodSource("yamlConfigurers")
    void testError_MapWithNestedObject(String name, Configurer configurer) {
        String yaml = """
            settings:
              db:
                host: localhost
                port: invalid
            """;

        assertThatThrownBy(() -> this.loadConfig(MapNestedConfig.class, configurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("settings[\"db\"].port");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'settings["db"].port' to Integer from String
                     --> 4:11
                      |
                    4 |     port: invalid
                      |           ^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    @ParameterizedTest(name = "{0}: List of maps")
    @MethodSource("yamlConfigurers")
    void testError_ListOfMaps(String name, Configurer configurer) {
        String yaml = """
            items:
              - first: 10
                second: 20
              - first: 30
                second: invalid
            """;

        assertThatThrownBy(() -> this.loadConfig(ListOfMapsConfig.class, configurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("items[1][\"second\"]");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'items[1]["second"]' to Integer from String
                     --> 5:13
                      |
                    5 |     second: invalid
                      |             ^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    @ParameterizedTest(name = "{0}: Nested object with map field")
    @MethodSource("yamlConfigurers")
    void testError_NestedObjectWithMap(String name, Configurer configurer) {
        String yaml = """
            server:
              host: example.com
              settings:
                timeout: invalid
            """;

        assertThatThrownBy(() -> this.loadConfig(ServerWithSettingsConfig.class, configurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("server.settings[\"timeout\"]");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'server.settings["timeout"]' to Integer from String
                     --> 4:14
                      |
                    4 |     timeout: invalid
                      |              ^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    @ParameterizedTest(name = "{0}: Deployment config (map -> list -> nested -> nested)")
    @MethodSource("yamlConfigurers")
    void testError_DeploymentConfig(String name, Configurer configurer) {
        String yaml = """
            environments:
              production:
                clusters:
                  - name: us-east
                    nodes:
                      - host: node1.example.com
                        resources:
                          memory: 8GB
                          cpu: invalid_cores
            """;

        assertThatThrownBy(() -> this.loadConfig(DeploymentConfig.class, configurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("environments[\"production\"].clusters[0].nodes[0].resources.cpu");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'environments["production"].clusters[0].nodes[0].resources.cpu' to Integer from String
                     --> 9:20
                      |
                    9 |               cpu: invalid_cores
                      |                    ^^^^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    // ==================== Helper Methods ====================

    private <T extends OkaeriConfig> T loadConfig(Class<T> clazz, Configurer configurer, String yaml) {
        T config = ConfigManager.create(clazz);
        config.setConfigurer(configurer);
        config.load(yaml);
        return config;
    }

    private <T extends OkaeriConfig> T loadConfigWithCommons(Class<T> clazz, Configurer configurer, String yaml) {
        T config = ConfigManager.create(clazz);
        configurer.register(new SerdesCommons());
        config.setConfigurer(configurer);
        config.load(yaml);
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
    public static class LongConfig extends OkaeriConfig {
        private long value;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class DoubleConfig extends OkaeriConfig {
        private double value;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class FloatConfig extends OkaeriConfig {
        private float value;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class BooleanConfig extends OkaeriConfig {
        private boolean value;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class UuidConfig extends OkaeriConfig {
        private UUID value;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class BigDecimalConfig extends OkaeriConfig {
        private BigDecimal value;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class BigIntegerConfig extends OkaeriConfig {
        private BigInteger value;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class DurationConfig extends OkaeriConfig {
        private Duration value;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class InstantConfig extends OkaeriConfig {
        private Instant value;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class LocaleConfig extends OkaeriConfig {
        private Locale value;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class PatternConfig extends OkaeriConfig {
        private Pattern value;
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
    public static class DeepNestedConfig extends OkaeriConfig {
        private Level1Config level1 = new Level1Config();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class Level1Config extends OkaeriConfig {
        private Level2Config level2 = new Level2Config();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class Level2Config extends OkaeriConfig {
        private Level3Config level3 = new Level3Config();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class Level3Config extends OkaeriConfig {
        private int value = 100;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class ListConfig extends OkaeriConfig {
        private List<Integer> numbers;
    }

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

    // Map with nested object values: settings["db"].port
    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class MapNestedConfig extends OkaeriConfig {
        private Map<String, DatabaseConfig> settings;
    }

    // List of maps: items[1]["count"]
    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class ListOfMapsConfig extends OkaeriConfig {
        private List<Map<String, Integer>> items;
    }

    // Nested object with map: server.settings["timeout"]
    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class ServerWithSettingsConfig extends OkaeriConfig {
        private ServerWithSettings server;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class ServerWithSettings extends OkaeriConfig {
        private String host;
        private Map<String, Integer> settings;
    }

    // Deployment config: environments["production"].clusters[0].nodes[0].resources.cpu
    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class DeploymentConfig extends OkaeriConfig {
        private Map<String, EnvironmentConfig> environments;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class EnvironmentConfig extends OkaeriConfig {
        private List<ClusterConfig> clusters;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class ClusterConfig extends OkaeriConfig {
        private String name;
        private List<NodeConfig> nodes;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class NodeConfig extends OkaeriConfig {
        private String host;
        private ResourceConfig resources;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class ResourceConfig extends OkaeriConfig {
        private String memory;
        private int cpu;
    }
}
