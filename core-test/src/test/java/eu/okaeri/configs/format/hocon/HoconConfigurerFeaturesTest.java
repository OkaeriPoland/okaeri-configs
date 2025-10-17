package eu.okaeri.configs.format.hocon;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.Header;
import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.hocon.lightbend.HoconLightbendConfigurer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayOutputStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Generalized feature tests for all HOCON configurer implementations.
 * Tests common functionality across HOCON configurers.
 * <p>
 * Currently only HoconLightbendConfigurer exists, but structured to support future implementations.
 */
class HoconConfigurerFeaturesTest {

    static Stream<Arguments> hoconConfigurers() {
        return Stream.of(
            Arguments.of("Lightbend", new HoconLightbendConfigurer())
            // Future implementations can be added here
        );
    }

    @ParameterizedTest(name = "{0}: Load from string")
    @MethodSource("hoconConfigurers")
    void testLoad_FromString(String configurerName, Configurer configurer) throws Exception {
        // Given: HOCON content as string
        String hocon = """
            name = "Test Config"
            value = 42
            enabled = true
            """;

        // When: Load into config
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(configurer);
        config.load(hocon);

        // Then: Values are loaded correctly
        assertThat(config.getName()).isEqualTo("Test Config");
        assertThat(config.getValue()).isEqualTo(42);
        assertThat(config.isEnabled()).isTrue();
    }

    @ParameterizedTest(name = "{0}: Comment preservation")
    @MethodSource("hoconConfigurers")
    void testWrite_CommentPreservation(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with comments
        CommentedConfig config = ConfigManager.create(CommentedConfig.class);
        config.withConfigurer(configurer);
        config.setSimpleField("test value");
        config.setNumberField(999);

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String hocon = output.toString();

        // Then: Comments are present in HOCON output
        assertThat(hocon).contains("# This is a simple field comment");
        assertThat(hocon).contains("# Multi-line comment");
        assertThat(hocon).contains("# Line 2 of comment");
        assertThat(hocon).contains("simpleField");
        assertThat(hocon).contains("numberField");
    }

    @ParameterizedTest(name = "{0}: Header preservation")
    @MethodSource("hoconConfigurers")
    void testWrite_HeaderPreservation(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with header
        HeaderedConfig config = ConfigManager.create(HeaderedConfig.class);
        config.withConfigurer(configurer);

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String hocon = output.toString();

        // Then: Header is present at the top of HOCON
        assertThat(hocon).contains("# ===================");
        assertThat(hocon).contains("# Test Configuration");
        assertThat(hocon).contains("# Version 1.0");
        assertThat(hocon).contains("# ===================");
    }

    @ParameterizedTest(name = "{0}: Equals sign syntax")
    @MethodSource("hoconConfigurers")
    void testLoad_EqualsSyntax(String configurerName, Configurer configurer) throws Exception {
        // Given: HOCON with = instead of : (HOCON feature)
        String hocon = """
            name = "Test"
            value = 123
            enabled = false
            """;

        // When: Load into config
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(configurer);
        config.load(hocon);

        // Then: Values are loaded correctly
        assertThat(config.getName()).isEqualTo("Test");
        assertThat(config.getValue()).isEqualTo(123);
        assertThat(config.isEnabled()).isFalse();
    }

    @ParameterizedTest(name = "{0}: Round-trip string preservation")
    @MethodSource("hoconConfigurers")
    void testRoundTrip_StringValues(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with various string values
        TestConfig original = ConfigManager.create(TestConfig.class);
        original.withConfigurer(configurer);
        original.setName("Test String");
        String hocon = original.saveToString();

        // When: Load into new config
        TestConfig loaded = ConfigManager.create(TestConfig.class);
        loaded.withConfigurer(configurer);
        loaded.load(hocon);

        // Then: Values are preserved
        assertThat(loaded.getName()).isEqualTo("Test String");
    }

    @ParameterizedTest(name = "{0}: Numeric values")
    @MethodSource("hoconConfigurers")
    void testWrite_NumericValues(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with numeric values
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(configurer);
        config.setValue(12345);

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String hocon = output.toString();

        // Then: Numbers are written
        assertThat(hocon).contains("value");
        assertThat(hocon).contains("12345");
    }

    @ParameterizedTest(name = "{0}: Boolean values")
    @MethodSource("hoconConfigurers")
    void testWrite_BooleanValues(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with boolean values
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(configurer);
        config.setEnabled(true);

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String hocon = output.toString();

        // Then: Boolean is written correctly
        assertThat(hocon).contains("enabled");
        assertThat(hocon).contains("true");
    }

    @ParameterizedTest(name = "{0}: Quoteless string loading")
    @MethodSource("hoconConfigurers")
    void testLoad_QuotelessStrings(String configurerName, Configurer configurer) throws Exception {
        // Given: HOCON with unquoted strings (HOCON feature)
        String hocon = """
            name = SimpleValue
            value = 42
            enabled = true
            """;

        // When: Load into config
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(configurer);
        config.load(hocon);

        // Then: Unquoted string is loaded correctly
        assertThat(config.getName()).isEqualTo("SimpleValue");
    }

    @ParameterizedTest(name = "{0}: Trailing commas allowed")
    @MethodSource("hoconConfigurers")
    void testLoad_TrailingCommas(String configurerName, Configurer configurer) throws Exception {
        // Given: HOCON with trailing commas (HOCON feature)
        String hocon = """
            name = "Test",
            value = 42,
            enabled = true,
            """;

        // When: Load into config
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(configurer);
        config.load(hocon);

        // Then: Values are loaded correctly
        assertThat(config.getName()).isEqualTo("Test");
        assertThat(config.getValue()).isEqualTo(42);
        assertThat(config.isEnabled()).isTrue();
    }

    // Test config classes

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class TestConfig extends OkaeriConfig {
        private String name = "default";
        private int value = 0;
        private boolean enabled = false;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class CommentedConfig extends OkaeriConfig {
        @Comment("This is a simple field comment")
        private String simpleField = "default";

        @Comment({"Multi-line comment", "Line 2 of comment"})
        private int numberField = 42;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @Header("===================")
    @Header("Test Configuration")
    @Header("Version 1.0")
    @Header("===================")
    public static class HeaderedConfig extends OkaeriConfig {
        private String field = "value";
    }
}
