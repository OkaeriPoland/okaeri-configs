package eu.okaeri.configs.serdes;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.configurer.InMemoryConfigurer;
import eu.okaeri.configs.exception.OkaeriException;
import eu.okaeri.configs.serdes.standard.EnvironmentPlaceholderProcessor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for ValuePreProcessor and EnvironmentPlaceholderProcessor.
 * <p>
 * Verifies:
 * - Basic placeholder resolution (${VAR})
 * - Default value syntax (${VAR:default})
 * - Escape syntax ($${VAR} → literal ${VAR})
 * - Missing placeholder throws exception
 * - Save preserves original placeholder string
 * - Type conversion through placeholder
 * - Nested subconfig placeholder resolution
 * - System property takes precedence over env var
 */
class ValuePreProcessorTest {

    @AfterEach
    void cleanup() {
        System.clearProperty("TEST_PLACEHOLDER");
        System.clearProperty("TEST_HOST");
        System.clearProperty("TEST_PORT");
        System.clearProperty("NESTED_PLACEHOLDER");
        System.clearProperty("TEST_VALUE");
        System.clearProperty("PATH");
    }

    // Test configs

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class SimplePlaceholderConfig extends OkaeriConfig {
        private String host = "default-host";
        private String password = "default-password";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class TypeConversionConfig extends OkaeriConfig {
        private int port = 8080;
        private String host = "localhost";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class EscapeSyntaxConfig extends OkaeriConfig {
        private String literal = "default";
        private String mixed = "default";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class NestedSubconfig extends OkaeriConfig {
        private String nestedHost = "default-nested";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class ConfigWithNested extends OkaeriConfig {
        private String topLevel = "default-top";
        private NestedSubconfig nested = new NestedSubconfig();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class SingleFieldConfig extends OkaeriConfig {
        private String value = "default";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class UrlConfig extends OkaeriConfig {
        private String url = "default";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class PathConfig extends OkaeriConfig {
        private String path = "default";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class PasswordConfig extends OkaeriConfig {
        private String password = "default";
    }

    // Tests

    @Test
    void testPlaceholder_ResolvesFromSystemProperty() {
        // Given
        System.setProperty("TEST_PLACEHOLDER", "secret-password");
        SimplePlaceholderConfig config = ConfigManager.create(SimplePlaceholderConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new InMemoryConfigurer());
                opt.resolvePlaceholders();
            });
        });

        // When
        Map<String, Object> data = new HashMap<>();
        data.put("host", "${TEST_HOST:localhost}");
        data.put("password", "${TEST_PLACEHOLDER}");
        config.load(data);

        // Then
        assertThat(config.getPassword()).isEqualTo("secret-password");
    }

    @Test
    void testPlaceholder_DefaultValue_UsedWhenVarNotSet() {
        // Given - TEST_HOST not set
        System.setProperty("TEST_PLACEHOLDER", "secret");
        SimplePlaceholderConfig config = ConfigManager.create(SimplePlaceholderConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new InMemoryConfigurer());
                opt.resolvePlaceholders();
            });
        });

        // When
        Map<String, Object> data = new HashMap<>();
        data.put("host", "${TEST_HOST:localhost}");
        data.put("password", "${TEST_PLACEHOLDER}");
        config.load(data);

        // Then
        assertThat(config.getHost()).isEqualTo("localhost");
    }

