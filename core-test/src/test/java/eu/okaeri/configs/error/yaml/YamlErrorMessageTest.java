package eu.okaeri.configs.error.yaml;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.CustomKey;
import eu.okaeri.configs.annotation.NameModifier;
import eu.okaeri.configs.annotation.NameStrategy;
import eu.okaeri.configs.annotation.Names;
import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.configurer.WrappedConfigurer;
import eu.okaeri.configs.exception.OkaeriConfigException;
import eu.okaeri.configs.exception.ValueIndexedException;
import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.ObjectTransformer;
import eu.okaeri.configs.serdes.SerdesContext;
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
                      |        ^^^^^^^^^^^^^^ Expected duration (e.g. 30s, 5m, 1h30m, 1d)""");
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
                      |        ^^^^^^^^^^^^^^ Expected ISO-8601 instant (e.g. 2006-01-02T15:04:05Z)""");
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

    // ==================== WrappedConfigurer Integration Tests ====================

    /**
     * Integration test for WrappedConfigurer wrapping format configurer.
     * <p>
     * This tests that rawContent is properly propagated to the wrapped configurer
     * so that createSourceWalker() (which is delegated) can access it via this.getRawContent().
     */
    @ParameterizedTest(name = "{0}: WrappedConfigurer integration - @Names HYPHEN_CASE subconfig")
    @MethodSource("yamlConfigurers")
    void testError_WrappedConfigurer_HyphenCaseSubconfig(String name, Configurer configurer) {
        String yaml = """
            scoreboard:
              dummy:
                update-rate: invalid_duration
            """;

        // Wrap the configurer (simulates validator wrapping format configurer)
        WrappedConfigurer wrappedConfigurer = new WrappedConfigurer(configurer);
        wrappedConfigurer.register(new SerdesCommons());

        assertThatThrownBy(() -> this.loadConfig(ScoreboardConfig.class, wrappedConfigurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("scoreboard.dummy.update-rate");
                assertThat(e.getMessage()).isEqualTo("""
                    error[DurationTransformer]: Cannot transform 'scoreboard.dummy.update-rate' to Duration from String
                     --> 3:18
                      |
                    3 |     update-rate: invalid_duration
                      |                  ^^^^^^^^^^^^^^^^ Expected duration (e.g. 30s, 5m, 1h30m, 1d)""");
            });
    }

    /**
     * Integration test for WrappedConfigurer with @CustomKey annotation.
     * Ensures source walker works correctly when createSourceWalker() is delegated.
     */
    @ParameterizedTest(name = "{0}: WrappedConfigurer integration - @CustomKey")
    @MethodSource("yamlConfigurers")
    void testError_WrappedConfigurer_CustomKey(String name, Configurer configurer) {
        String yaml = "custom-value: not_a_number";

        // Wrap the configurer (simulates validator wrapping format configurer)
        WrappedConfigurer wrappedConfigurer = new WrappedConfigurer(configurer);

        assertThatThrownBy(() -> this.loadConfig(CustomKeyConfig.class, wrappedConfigurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("custom-value");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'custom-value' to Integer from String
                     --> 1:15
                      |
                    1 | custom-value: not_a_number
                      |               ^^^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
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
        // Register custom test transformers for range testing
        configurer.register(registry -> {
            registry.register(new RangeTestTransformer());
            registry.register(new MultiLineRangeTestTransformer());
            registry.register(new MiddleLineRangeTestTransformer());
            registry.register(new SingleCharMultilineTransformer());
            // Context lines control transformers
            registry.register(new ContextBefore3Transformer());
            registry.register(new ContextAfter3Transformer());
            registry.register(new ContextBoth3Transformer());
            registry.register(new ContextBefore1After1Transformer());
        });
        config.setConfigurer(configurer);
        config.load(yaml);
        return config;
    }

    private <T extends OkaeriConfig> T loadConfigWithErrorComments(Class<T> clazz, Configurer configurer, String yaml) {
        T config = ConfigManager.create(clazz, it -> {
            it.configure(opt -> {
                opt.configurer(configurer);
                opt.errorComments(true);
            });
        });
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

    // ==================== Multiline String Styles ====================

    @ParameterizedTest(name = "{0}: Invalid Integer in literal block scalar (|)")
    @MethodSource("yamlConfigurers")
    void testError_LiteralBlockScalar(String name, Configurer configurer) {
        String yaml = """
            value: |
              not_a_number
            """;

        assertThatThrownBy(() -> this.loadConfig(IntegerConfig.class, configurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("value");
                // Shows key line and content for multiline block
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'value' to Integer from String
                     --> 2:3
                      |
                    1 | value: |
                    2 |   not_a_number
                      |   ^^^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    @ParameterizedTest(name = "{0}: Invalid Integer in folded block scalar (>)")
    @MethodSource("yamlConfigurers")
    void testError_FoldedBlockScalar(String name, Configurer configurer) {
        String yaml = """
            value: >
              not_a_number
            """;

        assertThatThrownBy(() -> this.loadConfig(IntegerConfig.class, configurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("value");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'value' to Integer from String
                     --> 2:3
                      |
                    1 | value: >
                    2 |   not_a_number
                      |   ^^^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    @ParameterizedTest(name = "{0}: Invalid Integer in literal block with strip (|-)")
    @MethodSource("yamlConfigurers")
    void testError_LiteralBlockScalarStrip(String name, Configurer configurer) {
        String yaml = """
            value: |-
              not_a_number
            """;

        assertThatThrownBy(() -> this.loadConfig(IntegerConfig.class, configurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("value");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'value' to Integer from String
                     --> 2:3
                      |
                    1 | value: |-
                    2 |   not_a_number
                      |   ^^^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    @ParameterizedTest(name = "{0}: Invalid Integer in literal block with keep (|+)")
    @MethodSource("yamlConfigurers")
    void testError_LiteralBlockScalarKeep(String name, Configurer configurer) {
        String yaml = """
            value: |+
              not_a_number
            """;

        assertThatThrownBy(() -> this.loadConfig(IntegerConfig.class, configurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("value");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'value' to Integer from String
                     --> 2:3
                      |
                    1 | value: |+
                    2 |   not_a_number
                      |   ^^^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    @ParameterizedTest(name = "{0}: Invalid Integer in nested literal block scalar")
    @MethodSource("yamlConfigurers")
    void testError_NestedLiteralBlockScalar(String name, Configurer configurer) {
        String yaml = """
            database:
              host: localhost
              port: |
                not_a_port
            """;

        assertThatThrownBy(() -> this.loadConfig(NestedConfig.class, configurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("database.port");
                // Shows key line and content for multiline block
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'database.port' to Integer from String
                     --> 4:5
                      |
                    3 |   port: |
                    4 |     not_a_port
                      |     ^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    @ParameterizedTest(name = "{0}: Invalid enum in literal block scalar")
    @MethodSource("yamlConfigurers")
    void testError_EnumLiteralBlockScalar(String name, Configurer configurer) {
        String yaml = """
            level: |
              MDIUM
            """;

        assertThatThrownBy(() -> this.loadConfig(EnumConfig.class, configurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("level");
                // Shows key line and content for multiline block
                assertThat(e.getMessage()).isEqualTo("""
                    Cannot resolve 'level' to Level from String
                     --> 2:3
                      |
                    1 | level: |
                    2 |   MDIUM
                      |   ^^^^^ Expected MEDIUM, HIGH or LOW""");
            });
    }

    @ParameterizedTest(name = "{0}: Multiline literal block with multiple lines - first line error")
    @MethodSource("yamlConfigurers")
    void testError_MultilineLiteralBlock(String name, Configurer configurer) {
        String yaml = """
            value: |
              not_valid
              second line
              third line
            """;

        assertThatThrownBy(() -> this.loadConfig(IntegerConfig.class, configurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("value");
                // The error points to where the multiline content starts
                // Points to last content line, showing all lines from key to bottom
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'value' to Integer from String
                     --> 4:3
                      |
                    1 | value: |
                    2 |   not_valid
                    3 |   second line
                    4 |   third line
                      |   ^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    @ParameterizedTest(name = "{0}: Invalid Pattern in literal block - specific character")
    @MethodSource("yamlConfigurers")
    void testError_PatternLiteralBlockScalar_SpecificChar(String name, Configurer configurer) {
        // Pattern with unclosed bracket - error at specific position within multiline content
        String yaml = """
            value: |
              [invalid(regex
            """;

        assertThatThrownBy(() -> this.loadConfigWithCommons(PatternConfig.class, configurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("value");
                // Should point to specific character within the multiline content
                assertThat(e.getMessage()).isEqualTo("""
                    error[PatternTransformer]: Cannot transform 'value' to Pattern from String
                     --> 2:16
                      |
                    1 | value: |
                    2 |   [invalid(regex
                      |                ^ Unclosed character class""");
            });
    }

    @ParameterizedTest(name = "{0}: Single char error in middle of multiline content")
    @MethodSource("yamlConfigurers")
    void testError_SingleCharMultiline_ErrorInMiddle(String name, Configurer configurer) {
        // Custom transformer throws at index 13 (the '[' character position)
        // Content: "^start\nmiddle[broken\nend$" - index 13 is '[' on line 2 of content (line 3 in file)
        String yaml = """
            value: |
              ^start
              middle[broken
              end$
            """;

        assertThatThrownBy(() -> this.loadConfigWithCommons(SingleCharMultilineConfig.class, configurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("value");
                // Index 13 points to '[' on line 3 (content line 2)
                // Should show key line, content lines, error marker, and remaining content
                assertThat(e.getMessage()).isEqualTo("""
                    error[SingleCharMultilineTransformer]: Cannot transform 'value' to SingleCharMultilineType from String
                     --> 3:9
                      |
                    1 | value: |
                    2 |   ^start
                    3 |   middle[broken
                      |         ^ Invalid character at position 13
                    4 |   end$""");
            });
    }

    @ParameterizedTest(name = "{0}: Pattern error detected at end of multiline block")
    @MethodSource("yamlConfigurers")
    void testError_PatternMultiline_ErrorAtEnd(String name, Configurer configurer) {
        // Pattern with unclosed bracket - error is detected at end of string
        // The regex "[a-z\n0-9" has unclosed [ detected at end
        // PatternSyntaxException.getIndex() returns position past last char
        String yaml = """
            value: |
              [a-z
              0-9
            """;

        assertThatThrownBy(() -> this.loadConfigWithCommons(PatternConfig.class, configurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("value");
                // Error detected at end of content - points to last character
                // Content: "[a-z\n0-9" - last char is '9' at column 5 (0-indexed: 4)
                assertThat(e.getMessage()).isEqualTo("""
                    error[PatternTransformer]: Cannot transform 'value' to Pattern from String
                     --> 3:5
                      |
                    1 | value: |
                    2 |   [a-z
                    3 |   0-9
                      |     ^ Unclosed character class""");
            });
    }

    @ParameterizedTest(name = "{0}: Long multiline with ellipsis")
    @MethodSource("yamlConfigurers")
    void testError_LongMultilineWithEllipsis(String name, Configurer configurer) {
        // More than 5 lines between key and error - should show ellipsis
        String yaml = """
            value: |
              line1
              line2
              line3
              line4
              line5
              line6
              not_a_number
            """;

        assertThatThrownBy(() -> this.loadConfig(IntegerConfig.class, configurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("value");
                // Should show key line, 2 lines after, ellipsis, 2 lines before error, and error line
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'value' to Integer from String
                     --> 8:3
                      |
                    1 | value: |
                    2 |   line1
                    3 |   line2
                      | ... (2 more lines)
                    6 |   line5
                    7 |   line6
                    8 |   not_a_number
                      |   ^^^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    @ParameterizedTest(name = "{0}: ValueIndexedException range within single line in multiline block")
    @MethodSource("yamlConfigurers")
    void testError_MultilineRangeWithinSingleLine(String name, Configurer configurer) {
        // Test that startIndex + length highlights a range within a single line
        // Uses RangeTestConfig with custom transformer that throws ValueIndexedException
        String yaml = """
            value: |
              hello world foo bar
            """;

        assertThatThrownBy(() -> this.loadConfigWithCommons(RangeTestConfig.class, configurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("value");
                // RangeTestTransformer throws error at index 6, length 5 ("world")
                assertThat(e.getMessage()).isEqualTo("""
                    error[RangeTestTransformer]: Cannot transform 'value' to RangeTestType from String
                     --> 2:9
                      |
                    1 | value: |
                    2 |   hello world foo bar
                      |         ^^^^^ Invalid word at position 6""");
            });
    }

    @ParameterizedTest(name = "{0}: ValueIndexedException range spanning multiple lines in multiline block")
    @MethodSource("yamlConfigurers")
    void testError_MultilineRangeSpanningLines(String name, Configurer configurer) {
        // Test that startIndex + length can span multiple lines
        // Range starts on line 2 and ends on line 3
        String yaml = """
            value: |
              hello world
              foo bar
            """;

        assertThatThrownBy(() -> this.loadConfigWithCommons(MultiLineRangeTestConfig.class, configurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("value");
                // MultiLineRangeTestTransformer throws error at index 6, length 13 ("world\nfoo bar")
                // Spans from "world" on line 2 to "bar" on line 3
                assertThat(e.getMessage()).isEqualTo("""
                    error[MultiLineRangeTestTransformer]: Cannot transform 'value' to MultiLineRangeTestType from String
                     --> 2:9
                      |
                    1 | value: |
                    2 |   hello world
                      |         ^^^^^
                    3 |   foo bar
                      |   ^^^^^^^ Invalid range spanning lines""");
            });
    }

    @ParameterizedTest(name = "{0}: ValueIndexedException range on middle line only in 3-line multiline block")
    @MethodSource("yamlConfigurers")
    void testError_MultilineRangeMiddleLineOnly(String name, Configurer configurer) {
        // Test error on middle line (line 3) with content before and after
        // 3 content lines: line 2 = "first", line 3 = "hello world", line 4 = "last"
        String yaml = """
            value: |
              first
              hello world
              last
            """;

        assertThatThrownBy(() -> this.loadConfigWithCommons(MiddleLineRangeTestConfig.class, configurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("value");
                // MiddleLineRangeTestTransformer throws error at index 12, length 5 ("world" on line 3)
                // "first\nhello world\nlast" - index 12 is 'w' in "world"
                assertThat(e.getMessage()).isEqualTo("""
                    error[MiddleLineRangeTestTransformer]: Cannot transform 'value' to MiddleLineRangeTestType from String
                     --> 3:9
                      |
                    1 | value: |
                    2 |   first
                    3 |   hello world
                      |         ^^^^^ Invalid word in middle
                    4 |   last""");
            });
    }

    // ==================== ValueIndexedException Context Lines Tests ====================
    // These tests verify that contextLinesBefore/After shows surrounding YAML content
    // (other fields, comments) for better error context - NOT multiline block content

    @ParameterizedTest(name = "{0}: contextLinesBefore=2 shows 2 lines before error field")
    @MethodSource("yamlConfigurers")
    void testError_ContextLinesBefore_ShowsSurroundingFields(String name, Configurer configurer) {
        // Regular YAML with multiple fields - error on 'value' field
        // contextLinesBefore=2 should show the 2 lines before the error line
        String yaml = """
            # Configuration file
            name: test
            count: 42
            value: not_an_int
            enabled: true
            """;

        assertThatThrownBy(() -> this.loadConfig(IntegerConfig.class, configurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("value");
                // Default context is 0, so only error line shown
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'value' to Integer from String
                     --> 4:8
                      |
                    4 | value: not_an_int
                      |        ^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    @ParameterizedTest(name = "{0}: contextLinesBefore=2, contextLinesAfter=1 shows surrounding context")
    @MethodSource("yamlConfigurers")
    void testError_ContextBothDirections_ShowsSurroundingFields(String name, Configurer configurer) {
        // Test with nested config where we can control context via custom transformer
        // Using ContextBefore1After1Config which has contextLinesBefore=1, contextLinesAfter=1
        String yaml = """
            # Header comment
            name: test
            value: hello world
            count: 42
            enabled: true
            """;

        assertThatThrownBy(() -> this.loadConfigWithCommons(ContextBefore1After1Config.class, configurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("value");
                // contextLinesBefore=1 shows 1 line before, contextLinesAfter=1 shows 1 line after
                assertThat(e.getMessage()).isEqualTo("""
                    error[ContextBefore1After1Transformer]: Cannot transform 'value' to ContextBefore1After1Type from String
                     --> 3:14
                      |
                    2 | name: test
                    3 | value: hello world
                      |              ^^^^^ Error with custom context
                    4 | count: 42""");
            });
    }

    @ParameterizedTest(name = "{0}: contextLinesBefore=3 shows comment and fields before error")
    @MethodSource("yamlConfigurers")
    void testError_ContextShowsComments(String name, Configurer configurer) {
        // Test that context includes comments above the error field
        String yaml = """
            # Main configuration
            # Version: 1.0
            name: myapp
            # The value below is important
            value: hello world
            debug: false
            """;

        assertThatThrownBy(() -> this.loadConfigWithCommons(ContextBefore3Config.class, configurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("value");
                // contextLinesBefore=3 shows the comment and preceding fields
                // Index 30 is beyond "hello world" (11 chars), so highlights entire value from column 8
                assertThat(e.getMessage()).isEqualTo("""
                    error[ContextBefore3Transformer]: Cannot transform 'value' to ContextBefore3Type from String
                     --> 5:8
                      |
                    2 | # Version: 1.0
                    3 | name: myapp
                    4 | # The value below is important
                    5 | value: hello world
                      |        ^^^^^^^^^^^ Error with 3 lines before""");
            });
    }

    // ==================== errorComments Option Test ====================

    /**
     * Tests that errorComments(true) config option automatically includes
     * all consecutive comment lines above the error field.
     */
    @ParameterizedTest(name = "{0}: errorComments includes consecutive comments above field")
    @MethodSource("yamlConfigurers")
    void testError_ErrorCommentsOption_IncludesConsecutiveComments(String name, Configurer configurer) {
        String yaml = """
            name: test
            # This is a comment about the value
            # Value must be a valid integer
            value: invalid
            debug: false
            """;

        assertThatThrownBy(() -> this.loadConfigWithErrorComments(IntegerConfig.class, configurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("value");
                // errorComments(true) should include both comment lines above the field
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'value' to Integer from String
                     --> 4:8
                      |
                    2 | # This is a comment about the value
                    3 | # Value must be a valid integer
                    4 | value: invalid
                      |        ^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    /**
     * Tests that errorComments stops at non-comment lines (doesn't cross blank lines).
     */
    @ParameterizedTest(name = "{0}: errorComments stops at blank lines")
    @MethodSource("yamlConfigurers")
    void testError_ErrorCommentsOption_StopsAtBlankLines(String name, Configurer configurer) {
        String yaml = """
            name: test
            # This comment is separated by blank line

            # This is the only comment directly above
            value: invalid
            """;

        assertThatThrownBy(() -> this.loadConfigWithErrorComments(IntegerConfig.class, configurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("value");
                // Only the comment directly above (no blank line) should be included
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'value' to Integer from String
                     --> 5:8
                      |
                    4 | # This is the only comment directly above
                    5 | value: invalid
                      |        ^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    // ==================== @CustomKey Annotation Tests ====================

    /**
     * Tests that error messages work correctly with @CustomKey annotation.
     * The YAML uses the custom key name (e.g., "custom-value"), and the error
     * message should correctly locate and display the source line.
     */
    @ParameterizedTest(name = "{0}: @CustomKey simple field")
    @MethodSource("yamlConfigurers")
    void testError_CustomKey_SimpleField(String name, Configurer configurer) {
        String yaml = "custom-value: not_a_number";

        assertThatThrownBy(() -> this.loadConfig(CustomKeyConfig.class, configurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("custom-value");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'custom-value' to Integer from String
                     --> 1:15
                      |
                    1 | custom-value: not_a_number
                      |               ^^^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    /**
     * Tests that error messages work correctly with nested @CustomKey annotations.
     * Both the parent field and nested field use custom key names.
     */
    @ParameterizedTest(name = "{0}: @CustomKey nested field")
    @MethodSource("yamlConfigurers")
    void testError_CustomKey_NestedField(String name, Configurer configurer) {
        String yaml = """
            some:
              custom-key:
                nested-value: not_a_number
            """;

        assertThatThrownBy(() -> this.loadConfig(NestedCustomKeyConfig.class, configurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("some.custom-key.nested-value");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'some.custom-key.nested-value' to Integer from String
                     --> 3:19
                      |
                    3 |     nested-value: not_a_number
                      |                   ^^^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    /**
     * Tests that error messages work correctly when only the parent has @CustomKey.
     */
    @ParameterizedTest(name = "{0}: @CustomKey on parent only")
    @MethodSource("yamlConfigurers")
    void testError_CustomKey_ParentOnly(String name, Configurer configurer) {
        String yaml = """
            custom-parent:
              value: not_a_number
            """;

        assertThatThrownBy(() -> this.loadConfig(CustomKeyParentConfig.class, configurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("custom-parent.value");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'custom-parent.value' to Integer from String
                     --> 2:10
                      |
                    2 |   value: not_a_number
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
    @MethodSource("yamlConfigurers")
    void testError_Names_HyphenCase_NestedField(String name, Configurer configurer) {
        String yaml = """
            scoreboard:
              dummy:
                update-rate: hello
            """;

        assertThatThrownBy(() -> this.loadConfigWithCommons(ScoreboardConfig.class, configurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("scoreboard.dummy.update-rate");
                assertThat(e.getMessage()).isEqualTo("""
                    error[DurationTransformer]: Cannot transform 'scoreboard.dummy.update-rate' to Duration from String
                     --> 3:18
                      |
                    3 |     update-rate: hello
                      |                  ^^^^^ Expected duration (e.g. 30s, 5m, 1h30m, 1d)""");
            });
    }

    /**
     * Tests @Names HYPHEN_CASE with a simple nested field.
     */
    @ParameterizedTest(name = "{0}: @Names HYPHEN_CASE simple nested")
    @MethodSource("yamlConfigurers")
    void testError_Names_HyphenCase_SimpleNested(String name, Configurer configurer) {
        String yaml = """
            my-section:
              my-value: not_a_number
            """;

        assertThatThrownBy(() -> this.loadConfig(HyphenCaseConfig.class, configurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("my-section.my-value");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'my-section.my-value' to Integer from String
                     --> 2:13
                      |
                    2 |   my-value: not_a_number
                      |             ^^^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    // ==================== Sibling Subconfig Accumulation Test ====================

    /**
     * Tests that error paths don't accumulate across sibling subconfigs.
     * This was a bug where processing subconfig A would pollute the parent's basePath,
     * causing subconfig B's error to show path "A.B.field" instead of just "B.field".
     */
    @ParameterizedTest(name = "{0}: Sibling subconfigs don't accumulate paths")
    @MethodSource("yamlConfigurers")
    void testError_SiblingSubconfigsDoNotAccumulatePaths(String name, Configurer configurer) {
        // Config with 3 sibling subconfigs, error in the last one
        String yaml = """
            sectionA:
              valueA: 100
            sectionB:
              valueB: 200
            sectionC:
              valueC: not_a_number
            """;

        assertThatThrownBy(() -> this.loadConfig(SiblingSubconfigsConfig.class, configurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                // Path should be just "sectionC.valueC", NOT "sectionA.sectionB.sectionC.valueC"
                assertThat(e.getPath().toString()).isEqualTo("sectionC.valueC");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'sectionC.valueC' to Integer from String
                     --> 6:11
                      |
                    6 |   valueC: not_a_number
                      |           ^^^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

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

    // ==================== Range Test Types and Transformers ====================

    // Marker type for single-line range test
    public static class RangeTestType {
    }

    // Marker type for multi-line range test
    public static class MultiLineRangeTestType {
    }

    // Marker type for middle line range test
    public static class MiddleLineRangeTestType {
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class RangeTestConfig extends OkaeriConfig {
        private RangeTestType value;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class MultiLineRangeTestConfig extends OkaeriConfig {
        private MultiLineRangeTestType value;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class MiddleLineRangeTestConfig extends OkaeriConfig {
        private MiddleLineRangeTestType value;
    }

    // Marker type for single char in multiline test
    public static class SingleCharMultilineType {}

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class SingleCharMultilineConfig extends OkaeriConfig {
        private SingleCharMultilineType value;
    }

    // Transformer that throws ValueIndexedException with index=6, length=5 ("world")
    public static class RangeTestTransformer extends ObjectTransformer<String, RangeTestType> {
        @Override
        public GenericsPair<String, RangeTestType> getPair() {
            return this.genericsPair(String.class, RangeTestType.class);
        }

        @Override
        public RangeTestType transform(String data, SerdesContext serdesContext) {
            throw new ValueIndexedException("Invalid word at position 6", 6, 5);
        }
    }

    // Transformer that throws ValueIndexedException with index=6, length=13 spanning lines
    public static class MultiLineRangeTestTransformer extends ObjectTransformer<String, MultiLineRangeTestType> {
        @Override
        public GenericsPair<String, MultiLineRangeTestType> getPair() {
            return this.genericsPair(String.class, MultiLineRangeTestType.class);
        }

        @Override
        public MultiLineRangeTestType transform(String data, SerdesContext serdesContext) {
            // "hello world\nfoo bar" - index 6 is 'w', length 13 covers "world\nfoo bar"
            throw new ValueIndexedException("Invalid range spanning lines", 6, 13);
        }
    }

    // Transformer that throws ValueIndexedException with index=12, length=5 ("world" on middle line)
    public static class MiddleLineRangeTestTransformer extends ObjectTransformer<String, MiddleLineRangeTestType> {
        @Override
        public GenericsPair<String, MiddleLineRangeTestType> getPair() {
            return this.genericsPair(String.class, MiddleLineRangeTestType.class);
        }

        @Override
        public MiddleLineRangeTestType transform(String data, SerdesContext serdesContext) {
            // "first\nhello world\nlast" - index 12 is 'w' in "world"
            throw new ValueIndexedException("Invalid word in middle", 12, 5);
        }
    }

    // Transformer that throws ValueIndexedException at index 13 (single char in middle of multiline)
    // For content "^start\nmiddle[broken\nend$" - index 13 is the '[' character
    public static class SingleCharMultilineTransformer extends ObjectTransformer<String, SingleCharMultilineType> {
        @Override
        public GenericsPair<String, SingleCharMultilineType> getPair() {
            return this.genericsPair(String.class, SingleCharMultilineType.class);
        }

        @Override
        public SingleCharMultilineType transform(String data, SerdesContext serdesContext) {
            // "^start\nmiddle[broken\nend$" - index 13 is '[' on line 2 of content
            throw new ValueIndexedException("Invalid character at position 13", 13, 1);
        }
    }

    // ==================== Context Lines Test Types ====================

    public static class ContextBefore3Type {}
    public static class ContextAfter3Type {}
    public static class ContextBoth3Type {}
    public static class ContextBefore1After1Type {}

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class ContextBefore3Config extends OkaeriConfig {
        private ContextBefore3Type value;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class ContextAfter3Config extends OkaeriConfig {
        private ContextAfter3Type value;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class ContextBoth3Config extends OkaeriConfig {
        private ContextBoth3Type value;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class ContextBefore1After1Config extends OkaeriConfig {
        private ContextBefore1After1Type value;
    }

    // Transformer with contextLinesBefore=3 - error at index 30 in long content
    public static class ContextBefore3Transformer extends ObjectTransformer<String, ContextBefore3Type> {
        @Override
        public GenericsPair<String, ContextBefore3Type> getPair() {
            return this.genericsPair(String.class, ContextBefore3Type.class);
        }

        @Override
        public ContextBefore3Type transform(String data, SerdesContext serdesContext) {
            throw ValueIndexedException.builder()
                .message("Error with 3 lines before")
                .startIndex(30)
                .length(5)
                .contextLinesBefore(3)
                .contextLinesAfter(0)
                .build();
        }
    }

    // Transformer with contextLinesAfter=3 - error at index 6 in long content
    public static class ContextAfter3Transformer extends ObjectTransformer<String, ContextAfter3Type> {
        @Override
        public GenericsPair<String, ContextAfter3Type> getPair() {
            return this.genericsPair(String.class, ContextAfter3Type.class);
        }

        @Override
        public ContextAfter3Type transform(String data, SerdesContext serdesContext) {
            throw ValueIndexedException.builder()
                .message("Error with 3 lines after")
                .startIndex(6)
                .length(5)
                .contextLinesBefore(0)
                .contextLinesAfter(3)
                .build();
        }
    }

    // Transformer with contextLinesBefore=3 and contextLinesAfter=3
    public static class ContextBoth3Transformer extends ObjectTransformer<String, ContextBoth3Type> {
        @Override
        public GenericsPair<String, ContextBoth3Type> getPair() {
            return this.genericsPair(String.class, ContextBoth3Type.class);
        }

        @Override
        public ContextBoth3Type transform(String data, SerdesContext serdesContext) {
            throw ValueIndexedException.builder()
                .message("Error with 3 lines before and after")
                .startIndex(24)
                .length(5)
                .contextLinesBefore(3)
                .contextLinesAfter(3)
                .build();
        }
    }

    // Transformer with contextLinesBefore=1 and contextLinesAfter=1 (forces ellipsis even for short content)
    public static class ContextBefore1After1Transformer extends ObjectTransformer<String, ContextBefore1After1Type> {
        @Override
        public GenericsPair<String, ContextBefore1After1Type> getPair() {
            return this.genericsPair(String.class, ContextBefore1After1Type.class);
        }

        @Override
        public ContextBefore1After1Type transform(String data, SerdesContext serdesContext) {
            throw ValueIndexedException.builder()
                .message("Error with custom context")
                .startIndex(6)
                .length(5)
                .contextLinesBefore(1)
                .contextLinesAfter(1)
                .build();
        }
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
