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

        assertThatThrownBy(() -> loadConfig(IntegerConfig.class, configurer, yaml))
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

        assertThatThrownBy(() -> loadConfig(LongConfig.class, configurer, yaml))
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

        assertThatThrownBy(() -> loadConfig(DoubleConfig.class, configurer, yaml))
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

        assertThatThrownBy(() -> loadConfig(FloatConfig.class, configurer, yaml))
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

        assertThatThrownBy(() -> loadConfig(BooleanConfig.class, configurer, yaml))
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

        assertThatThrownBy(() -> loadConfig(UuidConfig.class, configurer, yaml))
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

        assertThatThrownBy(() -> loadConfig(BigDecimalConfig.class, configurer, yaml))
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

        assertThatThrownBy(() -> loadConfig(BigIntegerConfig.class, configurer, yaml))
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

        assertThatThrownBy(() -> loadConfigWithCommons(DurationConfig.class, configurer, yaml))
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

        assertThatThrownBy(() -> loadConfigWithCommons(InstantConfig.class, configurer, yaml))
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

        assertThatThrownBy(() -> loadConfigWithCommons(LocaleConfig.class, configurer, yaml))
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

        assertThatThrownBy(() -> loadConfigWithCommons(PatternConfig.class, configurer, yaml))
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

        assertThatThrownBy(() -> loadConfig(NestedConfig.class, configurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("database.port");
                assertThat(e.getMessage()).contains("database.port");
                assertThat(e.getMessage()).contains("Integer");
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

        assertThatThrownBy(() -> loadConfig(DeepNestedConfig.class, configurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("level1.level2.level3.value");
                assertThat(e.getMessage()).contains("level1.level2.level3.value");
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

        assertThatThrownBy(() -> loadConfig(ListConfig.class, configurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("numbers[2]");
                assertThat(e.getMessage()).contains("numbers[2]");
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

        assertThatThrownBy(() -> loadConfig(ServerListConfig.class, configurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("servers[1].port");
                assertThat(e.getMessage()).contains("servers[1].port");
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

        assertThatThrownBy(() -> loadConfig(MapConfig.class, configurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("limits[\"weekly\"]");
                assertThat(e.getMessage()).contains("limits");
                assertThat(e.getMessage()).contains("weekly");
            });
    }

    // ==================== Enum Errors ====================

    @ParameterizedTest(name = "{0}: Invalid enum value")
    @MethodSource("yamlConfigurers")
    void testError_InvalidEnum(String name, Configurer configurer) {
        String yaml = "level: INVALID_LEVEL";

        assertThatThrownBy(() -> loadConfig(EnumConfig.class, configurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("level");
                assertThat(e.getMessage()).contains("level");
                assertThat(e.getMessage()).contains("INVALID_LEVEL");
                // Should suggest valid values
                assertThat(e.getMessage()).contains("LOW");
                assertThat(e.getMessage()).contains("MEDIUM");
                assertThat(e.getMessage()).contains("HIGH");
            });
    }

    // ==================== Helper Methods ====================

    private <T extends OkaeriConfig> T loadConfig(Class<T> clazz, Configurer configurer, String yaml) {
        T config = ConfigManager.create(clazz);
        config.withConfigurer(configurer);
        config.load(yaml);
        return config;
    }

    private <T extends OkaeriConfig> T loadConfigWithCommons(Class<T> clazz, Configurer configurer, String yaml) {
        T config = ConfigManager.create(clazz);
        config.withConfigurer(configurer, new SerdesCommons());
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
}