    @Test
    void testPlaceholder_DefaultValue_OverriddenBySystemProperty() {
        // Given
        System.setProperty("TEST_HOST", "production-server");
        System.setProperty("TEST_PLACEHOLDER", "secret");
        SimplePlaceholderConfig config = ConfigManager.create(SimplePlaceholderConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new InMemoryConfigurer());
                opt.resolvePlaceholders();
            });
        });

        // When
        Map<String, Object> data = new HashMap<>();
        data.put("host", "${TEST_HOST:localhost}");
        data.put("password", "${TEST_PLACEHOLDER}");
        config.load(data);

        // Then
        assertThat(config.getHost()).isEqualTo("production-server");
    }

    @Test
    void testPlaceholder_MissingWithoutDefault_ThrowsException() {
        // Given - MISSING_VAR not set, no default
        SimplePlaceholderConfig config = ConfigManager.create(SimplePlaceholderConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new InMemoryConfigurer());
                opt.resolvePlaceholders();
            });
        });

        // When/Then
        Map<String, Object> data = new HashMap<>();
        data.put("host", "localhost");
        data.put("password", "${MISSING_VAR}");

        assertThatThrownBy(() -> config.load(data))
            .isInstanceOf(OkaeriException.class)
            .hasMessageContaining("Unresolved property or env")
            .hasMessageContaining("MISSING_VAR");
    }

    @Test
    void testPlaceholder_EscapeSyntax_ReturnsLiteral() {
        // Given
        EscapeSyntaxConfig config = ConfigManager.create(EscapeSyntaxConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new InMemoryConfigurer());
                opt.resolvePlaceholders();
            });
        });

        // When
        Map<String, Object> data = new HashMap<>();
        data.put("literal", "$${NOT_RESOLVED}");
        data.put("mixed", "${TEST_HOST:localhost}:$${PORT}");
        config.load(data);

        // Then - $${...} becomes literal ${...}
        assertThat(config.getLiteral()).isEqualTo("${NOT_RESOLVED}");
    }

    @Test
    void testPlaceholder_MixedEscapeAndResolve() {
        // Given
        System.setProperty("TEST_HOST", "myserver");
        EscapeSyntaxConfig config = ConfigManager.create(EscapeSyntaxConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new InMemoryConfigurer());
                opt.resolvePlaceholders();
            });
        });

        // When
        Map<String, Object> data = new HashMap<>();
        data.put("literal", "$${NOT_RESOLVED}");
        data.put("mixed", "${TEST_HOST:localhost}:$${PORT}");
        config.load(data);

        // Then - ${TEST_HOST} resolved, $${PORT} becomes literal
        assertThat(config.getMixed()).isEqualTo("myserver:${PORT}");
    }

    @Test
    void testPlaceholder_SavePreservesOriginal() {
        // Given
        System.setProperty("TEST_HOST", "resolved-host");
        System.setProperty("TEST_PLACEHOLDER", "resolved-password");
        SimplePlaceholderConfig config = ConfigManager.create(SimplePlaceholderConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new InMemoryConfigurer());
                opt.resolvePlaceholders();
            });
        });

        // When - Load resolves placeholders
        Map<String, Object> data = new HashMap<>();
        data.put("host", "${TEST_HOST:localhost}");
        data.put("password", "${TEST_PLACEHOLDER}");
        config.load(data);

        // Then - Field has resolved value
        assertThat(config.getHost()).isEqualTo("resolved-host");

        // When - Save should preserve original placeholder
        Map<String, Object> saved = config.asMap();

        // Then - Original placeholders are preserved
        assertThat(saved.get("host")).isEqualTo("${TEST_HOST:localhost}");
        assertThat(saved.get("password")).isEqualTo("${TEST_PLACEHOLDER}");
    }

    @Test
    void testPlaceholder_TypeConversion_StringToInt() {
        // Given
        System.setProperty("TEST_PORT", "9999");
        TypeConversionConfig config = ConfigManager.create(TypeConversionConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new InMemoryConfigurer());
                opt.resolvePlaceholders();
            });
        });

        // When
        Map<String, Object> data = new HashMap<>();
        data.put("port", "${TEST_PORT:8080}");
        data.put("host", "${TEST_HOST:localhost}");
        config.load(data);

        // Then - Placeholder resolved and converted to int
        assertThat(config.getPort()).isEqualTo(9999);
        assertThat(config.getHost()).isEqualTo("localhost");
    }

    @Test
    void testPlaceholder_TypeConversion_DefaultToInt() {
        // Given - TEST_PORT not set, use default
        TypeConversionConfig config = ConfigManager.create(TypeConversionConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new InMemoryConfigurer());
                opt.resolvePlaceholders();
            });
        });

        // When
        Map<String, Object> data = new HashMap<>();
        data.put("port", "${TEST_PORT:3000}");
        data.put("host", "localhost");
        config.load(data);

        // Then - Default value converted to int
        assertThat(config.getPort()).isEqualTo(3000);
    }

    @Test
    void testPlaceholder_NestedSubconfig_Resolved() {
        // Given
        System.setProperty("TEST_HOST", "top-resolved");
        System.setProperty("NESTED_PLACEHOLDER", "nested-resolved");
        ConfigWithNested config = ConfigManager.create(ConfigWithNested.class, it -> {
            it.configure(opt -> {
                opt.configurer(new InMemoryConfigurer());
                opt.resolvePlaceholders();
            });
        });

        // When
        Map<String, Object> nestedData = new HashMap<>();
        nestedData.put("nestedHost", "${NESTED_PLACEHOLDER:nested-default}");

        Map<String, Object> data = new HashMap<>();
        data.put("topLevel", "${TEST_HOST:top-default}");
        data.put("nested", nestedData);
        config.load(data);

        // Then - Both top-level and nested placeholders resolved
        assertThat(config.getTopLevel()).isEqualTo("top-resolved");
        assertThat(config.getNested().getNestedHost()).isEqualTo("nested-resolved");
    }

    @Test
    void testPlaceholder_NestedSubconfig_DefaultValues() {
        // Given - No system properties set
        ConfigWithNested config = ConfigManager.create(ConfigWithNested.class, it -> {
            it.configure(opt -> {
                opt.configurer(new InMemoryConfigurer());
                opt.resolvePlaceholders();
            });
        });

        // When
        Map<String, Object> nestedData = new HashMap<>();
        nestedData.put("nestedHost", "${NESTED_PLACEHOLDER:nested-default}");

        Map<String, Object> data = new HashMap<>();
        data.put("topLevel", "${TEST_HOST:top-default}");
        data.put("nested", nestedData);
        config.load(data);

        // Then - Default values used
        assertThat(config.getTopLevel()).isEqualTo("top-default");
        assertThat(config.getNested().getNestedHost()).isEqualTo("nested-default");
    }

    @Test
    void testPlaceholder_SystemPropertyPrecedence_OverEnvVar() {
        // Given - PATH exists as env var, override with system property
        System.setProperty("PATH", "system-path-wins");

        PathConfig config = ConfigManager.create(PathConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new InMemoryConfigurer());
                opt.resolvePlaceholders();
            });
        });

        // When
        Map<String, Object> data = new HashMap<>();
        data.put("path", "${PATH}");
        config.load(data);

        // Then - System property takes precedence
        assertThat(config.getPath()).isEqualTo("system-path-wins");
    }

    @Test
    void testPlaceholder_MultiplePlaceholdersInSingleValue() {
        // Given
        System.setProperty("TEST_HOST", "myhost");
        System.setProperty("TEST_PORT", "8080");

        UrlConfig config = ConfigManager.create(UrlConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new InMemoryConfigurer());
                opt.resolvePlaceholders();
            });
        });

        // When
        Map<String, Object> data = new HashMap<>();
        data.put("url", "http://${TEST_HOST}:${TEST_PORT}/api");
        config.load(data);

        // Then - Both placeholders resolved
        assertThat(config.getUrl()).isEqualTo("http://myhost:8080/api");
    }

    @Test
    void testPlaceholder_NoPlaceholders_PassesThrough() {
        // Given
        SingleFieldConfig config = ConfigManager.create(SingleFieldConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new InMemoryConfigurer());
                opt.resolvePlaceholders();
            });
        });

        // When
        Map<String, Object> data = new HashMap<>();
        data.put("value", "plain text without placeholders");
        config.load(data);

        // Then - Value unchanged
        assertThat(config.getValue()).isEqualTo("plain text without placeholders");
    }

    @Test
    void testPlaceholder_EmptyDefault_AllowedAndUsed() {
        // Given - No TEST_VAR set
        SingleFieldConfig config = ConfigManager.create(SingleFieldConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new InMemoryConfigurer());
                opt.resolvePlaceholders();
            });
        });

        // When
        Map<String, Object> data = new HashMap<>();
        data.put("value", "${TEST_VAR:}");
        config.load(data);

        // Then - Empty default used
        assertThat(config.getValue()).isEmpty();
    }

    @Test
    void testPlaceholder_CustomPreProcessor() {
        // Given - Custom pre-processor that uppercases values
        ValuePreProcessor uppercaseProcessor = (value, context) -> {
            if (value instanceof String) {
                return PreProcessResult.transformed(((String) value).toUpperCase());
            }
            return PreProcessResult.noop();
        };

        SingleFieldConfig config = ConfigManager.create(SingleFieldConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new InMemoryConfigurer());
                opt.valuePreProcessor(uppercaseProcessor);
            });
        });

        // When
        Map<String, Object> data = new HashMap<>();
        data.put("value", "hello world");
        config.load(data);

        // Then - Custom processor applied
        assertThat(config.getValue()).isEqualTo("HELLO WORLD");
    }

    @Test
    void testPlaceholder_ChainedProcessors() {
        // Given - Chain of processors: resolve env vars then uppercase
        System.setProperty("TEST_VALUE", "hello");

        ValuePreProcessor uppercaseProcessor = (value, context) -> {
            if (value instanceof String) {
                return PreProcessResult.runtimeOnly(((String) value).toUpperCase());
            }
            return PreProcessResult.noop();
        };

        SingleFieldConfig config = ConfigManager.create(SingleFieldConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new InMemoryConfigurer());
                opt.valuePreProcessor(
                    new EnvironmentPlaceholderProcessor(),
                    uppercaseProcessor
                );
            });
        });

        // When
        Map<String, Object> data = new HashMap<>();
        data.put("value", "${TEST_VALUE}");
        config.load(data);

        // Then - Both processors applied: ${TEST_VALUE} -> "hello" -> "HELLO"
        assertThat(config.getValue()).isEqualTo("HELLO");

        // And - Original placeholder preserved for saving (runtimeOnly)
        Map<String, Object> saved = config.asMap();
        assertThat(saved.get("value")).isEqualTo("${TEST_VALUE}");
    }

    @Test
    void testPlaceholder_TransformedWritesToFile() {
        // Given - Processor that transforms and writes to file
        ValuePreProcessor transformProcessor = (value, context) -> {
            if ((value instanceof String) && ((String) value).startsWith("secret:")) {
                // Simulate decryption that should be persisted
                String decrypted = ((String) value).substring(7);
                return PreProcessResult.transformed(decrypted);
            }
            return PreProcessResult.noop();
        };

        PasswordConfig config = ConfigManager.create(PasswordConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new InMemoryConfigurer());
                opt.valuePreProcessor(transformProcessor);
            });
        });

        // When
        Map<String, Object> data = new HashMap<>();
        data.put("password", "secret:mypassword");
        config.load(data);

        // Then - Transformed value used
        assertThat(config.getPassword()).isEqualTo("mypassword");

        // And - Transformed value written to file (not original)
        Map<String, Object> saved = config.asMap();
        assertThat(saved.get("password")).isEqualTo("mypassword");
    }
}
