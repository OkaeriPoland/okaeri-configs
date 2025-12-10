package eu.okaeri.configs.lifecycle;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.exception.OkaeriException;
import eu.okaeri.configs.exception.ValidationException;
import eu.okaeri.configs.validator.ConfigValidator;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for OkaeriConfig validation operations.
 * <p>
 * Scenarios tested:
 * - Validation on load (deserialization) - validator rejects bad data
 * - Validation on save (serialization) - validator rejects mutated fields
 * - Manual validation - validate() method called explicitly
 * - Validation control flags (validateOnLoad, validateOnSave)
 * - No validator registered (no-op behavior)
 */
class ConfigValidationTest {

    /**
     * Simple validator that rejects negative integers for "value" field.
     */
    static class SimpleValidator implements ConfigValidator {
        @Override
        public boolean isValid(@NonNull Object entity) {
            if (entity instanceof SimpleTestConfig config) {
                if (config.getValue() < 0) {
                    throw new ValidationException("value must not be negative");
                }
            }
            return true;
        }
    }

    /**
     * Validator that only validates on load, not on save.
     */
    static class LoadOnlyValidator implements ConfigValidator {
        @Override
        public boolean isValid(@NonNull Object entity) {
            if (entity instanceof SimpleTestConfig config) {
                if (config.getValue() < 0) {
                    throw new ValidationException("value must not be negative (load)");
                }
            }
            return true;
        }

        @Override
        public boolean validateOnLoad() {
            return true;
        }

        @Override
        public boolean validateOnSave() {
            return false;
        }
    }

    /**
     * Validator that only validates on save, not on load.
     */
    static class SaveOnlyValidator implements ConfigValidator {
        @Override
        public boolean isValid(@NonNull Object entity) {
            if (entity instanceof SimpleTestConfig config) {
                if (config.getValue() < 0) {
                    throw new ValidationException("value must not be negative (save)");
                }
            }
            return true;
        }

        @Override
        public boolean validateOnLoad() {
            return false;
        }

        @Override
        public boolean validateOnSave() {
            return true;
        }
    }

