package eu.okaeri.configs.error;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.exception.OkaeriConfigException;
import eu.okaeri.configs.properties.IniConfigurer;
import eu.okaeri.configs.properties.PropertiesConfigurer;
import eu.okaeri.configs.toml.TomlJacksonConfigurer;
import eu.okaeri.configs.yaml.bukkit.YamlBukkitConfigurer;
import eu.okaeri.configs.yaml.bungee.YamlBungeeConfigurer;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests error messages for placeholder resolution failures.
 * Parametrized across all format backends to ensure consistent error reporting.
 */
class PlaceholderErrorMessageTest {

    @AfterEach
    void cleanup() {
        System.clearProperty("TEST_HOST");
        System.clearProperty("TEST_PORT");
    }

    static Stream<Arguments> yamlConfigurers() {
        return Stream.of(
            Arguments.of("SnakeYAML", new YamlSnakeYamlConfigurer()),
            Arguments.of("Bukkit", new YamlBukkitConfigurer()),
            Arguments.of("Bungee", new YamlBungeeConfigurer())
        );
    }

    static Stream<Arguments> tomlConfigurers() {
        return Stream.of(
            Arguments.of("TomlJackson", new TomlJacksonConfigurer())
        );
    }

    static Stream<Arguments> iniConfigurers() {
        return Stream.of(
            Arguments.of("Properties", new PropertiesConfigurer()),
            Arguments.of("INI", new IniConfigurer())
        );
    }

    // ==================== YAML Placeholder Errors ====================

    @ParameterizedTest(name = "{0}: Missing placeholder")
    @MethodSource("yamlConfigurers")
    void testError_MissingPlaceholder_Yaml(String name, Configurer configurer) {
        String yaml = """
            host: localhost
            password: ${MISSING_VAR}
            """;

        assertThatThrownBy(() -> this.loadConfigWithPlaceholders(PasswordConfig.class, configurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("password");
                assertThat(e.getMessage()).isEqualTo("""
                    error[EnvironmentPlaceholderProcessor]: Cannot pre-process 'password' to String from String
                     --> 2:11
                      |
                    2 | password: ${MISSING_VAR}
                      |           ^^^^^^^^^^^^^^ Unresolved property or env""");
            });
    }

    @ParameterizedTest(name = "{0}: Missing placeholder in nested config")
    @MethodSource("yamlConfigurers")
    void testError_MissingPlaceholder_Nested_Yaml(String name, Configurer configurer) {
        String yaml = """
            database:
              host: localhost
              password: ${DB_PASSWORD}
            """;

        assertThatThrownBy(() -> this.loadConfigWithPlaceholders(DatabaseConfig.class, configurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("database.password");
                assertThat(e.getMessage()).isEqualTo("""
                    error[EnvironmentPlaceholderProcessor]: Cannot pre-process 'database.password' to String from String
                     --> 3:13
                      |
                    3 |   password: ${DB_PASSWORD}
                      |             ^^^^^^^^^^^^^^ Unresolved property or env""");
            });
    }

    @ParameterizedTest(name = "{0}: Missing placeholder with surrounding text")
    @MethodSource("yamlConfigurers")
    void testError_MissingPlaceholder_WithSurroundingText_Yaml(String name, Configurer configurer) {
        String yaml = """
            url: http://${MISSING_HOST}:8080/api
            """;

        assertThatThrownBy(() -> this.loadConfigWithPlaceholders(UrlConfig.class, configurer, yaml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("url");
                assertThat(e.getMessage()).isEqualTo("""
                    error[EnvironmentPlaceholderProcessor]: Cannot pre-process 'url' to String from String
                     --> 1:13
                      |
                    1 | url: http://${MISSING_HOST}:8080/api
                      |             ^^^^^^^^^^^^^^^ Unresolved property or env""");
            });
    }

    // ==================== TOML Placeholder Errors ====================

    @ParameterizedTest(name = "{0}: Missing placeholder")
    @MethodSource("tomlConfigurers")
    void testError_MissingPlaceholder_Toml(String name, Configurer configurer) {
        String toml = """
            host = 'localhost'
            password = '${MISSING_VAR}'
            """;

        assertThatThrownBy(() -> this.loadConfigWithPlaceholders(PasswordConfig.class, configurer, toml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("password");
                assertThat(e.getMessage()).isEqualTo("""
                    error[EnvironmentPlaceholderProcessor]: Cannot pre-process 'password' to String from String
                     --> 2:13
                      |
                    2 | password = '${MISSING_VAR}'
                      |             ^^^^^^^^^^^^^^ Unresolved property or env""");
            });
    }

    @ParameterizedTest(name = "{0}: Missing placeholder in nested config")
    @MethodSource("tomlConfigurers")
    void testError_MissingPlaceholder_Nested_Toml(String name, Configurer configurer) {
        String toml = """
            [database]
            host = 'localhost'
            password = '${DB_PASSWORD}'
            """;

        assertThatThrownBy(() -> this.loadConfigWithPlaceholders(DatabaseConfig.class, configurer, toml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("database.password");
                assertThat(e.getMessage()).isEqualTo("""
                    error[EnvironmentPlaceholderProcessor]: Cannot pre-process 'database.password' to String from String
                     --> 3:13
                      |
                    3 | password = '${DB_PASSWORD}'
                      |             ^^^^^^^^^^^^^^ Unresolved property or env""");
            });
    }

    // ==================== INI/Properties Placeholder Errors ====================

    @ParameterizedTest(name = "{0}: Missing placeholder")
    @MethodSource("iniConfigurers")
    void testError_MissingPlaceholder_Ini(String name, Configurer configurer) {
        String ini = """
            host=localhost
            password=${MISSING_VAR}
            """;

        assertThatThrownBy(() -> this.loadConfigWithPlaceholders(PasswordConfig.class, configurer, ini))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("password");
                assertThat(e.getMessage()).isEqualTo("""
                    error[EnvironmentPlaceholderProcessor]: Cannot pre-process 'password' to String from String
                     --> 2:10
                      |
                    2 | password=${MISSING_VAR}
                      |          ^^^^^^^^^^^^^^ Unresolved property or env""");
            });
    }

    // ==================== Helper Methods ====================

    private <T extends OkaeriConfig> T loadConfigWithPlaceholders(Class<T> clazz, Configurer configurer, String content) {
        T config = ConfigManager.create(clazz, it -> {
            it.configure(opt -> {
                opt.configurer(configurer);
                opt.resolvePlaceholders();
            });
        });
        config.load(content);
        return config;
    }

    // ==================== Test Configs ====================

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class PasswordConfig extends OkaeriConfig {
        private String host = "default";
        private String password = "default";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class UrlConfig extends OkaeriConfig {
        private String url = "default";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class DatabaseConfig extends OkaeriConfig {
        private DbSettings database = new DbSettings();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class DbSettings extends OkaeriConfig {
        private String host = "localhost";
        private String password = "default";
    }
}
