package eu.okaeri.configs.error.ini;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.CustomKey;
import eu.okaeri.configs.annotation.NameModifier;
import eu.okaeri.configs.annotation.NameStrategy;
import eu.okaeri.configs.annotation.Names;
import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.configurer.WrappedConfigurer;
import eu.okaeri.configs.exception.OkaeriConfigException;
import eu.okaeri.configs.properties.IniConfigurer;
import eu.okaeri.configs.properties.PropertiesConfigurer;
import eu.okaeri.configs.serdes.commons.SerdesCommons;
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
 * Tests error messages for INI/Properties configuration loading failures.
 * Parametrized across Properties and INI backends to ensure consistent error reporting.
 */
class IniErrorMessageTest {

    static Stream<Arguments> iniConfigurers() {
        return Stream.of(
            Arguments.of("Properties", new PropertiesConfigurer()),
            Arguments.of("INI", new IniConfigurer())
        );
    }

    static Stream<Arguments> iniOnlyConfigurers() {
        return Stream.of(
            Arguments.of("INI", new IniConfigurer())
        );
    }

    // ==================== Standard Types (StandardSerdes) ====================

    @ParameterizedTest(name = "{0}: Invalid Integer")
    @MethodSource("iniConfigurers")
    void testError_InvalidInteger(String name, Configurer configurer) {
        String ini = "value=not_a_number";

        assertThatThrownBy(() -> this.loadConfig(IntegerConfig.class, configurer, ini))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("value");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'value' to Integer from String
                     --> 1:7
                      |
                    1 | value=not_a_number
                      |       ^^^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    @ParameterizedTest(name = "{0}: Invalid Long")
    @MethodSource("iniConfigurers")
    void testError_InvalidLong(String name, Configurer configurer) {
        String ini = "value=not_a_long";

        assertThatThrownBy(() -> this.loadConfig(LongConfig.class, configurer, ini))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("value");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToLongTransformer]: Cannot transform 'value' to Long from String
                     --> 1:7
                      |
                    1 | value=not_a_long
                      |       ^^^^^^^^^^ Expected long number (e.g. 42, -10, 9999999999)""");
            });
    }

    @ParameterizedTest(name = "{0}: Invalid Double")
    @MethodSource("iniConfigurers")
    void testError_InvalidDouble(String name, Configurer configurer) {
        String ini = "value=not_a_double";

        assertThatThrownBy(() -> this.loadConfig(DoubleConfig.class, configurer, ini))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("value");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToDoubleTransformer]: Cannot transform 'value' to Double from String
                     --> 1:7
                      |
                    1 | value=not_a_double
                      |       ^^^^^^^^^^^^ Expected decimal number (e.g. 3.14, -0.5, 100.0)""");
            });
    }

    @ParameterizedTest(name = "{0}: Invalid Float")
    @MethodSource("iniConfigurers")
    void testError_InvalidFloat(String name, Configurer configurer) {
        String ini = "value=not_a_float";

        assertThatThrownBy(() -> this.loadConfig(FloatConfig.class, configurer, ini))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("value");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToFloatTransformer]: Cannot transform 'value' to Float from String
                     --> 1:7
                      |
                    1 | value=not_a_float
                      |       ^^^^^^^^^^^ Expected float number (e.g. 3.14, -0.5, 100.0)""");
            });
    }

    @ParameterizedTest(name = "{0}: Invalid Boolean")
    @MethodSource("iniConfigurers")
    void testError_InvalidBoolean(String name, Configurer configurer) {
        String ini = "value=not_a_boolean";

        assertThatThrownBy(() -> this.loadConfig(BooleanConfig.class, configurer, ini))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("value");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToBooleanTransformer]: Cannot transform 'value' to Boolean from String
                     --> 1:7
                      |
                    1 | value=not_a_boolean
                      |       ^^^^^^^^^^^^^ Expected true or false""");
            });
    }

    @ParameterizedTest(name = "{0}: Invalid UUID")
    @MethodSource("iniConfigurers")
    void testError_InvalidUuid(String name, Configurer configurer) {
        String ini = "value=not-a-valid-uuid";

        assertThatThrownBy(() -> this.loadConfig(UuidConfig.class, configurer, ini))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("value");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToUuidTransformer]: Cannot transform 'value' to UUID from String
                     --> 1:7
                      |
                    1 | value=not-a-valid-uuid
                      |       ^^^^^^^^^^^^^^^^ Expected UUID (e.g. 550e8400-e29b-41d4-a716-446655440000)""");
            });
    }

    @ParameterizedTest(name = "{0}: Invalid BigDecimal")
    @MethodSource("iniConfigurers")
    void testError_InvalidBigDecimal(String name, Configurer configurer) {
        String ini = "value=not_a_decimal";

        assertThatThrownBy(() -> this.loadConfig(BigDecimalConfig.class, configurer, ini))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("value");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToBigDecimalTransformer]: Cannot transform 'value' to BigDecimal from String
                     --> 1:7
                      |
                    1 | value=not_a_decimal
                      |       ^^^^^^^^^^^^^ Expected precise decimal (e.g. 3.14, -100.50, 0)""");
            });
    }

    @ParameterizedTest(name = "{0}: Invalid BigInteger")
    @MethodSource("iniConfigurers")
    void testError_InvalidBigInteger(String name, Configurer configurer) {
        String ini = "value=not_an_integer";

        assertThatThrownBy(() -> this.loadConfig(BigIntegerConfig.class, configurer, ini))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("value");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToBigIntegerTransformer]: Cannot transform 'value' to BigInteger from String
                     --> 1:7
                      |
                    1 | value=not_an_integer
                      |       ^^^^^^^^^^^^^^ Expected precise integer (e.g. 42, -10, 0)""");
            });
    }

    // ==================== Serdes Commons Types ====================

    @ParameterizedTest(name = "{0}: Invalid Duration")
    @MethodSource("iniConfigurers")
    void testError_InvalidDuration(String name, Configurer configurer) {
        String ini = "value=not_a_duration";

        assertThatThrownBy(() -> this.loadConfigWithCommons(DurationConfig.class, configurer, ini))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("value");
                assertThat(e.getMessage()).isEqualTo("""
                    error[DurationTransformer]: Cannot transform 'value' to Duration from String
                     --> 1:7
                      |
                    1 | value=not_a_duration
                      |       ^^^^^^^^^^^^^^ Text cannot be parsed to a Duration""");
            });
    }

    @ParameterizedTest(name = "{0}: Invalid Instant")
    @MethodSource("iniConfigurers")
    void testError_InvalidInstant(String name, Configurer configurer) {
        String ini = "value=not_an_instant";

        assertThatThrownBy(() -> this.loadConfigWithCommons(InstantConfig.class, configurer, ini))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("value");
                assertThat(e.getMessage()).isEqualTo("""
                    error[InstantSerializer]: Cannot deserialize 'value' to Instant from String
                     --> 1:7
                      |
                    1 | value=not_an_instant
                      |       ^^^^^^^^^^^^^^ Text 'not_an_instant' could not be parsed at index 0""");
            });
    }

    @ParameterizedTest(name = "{0}: Invalid Locale")
    @MethodSource("iniConfigurers")
    void testError_InvalidLocale(String name, Configurer configurer) {
        String ini = "value=12345";

        assertThatThrownBy(() -> this.loadConfigWithCommons(LocaleConfig.class, configurer, ini))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("value");
                assertThat(e.getMessage()).isEqualTo("""
                    error[LocaleTransformer]: Cannot transform 'value' to Locale from String
                     --> 1:7
                      |
                    1 | value=12345
                      |       ^^^^^ Expected locale (e.g. en, en-US, pl-PL)""");
            });
    }

    @ParameterizedTest(name = "{0}: Invalid Pattern")
    @MethodSource("iniConfigurers")
    void testError_InvalidPattern(String name, Configurer configurer) {
        String ini = "value=[invalid(regex";

        assertThatThrownBy(() -> this.loadConfigWithCommons(PatternConfig.class, configurer, ini))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("value");
                assertThat(e.getMessage()).isEqualTo("""
                    error[PatternTransformer]: Cannot transform 'value' to Pattern from String
                     --> 1:20
                      |
                    1 | value=[invalid(regex
                      |                    ^ Unclosed character class""");
            });
    }

    // ==================== Nested Paths ====================

    @ParameterizedTest(name = "{0}: Invalid nested field")
    @MethodSource("iniConfigurers")
    void testError_NestedField(String name, Configurer configurer) {
        String ini = "database.host=localhost\ndatabase.port=not_a_port";

        assertThatThrownBy(() -> this.loadConfig(NestedConfig.class, configurer, ini))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("database.port");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'database.port' to Integer from String
                     --> 2:15
                      |
                    2 | database.port=not_a_port
                      |               ^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    @ParameterizedTest(name = "{0}: Invalid deeply nested field")
    @MethodSource("iniConfigurers")
    void testError_DeeplyNestedField(String name, Configurer configurer) {
        String ini = "level1.level2.level3.value=not_an_int";

        assertThatThrownBy(() -> this.loadConfig(DeepNestedConfig.class, configurer, ini))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("level1.level2.level3.value");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'level1.level2.level3.value' to Integer from String
                     --> 1:28
                      |
                    1 | level1.level2.level3.value=not_an_int
                      |                            ^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    // ==================== List Paths ====================

    @ParameterizedTest(name = "{0}: Invalid list element")
    @MethodSource("iniConfigurers")
    void testError_ListElement(String name, Configurer configurer) {
        String ini = "numbers=1,2,not_a_number,4";

        assertThatThrownBy(() -> this.loadConfig(ListConfig.class, configurer, ini))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("numbers[2]");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'numbers[2]' to Integer from String
                     --> 1:13
                      |
                    1 | numbers=1,2,not_a_number,4
                      |             ^^^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    // ==================== Map Paths ====================

    @ParameterizedTest(name = "{0}: Invalid map value")
    @MethodSource("iniConfigurers")
    void testError_MapValue(String name, Configurer configurer) {
        String ini = "limits.daily=100\nlimits.weekly=not_a_number";

        assertThatThrownBy(() -> this.loadConfig(MapConfig.class, configurer, ini))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("limits[\"weekly\"]");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'limits["weekly"]' to Integer from String
                     --> 2:15
                      |
                    2 | limits.weekly=not_a_number
                      |               ^^^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    // ==================== Enum Errors ====================

    @ParameterizedTest(name = "{0}: Invalid enum value")
    @MethodSource("iniConfigurers")
    void testError_InvalidEnum(String name, Configurer configurer) {
        String ini = "level=MDIUM";

        assertThatThrownBy(() -> this.loadConfig(EnumConfig.class, configurer, ini))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("level");
                assertThat(e.getMessage()).isEqualTo("""
                    Cannot resolve 'level' to Level from String
                     --> 1:7
                      |
                    1 | level=MDIUM
                      |       ^^^^^ Expected MEDIUM, HIGH or LOW""");
            });
    }

    // ==================== INI-specific: Section format ====================

    @ParameterizedTest(name = "{0}: Invalid field in section (INI style)")
    @MethodSource("iniOnlyConfigurers")
    void testError_SectionField(String name, Configurer configurer) {
        String ini = "[database]\nhost=localhost\nport=not_a_port";

        assertThatThrownBy(() -> this.loadConfig(NestedConfig.class, configurer, ini))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("database.port");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'database.port' to Integer from String
                     --> 3:6
                      |
                    3 | port=not_a_port
                      |      ^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    // ==================== Sibling Subconfig Accumulation Test ====================

    /**
     * Tests that error paths don't accumulate across sibling subconfigs.
     * This was a bug where processing subconfig A would pollute the parent's basePath,
     * causing subconfig B's error to show path "A.B.field" instead of just "B.field".
     */
    @ParameterizedTest(name = "{0}: Sibling subconfigs don't accumulate paths")
    @MethodSource("iniConfigurers")
    void testError_SiblingSubconfigsDoNotAccumulatePaths(String name, Configurer configurer) {
        // Config with 3 sibling subconfigs, error in the last one
        String ini = """
            sectionA.valueA=100
            sectionB.valueB=200
            sectionC.valueC=not_a_number
            """;

        assertThatThrownBy(() -> this.loadConfig(SiblingSubconfigsConfig.class, configurer, ini))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                // Path should be just "sectionC.valueC", NOT "sectionA.sectionB.sectionC.valueC"
                assertThat(e.getPath().toString()).isEqualTo("sectionC.valueC");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'sectionC.valueC' to Integer from String
                     --> 3:17
                      |
                    3 | sectionC.valueC=not_a_number
                      |                 ^^^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    // ==================== @CustomKey Annotation Tests ====================

    /**
     * Tests that error messages work correctly with @CustomKey annotation.
     * The INI uses the custom key name (e.g., "custom-value"), and the error
     * message should correctly locate and display the source line.
     */
    @ParameterizedTest(name = "{0}: @CustomKey simple field")
    @MethodSource("iniConfigurers")
    void testError_CustomKey_SimpleField(String name, Configurer configurer) {
        String ini = "custom-value=not_a_number";

        assertThatThrownBy(() -> this.loadConfig(CustomKeyConfig.class, configurer, ini))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("custom-value");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'custom-value' to Integer from String
                     --> 1:14
                      |
                    1 | custom-value=not_a_number
                      |              ^^^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    /**
     * Tests that error messages work correctly with nested @CustomKey annotations.
     * Both the parent field and nested field use custom key names.
     */
    @ParameterizedTest(name = "{0}: @CustomKey nested field")
    @MethodSource("iniConfigurers")
    void testError_CustomKey_NestedField(String name, Configurer configurer) {
        String ini = "some.custom-key.nested-value=not_a_number";

        assertThatThrownBy(() -> this.loadConfig(NestedCustomKeyConfig.class, configurer, ini))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("some.custom-key.nested-value");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'some.custom-key.nested-value' to Integer from String
                     --> 1:30
                      |
                    1 | some.custom-key.nested-value=not_a_number
                      |                              ^^^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    /**
     * Tests that error messages work correctly when only the parent has @CustomKey.
     */
    @ParameterizedTest(name = "{0}: @CustomKey on parent only")
    @MethodSource("iniConfigurers")
    void testError_CustomKey_ParentOnly(String name, Configurer configurer) {
        String ini = "custom-parent.value=not_a_number";

        assertThatThrownBy(() -> this.loadConfig(CustomKeyParentConfig.class, configurer, ini))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("custom-parent.value");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'custom-parent.value' to Integer from String
                     --> 1:21
                      |
                    1 | custom-parent.value=not_a_number
                      |                     ^^^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    // ==================== @Names Annotation Tests (HYPHEN_CASE) ====================

    /**
     * Tests that error messages work correctly with @Names(strategy = HYPHEN_CASE).
     * This tests configs using @Names strategy where field names are transformed
     * to hyphen-case (e.g., updateRate -> update-rate).
     */
    @ParameterizedTest(name = "{0}: @Names HYPHEN_CASE nested field (scoreboard.dummy.update-rate pattern)")
    @MethodSource("iniConfigurers")
    void testError_Names_HyphenCase_NestedField(String name, Configurer configurer) {
        String ini = "scoreboard.dummy.update-rate=hello";

        assertThatThrownBy(() -> this.loadConfigWithCommons(ScoreboardConfig.class, configurer, ini))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("scoreboard.dummy.update-rate");
                assertThat(e.getMessage()).isEqualTo("""
                    error[DurationTransformer]: Cannot transform 'scoreboard.dummy.update-rate' to Duration from String
                     --> 1:30
                      |
                    1 | scoreboard.dummy.update-rate=hello
                      |                              ^^^^^ Text cannot be parsed to a Duration""");
            });
    }

    /**
     * Tests @Names HYPHEN_CASE with a simple nested field.
     */
    @ParameterizedTest(name = "{0}: @Names HYPHEN_CASE simple nested")
    @MethodSource("iniConfigurers")
    void testError_Names_HyphenCase_SimpleNested(String name, Configurer configurer) {
        String ini = "my-section.my-value=not_a_number";

        assertThatThrownBy(() -> this.loadConfig(HyphenCaseConfig.class, configurer, ini))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("my-section.my-value");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'my-section.my-value' to Integer from String
                     --> 1:21
                      |
                    1 | my-section.my-value=not_a_number
                      |                     ^^^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    // ==================== errorComments Option Tests ====================

    /**
     * Tests that errorComments(true) config option automatically includes
     * all consecutive comment lines above the error field.
     */
    @ParameterizedTest(name = "{0}: errorComments includes consecutive comments above field")
    @MethodSource("iniConfigurers")
    void testError_ErrorCommentsOption_IncludesConsecutiveComments(String name, Configurer configurer) {
        String ini = """
            name=test
            ; This is a comment about the value
            ; Value must be a valid integer
            value=invalid
            debug=false
            """;

        assertThatThrownBy(() -> this.loadConfigWithErrorComments(IntegerConfig.class, configurer, ini))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("value");
                // errorComments(true) should include both comment lines above the field
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'value' to Integer from String
                     --> 4:7
                      |
                    2 | ; This is a comment about the value
                    3 | ; Value must be a valid integer
                    4 | value=invalid
                      |       ^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    /**
     * Tests that errorComments stops at non-comment lines (doesn't cross blank lines).
     */
    @ParameterizedTest(name = "{0}: errorComments stops at blank lines")
    @MethodSource("iniConfigurers")
    void testError_ErrorCommentsOption_StopsAtBlankLines(String name, Configurer configurer) {
        String ini = """
            name=test
            ; This comment is separated by blank line

            ; This is the only comment directly above
            value=invalid
            """;

        assertThatThrownBy(() -> this.loadConfigWithErrorComments(IntegerConfig.class, configurer, ini))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("value");
                // Only the comment directly above (no blank line) should be included
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'value' to Integer from String
                     --> 5:7
                      |
                    4 | ; This is the only comment directly above
                    5 | value=invalid
                      |       ^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    // ==================== WrappedConfigurer Integration Tests ====================

    /**
     * Integration test for WrappedConfigurer wrapping format configurer.
     * Tests that rawContent is properly propagated for createSourceWalker().
     */
    @ParameterizedTest(name = "{0}: WrappedConfigurer integration - @Names HYPHEN_CASE subconfig")
    @MethodSource("iniOnlyConfigurers")
    void testError_WrappedConfigurer_HyphenCaseSubconfig(String name, Configurer configurer) {
        String ini = """
            [scoreboard.dummy]
            update-rate=invalid_duration
            """;

        WrappedConfigurer wrappedConfigurer = new WrappedConfigurer(configurer);
        wrappedConfigurer.register(new SerdesCommons());

        assertThatThrownBy(() -> this.loadConfig(ScoreboardConfig.class, wrappedConfigurer, ini))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("scoreboard.dummy.update-rate");
                assertThat(e.getMessage()).isEqualTo("""
                    error[DurationTransformer]: Cannot transform 'scoreboard.dummy.update-rate' to Duration from String
                     --> 2:13
                      |
                    2 | update-rate=invalid_duration
                      |             ^^^^^^^^^^^^^^^^ Text cannot be parsed to a Duration""");
            });
    }

    /**
     * Integration test for WrappedConfigurer with @CustomKey annotation.
     */
    @ParameterizedTest(name = "{0}: WrappedConfigurer integration - @CustomKey")
    @MethodSource("iniConfigurers")
    void testError_WrappedConfigurer_CustomKey(String name, Configurer configurer) {
        String ini = "custom-value=not_a_number";

        WrappedConfigurer wrappedConfigurer = new WrappedConfigurer(configurer);

        assertThatThrownBy(() -> this.loadConfig(CustomKeyConfig.class, wrappedConfigurer, ini))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("custom-value");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'custom-value' to Integer from String
                     --> 1:14
                      |
                    1 | custom-value=not_a_number
                      |              ^^^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    // ==================== Helper Methods ====================

    private <T extends OkaeriConfig> T loadConfig(Class<T> clazz, Configurer configurer, String content) {
        T config = ConfigManager.create(clazz);
        config.setConfigurer(configurer);
        config.load(content);
        return config;
    }

    private <T extends OkaeriConfig> T loadConfigWithCommons(Class<T> clazz, Configurer configurer, String content) {
        T config = ConfigManager.create(clazz);
        configurer.register(new SerdesCommons());
        config.setConfigurer(configurer);
        config.load(content);
        return config;
    }

    private <T extends OkaeriConfig> T loadConfigWithErrorComments(Class<T> clazz, Configurer configurer, String content) {
        T config = ConfigManager.create(clazz, it -> {
            it.configure(opt -> {
                opt.configurer(configurer);
                opt.errorComments(true);
            });
        });
        config.load(content);
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
