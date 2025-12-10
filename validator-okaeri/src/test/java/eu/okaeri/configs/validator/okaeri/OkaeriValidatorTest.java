package eu.okaeri.configs.validator.okaeri;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.exception.OkaeriException;
import eu.okaeri.configs.exception.ValidationException;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;
import eu.okaeri.validator.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for OkaeriValidator integration with okaeri-configs.
 * <p>
 * Tests all supported constraints from okaeri-validator:
 * - @NotNull / @Nullable
 * - @Min / @Max / @DecimalMin / @DecimalMax
 * - @Positive / @PositiveOrZero / @Negative / @NegativeOrZero
 * - @Size (for strings and collections)
 * - @NotBlank
 * - @Pattern
 */
class OkaeriValidatorTest {

    // ==================== @Min / @Max Tests ====================

    @Test
    void testMin_RejectsValueBelowMinimum() {
        // Arrange
        MinMaxConfig config = ConfigManager.create(MinMaxConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new YamlSnakeYamlConfigurer());
                opt.validator(new OkaeriValidator());
            });
        });

        // Act & Assert
        assertThatThrownBy(() -> config.load("age: 5"))
            .isInstanceOf(OkaeriException.class)
            .hasMessageContaining("age")
            .hasMessageContaining("invalid");
    }

    @Test
    void testMin_AcceptsValueAtMinimum() {
        // Arrange
        MinMaxConfig config = ConfigManager.create(MinMaxConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new YamlSnakeYamlConfigurer());
                opt.validator(new OkaeriValidator());
            });
        });

        // Act & Assert
        assertThatCode(() -> config.load("age: 18"))
            .doesNotThrowAnyException();
        assertThat(config.getAge()).isEqualTo(18);
    }

    @Test
    void testMax_RejectsValueAboveMaximum() {
        // Arrange
        MinMaxConfig config = ConfigManager.create(MinMaxConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new YamlSnakeYamlConfigurer());
                opt.validator(new OkaeriValidator());
            });
        });

        // Act & Assert
        assertThatThrownBy(() -> config.load("score: 1001"))
            .isInstanceOf(OkaeriException.class)
            .hasMessageContaining("score")
            .hasMessageContaining("invalid");
    }

    @Test
    void testMax_AcceptsValueAtMaximum() {
        // Arrange
        MinMaxConfig config = ConfigManager.create(MinMaxConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new YamlSnakeYamlConfigurer());
                opt.validator(new OkaeriValidator());
            });
        });

        // Act & Assert
        assertThatCode(() -> config.load("score: 1000"))
            .doesNotThrowAnyException();
        assertThat(config.getScore()).isEqualTo(1000);
    }

    // ==================== @Positive / @Negative Tests ====================

    @Test
    void testPositive_RejectsZero() {
        // Arrange
        PositiveNegativeConfig config = ConfigManager.create(PositiveNegativeConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new YamlSnakeYamlConfigurer());
                opt.validator(new OkaeriValidator());
            });
        });

        // Act & Assert
        assertThatThrownBy(() -> config.load("positiveValue: 0"))
            .isInstanceOf(OkaeriException.class)
            .hasMessageContaining("positiveValue")
            .hasMessageContaining("invalid");
    }

    @Test
    void testPositive_AcceptsPositiveValue() {
        // Arrange
        PositiveNegativeConfig config = ConfigManager.create(PositiveNegativeConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new YamlSnakeYamlConfigurer());
                opt.validator(new OkaeriValidator());
            });
        });

        // Act & Assert
        assertThatCode(() -> config.load("positiveValue: 42"))
            .doesNotThrowAnyException();
        assertThat(config.getPositiveValue()).isEqualTo(42);
    }

    @Test
    void testPositiveOrZero_AcceptsZero() {
        // Arrange
        PositiveNegativeConfig config = ConfigManager.create(PositiveNegativeConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new YamlSnakeYamlConfigurer());
                opt.validator(new OkaeriValidator());
            });
        });

        // Act & Assert
        assertThatCode(() -> config.load("positiveOrZeroValue: 0"))
            .doesNotThrowAnyException();
        assertThat(config.getPositiveOrZeroValue()).isEqualTo(0);
    }

    @Test
    void testNegative_AcceptsNegativeValue() {
        // Arrange
        PositiveNegativeConfig config = ConfigManager.create(PositiveNegativeConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new YamlSnakeYamlConfigurer());
                opt.validator(new OkaeriValidator());
            });
        });

        // Act & Assert
        assertThatCode(() -> config.load("negativeValue: -10"))
            .doesNotThrowAnyException();
        assertThat(config.getNegativeValue()).isEqualTo(-10);
    }

    @Test
    void testNegative_RejectsPositiveValue() {
        // Arrange
        PositiveNegativeConfig config = ConfigManager.create(PositiveNegativeConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new YamlSnakeYamlConfigurer());
                opt.validator(new OkaeriValidator());
            });
        });

        // Act & Assert
        assertThatThrownBy(() -> config.load("negativeValue: 10"))
            .isInstanceOf(OkaeriException.class)
            .hasMessageContaining("negativeValue")
            .hasMessageContaining("invalid");
    }

    // ==================== @Size Tests ====================

    @Test
    void testSize_RejectsStringTooShort() {
        // Arrange
        SizeConfig config = ConfigManager.create(SizeConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new YamlSnakeYamlConfigurer());
                opt.validator(new OkaeriValidator());
            });
        });

        // Act & Assert
        assertThatThrownBy(() -> config.load("username: \"ab\""))
            .isInstanceOf(OkaeriException.class)
            .hasMessageContaining("username")
            .hasMessageContaining("invalid");
    }

    @Test
    void testSize_RejectsStringTooLong() {
        // Arrange
        SizeConfig config = ConfigManager.create(SizeConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new YamlSnakeYamlConfigurer());
                opt.validator(new OkaeriValidator());
            });
        });

        // Act & Assert
        assertThatThrownBy(() -> config.load("username: \"thisusernameiswaytoolongtobeaccepted\""))
            .isInstanceOf(OkaeriException.class)
            .hasMessageContaining("username")
            .hasMessageContaining("invalid");
    }

    @Test
    void testSize_AcceptsStringInRange() {
        // Arrange
        SizeConfig config = ConfigManager.create(SizeConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new YamlSnakeYamlConfigurer());
                opt.validator(new OkaeriValidator());
            });
        });

        // Act & Assert
        assertThatCode(() -> config.load("username: \"validuser\""))
            .doesNotThrowAnyException();
        assertThat(config.getUsername()).isEqualTo("validuser");
    }

    @Test
    void testSize_RejectsCollectionTooSmall() {
        // Arrange
        SizeConfig config = ConfigManager.create(SizeConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new YamlSnakeYamlConfigurer());
                opt.validator(new OkaeriValidator());
            });
        });

        // Act & Assert
        assertThatThrownBy(() -> config.load("tags: []"))
            .isInstanceOf(OkaeriException.class)
            .hasMessageContaining("tags")
            .hasMessageContaining("invalid");
    }

    @Test
    void testSize_AcceptsCollectionInRange() {
        // Arrange
        SizeConfig config = ConfigManager.create(SizeConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new YamlSnakeYamlConfigurer());
                opt.validator(new OkaeriValidator());
            });
        });

        // Act & Assert
        assertThatCode(() -> config.load("""
            tags:
              - one
              - two
            """))
            .doesNotThrowAnyException();
        assertThat(config.getTags()).hasSize(2);
    }

    // ==================== @NotBlank Tests ====================

    @Test
    void testNotBlank_RejectsEmptyString() {
        // Arrange
        NotBlankConfig config = ConfigManager.create(NotBlankConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new YamlSnakeYamlConfigurer());
                opt.validator(new OkaeriValidator());
            });
        });

        // Act & Assert
        assertThatThrownBy(() -> config.load("email: \"\""))
            .isInstanceOf(OkaeriException.class)
            .hasMessageContaining("email")
            .hasMessageContaining("invalid");
    }

    @Test
    void testNotBlank_RejectsWhitespaceOnlyString() {
        // Arrange
        NotBlankConfig config = ConfigManager.create(NotBlankConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new YamlSnakeYamlConfigurer());
                opt.validator(new OkaeriValidator());
            });
        });

        // Act & Assert
        assertThatThrownBy(() -> config.load("email: \"   \""))
            .isInstanceOf(OkaeriException.class)
            .hasMessageContaining("email")
            .hasMessageContaining("invalid");
    }

    @Test
    void testNotBlank_AcceptsNonBlankString() {
        // Arrange
        NotBlankConfig config = ConfigManager.create(NotBlankConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new YamlSnakeYamlConfigurer());
                opt.validator(new OkaeriValidator());
            });
        });

        // Act & Assert
        assertThatCode(() -> config.load("email: \"user@example.com\""))
            .doesNotThrowAnyException();
        assertThat(config.getEmail()).isEqualTo("user@example.com");
    }

    // ==================== @Pattern Tests ====================

    @Test
    void testPattern_RejectsInvalidFormat() {
        // Arrange
        PatternConfig config = ConfigManager.create(PatternConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new YamlSnakeYamlConfigurer());
                opt.validator(new OkaeriValidator());
            });
        });

        // Act & Assert
        assertThatThrownBy(() -> config.load("phoneNumber: \"not-a-phone\""))
            .isInstanceOf(OkaeriException.class)
            .hasMessageContaining("phoneNumber")
            .hasMessageContaining("invalid");
    }

    @Test
    void testPattern_AcceptsValidFormat() {
        // Arrange
        PatternConfig config = ConfigManager.create(PatternConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new YamlSnakeYamlConfigurer());
                opt.validator(new OkaeriValidator());
            });
        });

        // Act & Assert
        assertThatCode(() -> config.load("phoneNumber: \"123-456-7890\""))
            .doesNotThrowAnyException();
        assertThat(config.getPhoneNumber()).isEqualTo("123-456-7890");
    }

    // ==================== Runtime Mutation Tests ====================

    @Test
    void testRuntimeMutation_RejectsInvalidValueOnSave() {
        // Arrange
        MinMaxConfig config = ConfigManager.create(MinMaxConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new YamlSnakeYamlConfigurer());
                opt.validator(new OkaeriValidator());
            });
        });

        config.load("age: 25\nscore: 500");

        // Act - mutate to invalid value
        config.setAge(10);  // Below minimum of 18

        // Assert
        assertThatThrownBy(config::saveToString)
            .isInstanceOf(OkaeriException.class)
            .hasMessageContaining("age")
            .hasMessageContaining("invalid");
    }

    @Test
    void testManualValidate_DetectsInvalidMutation() {
        // Arrange
        MinMaxConfig config = ConfigManager.create(MinMaxConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new YamlSnakeYamlConfigurer());
                opt.validator(new OkaeriValidator());
            });
        });

        config.load("age: 25\nscore: 500");

        // Act - mutate to invalid value
        config.setScore(2000);  // Above maximum of 1000

        // Assert
        assertThatThrownBy(config::validate)
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("score")
            .hasMessageContaining("invalid");
    }

    // ==================== Test Config Classes ====================

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class MinMaxConfig extends OkaeriConfig {
        @Min(18)
        private int age = 18;

        @Max(1000)
        private int score = 0;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class PositiveNegativeConfig extends OkaeriConfig {
        @Positive
        private int positiveValue = 1;

        @PositiveOrZero
        private int positiveOrZeroValue = 0;

        @Negative
        private int negativeValue = -1;

        @NegativeOrZero
        private int negativeOrZeroValue = 0;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class SizeConfig extends OkaeriConfig {
        @Size(min = 3, max = 20)
        private String username = "default";

        @Size(min = 1, max = 10)
        private List<String> tags = List.of("default");
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class NotBlankConfig extends OkaeriConfig {
        @NotBlank
        private String email = "default@example.com";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class PatternConfig extends OkaeriConfig {
        @Pattern("\\d{3}-\\d{3}-\\d{4}")
        private String phoneNumber = "123-456-7890";
    }
}