    @Test
    void testValidation_OnLoad_RejectsBadData() {
        // Arrange
        String yamlWithNegative = """
            value: -50
            name: test
            """;

        SimpleTestConfig config = ConfigManager.create(SimpleTestConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new YamlSnakeYamlConfigurer());
                opt.validator(new SimpleValidator());
            });
        });

        // Act & Assert
        assertThatThrownBy(() -> config.load(yamlWithNegative))
            .isInstanceOf(OkaeriException.class)
            .hasMessageContaining("value must not be negative");
    }

    @Test
    void testValidation_OnLoad_AcceptsValidData() {
        // Arrange
        String yamlWithPositive = """
            value: 100
            name: valid
            """;

        SimpleTestConfig config = ConfigManager.create(SimpleTestConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new YamlSnakeYamlConfigurer());
                opt.validator(new SimpleValidator());
            });
        });

        // Act & Assert
        assertThatCode(() -> config.load(yamlWithPositive))
            .doesNotThrowAnyException();

        assertThat(config.getValue()).isEqualTo(100);
        assertThat(config.getName()).isEqualTo("valid");
    }

    @Test
    void testValidation_OnSave_RejectsMutatedField() {
        // Arrange
        SimpleTestConfig config = ConfigManager.create(SimpleTestConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new YamlSnakeYamlConfigurer());
                opt.validator(new SimpleValidator());
            });
        });

        // Load valid data
        config.load("""
            value: 50
            name: test
            """);

        // Mutate to invalid value
        config.setValue(-999);

        // Act & Assert
        assertThatThrownBy(() -> config.saveToString())
            .isInstanceOf(OkaeriException.class)
            .hasMessageContaining("value must not be negative");
    }

    @Test
    void testValidation_OnSave_AcceptsValidMutation() {
        // Arrange
        SimpleTestConfig config = ConfigManager.create(SimpleTestConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new YamlSnakeYamlConfigurer());
                opt.validator(new SimpleValidator());
            });
        });

        config.load("""
            value: 50
            name: test
            """);

        // Mutate to valid value
        config.setValue(200);
        config.setName("updated");

        // Act & Assert
        assertThatCode(() -> config.saveToString())
            .doesNotThrowAnyException();

        String saved = config.saveToString();
        assertThat(saved).contains("value: 200");
        assertThat(saved).contains("name: updated");
    }

    @Test
    void testValidation_ManualValidate_RejectsInvalidValue() {
        // Arrange
        SimpleTestConfig config = ConfigManager.create(SimpleTestConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new YamlSnakeYamlConfigurer());
                opt.validator(new SimpleValidator());
            });
        });

        // Load valid data
        config.load("""
            value: 50
            name: test
            """);

        // Mutate to invalid value (but don't save)
        config.setValue(-10);

        // Act & Assert - manually validate
        assertThatThrownBy(() -> config.validate())
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("value must not be negative");
    }

    @Test
    void testValidation_ManualValidate_AcceptsValidValue() {
        // Arrange
        SimpleTestConfig config = ConfigManager.create(SimpleTestConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new YamlSnakeYamlConfigurer());
                opt.validator(new SimpleValidator());
            });
        });

        config.load("""
            value: 50
            name: test
            """);

        config.setValue(100);

        // Act & Assert
        assertThatCode(() -> config.validate())
            .doesNotThrowAnyException();
    }

    @Test
    void testValidation_LoadOnlyValidator_RejectsOnLoad() {
        // Arrange
        String yamlWithNegative = """
            value: -50
            name: test
            """;

        SimpleTestConfig config = ConfigManager.create(SimpleTestConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new YamlSnakeYamlConfigurer());
                opt.validator(new LoadOnlyValidator());
            });
        });

        // Act & Assert
        assertThatThrownBy(() -> config.load(yamlWithNegative))
            .isInstanceOf(OkaeriException.class)
            .hasMessageContaining("value must not be negative (load)");
    }

    @Test
    void testValidation_LoadOnlyValidator_AllowsSaveWithInvalidValue() {
        // Arrange
        SimpleTestConfig config = ConfigManager.create(SimpleTestConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new YamlSnakeYamlConfigurer());
                opt.validator(new LoadOnlyValidator());
            });
        });

        config.load("""
            value: 50
            name: test
            """);

        // Mutate to invalid value
        config.setValue(-999);

        // Act & Assert - save should succeed (validation disabled on save)
        assertThatCode(() -> config.saveToString())
            .doesNotThrowAnyException();

        String saved = config.saveToString();
        assertThat(saved).contains("value: -999");
    }

    @Test
    void testValidation_SaveOnlyValidator_AllowsLoadWithInvalidValue() {
        // Arrange
        String yamlWithNegative = """
            value: -50
            name: test
            """;

        SimpleTestConfig config = ConfigManager.create(SimpleTestConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new YamlSnakeYamlConfigurer());
                opt.validator(new SaveOnlyValidator());
            });
        });

        // Act & Assert - load should succeed (validation disabled on load)
        assertThatCode(() -> config.load(yamlWithNegative))
            .doesNotThrowAnyException();

        assertThat(config.getValue()).isEqualTo(-50);
    }

    @Test
    void testValidation_SaveOnlyValidator_RejectsOnSave() {
        // Arrange
        SimpleTestConfig config = ConfigManager.create(SimpleTestConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new YamlSnakeYamlConfigurer());
                opt.validator(new SaveOnlyValidator());
            });
        });

        // Load (validation disabled)
        config.load("""
            value: -50
            name: test
            """);

        // Act & Assert - save should fail (validation enabled on save)
        assertThatThrownBy(() -> config.saveToString())
            .isInstanceOf(OkaeriException.class)
            .hasMessageContaining("value must not be negative (save)");
    }

    @Test
    void testValidation_NoValidator_AllowsAnyValue() {
        // Arrange
        SimpleTestConfig config = ConfigManager.create(SimpleTestConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new YamlSnakeYamlConfigurer());
                // No validator registered
            });
        });

        // Act & Assert - load with negative value succeeds
        assertThatCode(() -> config.load("""
            value: -999
            name: test
            """))
            .doesNotThrowAnyException();

        assertThat(config.getValue()).isEqualTo(-999);

        // Mutate and save also succeeds
        config.setValue(-1000);
        assertThatCode(() -> config.saveToString())
            .doesNotThrowAnyException();

        // Manual validate is no-op
        assertThatCode(() -> config.validate())
            .doesNotThrowAnyException();
    }

    @Test
    void testValidation_ManualValidate_AlwaysValidates() {
        // Arrange - LoadOnlyValidator that normally skips save validation
        SimpleTestConfig config = ConfigManager.create(SimpleTestConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new YamlSnakeYamlConfigurer());
                opt.validator(new LoadOnlyValidator());
            });
        });

        config.load("""
            value: 50
            name: test
            """);

        config.setValue(-10);

        // Act & Assert - validate() always validates regardless of flags
        assertThatThrownBy(() -> config.validate())
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("value must not be negative");
    }

    // ==================== Test Config Classes ====================

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class SimpleTestConfig extends OkaeriConfig {
        private int value = 0;
        private String name = "default";
    }
}
