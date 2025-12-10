package eu.okaeri.configs.validator.jakartaee;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.exception.OkaeriException;
import eu.okaeri.configs.exception.ValidationException;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for JakartaValidator integration with okaeri-configs.
 * <p>
 * Tests all common constraints from Jakarta Bean Validation:
 * - @NotNull / @NotBlank / @NotEmpty
 * - @Min / @Max / @DecimalMin / @DecimalMax
 * - @Positive / @PositiveOrZero / @Negative / @NegativeOrZero
 * - @Size (for strings and collections)
 * - @Pattern
 * - @Email
 * - @AssertTrue / @AssertFalse
 */
class JakartaValidatorTest {

    // ==================== @Min / @Max Tests ====================

    @Test
    void testMin_RejectsValueBelowMinimum() {
        // Arrange
        MinMaxConfig config = ConfigManager.create(MinMaxConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new YamlSnakeYamlConfigurer());
                opt.validator(new JakartaValidator());
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
                opt.validator(new JakartaValidator());
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
                opt.validator(new JakartaValidator());
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
                opt.validator(new JakartaValidator());
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
                opt.validator(new JakartaValidator());
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
                opt.validator(new JakartaValidator());
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
                opt.validator(new JakartaValidator());
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
                opt.validator(new JakartaValidator());
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
                opt.validator(new JakartaValidator());
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
                opt.validator(new JakartaValidator());
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
                opt.validator(new JakartaValidator());
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
                opt.validator(new JakartaValidator());
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
                opt.validator(new JakartaValidator());
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
                opt.validator(new JakartaValidator());
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

    // ==================== @NotNull / @NotBlank / @NotEmpty Tests ====================

    @Test
    void testNotNull_RejectsNull() {
        // Arrange
        NotNullConfig config = ConfigManager.create(NotNullConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new YamlSnakeYamlConfigurer());
                opt.validator(new JakartaValidator());
            });
        });

        config.load("value: \"test\"");

        // Act - mutate to null
        config.setValue(null);

        // Assert
        assertThatThrownBy(config::validate)
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("value")
            .hasMessageContaining("invalid");
    }

    @Test
    void testNotBlank_RejectsEmptyString() {
        // Arrange
        NotBlankConfig config = ConfigManager.create(NotBlankConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new YamlSnakeYamlConfigurer());
                opt.validator(new JakartaValidator());
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
                opt.validator(new JakartaValidator());
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
                opt.validator(new JakartaValidator());
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
                opt.validator(new JakartaValidator());
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
                opt.validator(new JakartaValidator());
            });
        });

        // Act & Assert
        assertThatCode(() -> config.load("phoneNumber: \"123-456-7890\""))
            .doesNotThrowAnyException();
        assertThat(config.getPhoneNumber()).isEqualTo("123-456-7890");
    }

    // ==================== @Email Tests ====================

    @Test
    void testEmail_RejectsInvalidFormat() {
        // Arrange
        EmailConfig config = ConfigManager.create(EmailConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new YamlSnakeYamlConfigurer());
                opt.validator(new JakartaValidator());
            });
        });

        // Act & Assert
        assertThatThrownBy(() -> config.load("email: \"not-an-email\""))
            .isInstanceOf(OkaeriException.class)
            .hasMessageContaining("email")
            .hasMessageContaining("invalid");
    }

    @Test
    void testEmail_AcceptsValidFormat() {
        // Arrange
        EmailConfig config = ConfigManager.create(EmailConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new YamlSnakeYamlConfigurer());
                opt.validator(new JakartaValidator());
            });
        });

        // Act & Assert
        assertThatCode(() -> config.load("email: \"user@example.com\""))
            .doesNotThrowAnyException();
        assertThat(config.getEmail()).isEqualTo("user@example.com");
    }

    // ==================== @AssertTrue / @AssertFalse Tests ====================

    @Test
    void testAssertTrue_RejectsFalse() {
        // Arrange
        AssertConfig config = ConfigManager.create(AssertConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new YamlSnakeYamlConfigurer());
                opt.validator(new JakartaValidator());
            });
        });

        // Act & Assert
        assertThatThrownBy(() -> config.load("mustBeTrue: false"))
            .isInstanceOf(OkaeriException.class)
            .hasMessageContaining("mustBeTrue")
            .hasMessageContaining("invalid");
    }

    @Test
    void testAssertTrue_AcceptsTrue() {
        // Arrange
        AssertConfig config = ConfigManager.create(AssertConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new YamlSnakeYamlConfigurer());
                opt.validator(new JakartaValidator());
            });
        });

        // Act & Assert
        assertThatCode(() -> config.load("mustBeTrue: true"))
            .doesNotThrowAnyException();
        assertThat(config.isMustBeTrue()).isTrue();
    }

    @Test
    void testAssertFalse_RejectsTrue() {
        // Arrange
        AssertConfig config = ConfigManager.create(AssertConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new YamlSnakeYamlConfigurer());
                opt.validator(new JakartaValidator());
            });
        });

        // Act & Assert
        assertThatThrownBy(() -> config.load("mustBeFalse: true"))
            .isInstanceOf(OkaeriException.class)
            .hasMessageContaining("mustBeFalse")
            .hasMessageContaining("invalid");
    }

    @Test
    void testAssertFalse_AcceptsFalse() {
        // Arrange
        AssertConfig config = ConfigManager.create(AssertConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new YamlSnakeYamlConfigurer());
                opt.validator(new JakartaValidator());
            });
        });

        // Act & Assert
        assertThatCode(() -> config.load("mustBeFalse: false"))
            .doesNotThrowAnyException();
        assertThat(config.isMustBeFalse()).isFalse();
    }

    // ==================== Runtime Mutation Tests ====================

    @Test
    void testRuntimeMutation_RejectsInvalidValueOnSave() {
        // Arrange
        MinMaxConfig config = ConfigManager.create(MinMaxConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new YamlSnakeYamlConfigurer());
                opt.validator(new JakartaValidator());
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
                opt.validator(new JakartaValidator());
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

    // ==================== Cross-Field Validation Tests (JSR-380 Class-Level Constraints) ====================

    @Test
    void testClassLevelConstraint_PasswordsMatch_RejectsMismatch() {
        // Arrange
        PasswordConfig config = ConfigManager.create(PasswordConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new YamlSnakeYamlConfigurer());
                opt.validator(new JakartaValidator());
            });
        });

        // Act & Assert - passwords don't match
        assertThatThrownBy(() -> config.load("""
            password: secret123
            confirmPassword: different456
            """))
            .isInstanceOf(OkaeriException.class)
            .hasMessageContaining("PasswordConfig")
            .hasMessageContaining("invalid")
            .hasMessageContaining("Passwords must match");
    }

    @Test
    void testClassLevelConstraint_PasswordsMatch_AcceptsMatch() {
        // Arrange
        PasswordConfig config = ConfigManager.create(PasswordConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new YamlSnakeYamlConfigurer());
                opt.validator(new JakartaValidator());
            });
        });

        // Act & Assert - passwords match
        assertThatCode(() -> config.load("""
            password: secret123
            confirmPassword: secret123
            """))
            .doesNotThrowAnyException();

        assertThat(config.getPassword()).isEqualTo("secret123");
        assertThat(config.getConfirmPassword()).isEqualTo("secret123");
    }

    @Test
    void testClassLevelConstraint_DateRange_RejectsInvalidRange() {
        // Arrange
        DateRangeConfig config = ConfigManager.create(DateRangeConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new YamlSnakeYamlConfigurer());
                opt.validator(new JakartaValidator());
            });
        });

        // Act & Assert - end before start (quote dates to prevent YAML parsing as Date objects)
        assertThatThrownBy(() -> config.load("""
            startDate: "2025-12-31"
            endDate: "2025-01-01"
            """))
            .isInstanceOf(OkaeriException.class)
            .hasMessageContaining("DateRangeConfig")
            .hasMessageContaining("invalid")
            .hasMessageContaining("End date must be after start date");
    }

    @Test
    void testClassLevelConstraint_DateRange_AcceptsValidRange() {
        // Arrange
        DateRangeConfig config = ConfigManager.create(DateRangeConfig.class, it -> {
            it.configure(opt -> {
                opt.configurer(new YamlSnakeYamlConfigurer());
                opt.validator(new JakartaValidator());
            });
        });

        // Act & Assert - valid range (quote dates to prevent YAML parsing as Date objects)
        assertThatCode(() -> config.load("""
            startDate: "2025-01-01"
            endDate: "2025-12-31"
            """))
            .doesNotThrowAnyException();

        assertThat(config.getStartDate()).isEqualTo("2025-01-01");
        assertThat(config.getEndDate()).isEqualTo("2025-12-31");
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
    public static class NotNullConfig extends OkaeriConfig {
        @NotNull
        private String value = "default";
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
        @Pattern(regexp = "\\d{3}-\\d{3}-\\d{4}")
        private String phoneNumber = "123-456-7890";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class EmailConfig extends OkaeriConfig {
        @Email
        private String email = "default@example.com";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class AssertConfig extends OkaeriConfig {
        @AssertTrue
        private boolean mustBeTrue = true;

        @AssertFalse
        private boolean mustBeFalse = false;
    }

    /**
     * Config with class-level constraint for password matching (cross-field validation).
     */
    @Data
    @EqualsAndHashCode(callSuper = false)
    @PasswordsMatch
    public static class PasswordConfig extends OkaeriConfig {
        private String password = "";
        private String confirmPassword = "";
    }

    /**
     * Config with class-level constraint for date range validation (cross-field validation).
     */
    @Data
    @EqualsAndHashCode(callSuper = false)
    @ValidDateRange
    public static class DateRangeConfig extends OkaeriConfig {
        private String startDate = "2025-01-01";
        private String endDate = "2025-12-31";
    }

    // ==================== Custom Class-Level Constraint Annotations ====================

    /**
     * Custom class-level constraint ensuring password fields match.
     */
    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = PasswordsMatchValidator.class)
    @interface PasswordsMatch {
        String message() default "Passwords must match";
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};
    }

    /**
     * Validator for @PasswordsMatch constraint.
     * Must be static so Hibernate Validator can instantiate it.
     */
    public static class PasswordsMatchValidator implements ConstraintValidator<PasswordsMatch, PasswordConfig> {
        @Override
        public boolean isValid(PasswordConfig config, ConstraintValidatorContext context) {
            if (config == null) {
                return true;
            }
            String password = config.getPassword();
            String confirmPassword = config.getConfirmPassword();
            return (password != null) && password.equals(confirmPassword);
        }
    }

    /**
     * Custom class-level constraint ensuring end date is after start date.
     */
    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = ValidDateRangeValidator.class)
    @interface ValidDateRange {
        String message() default "End date must be after start date";
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};
    }

    /**
     * Validator for @ValidDateRange constraint.
     * Must be static so Hibernate Validator can instantiate it.
     */
    public static class ValidDateRangeValidator implements ConstraintValidator<ValidDateRange, DateRangeConfig> {
        @Override
        public boolean isValid(DateRangeConfig config, ConstraintValidatorContext context) {
            if (config == null) {
                return true;
            }
            String startDate = config.getStartDate();
            String endDate = config.getEndDate();
            if ((startDate == null) || (endDate == null)) {
                return true;
            }
            return startDate.compareTo(endDate) < 0;
        }
    }
}
