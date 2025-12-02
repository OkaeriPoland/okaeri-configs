package eu.okaeri.configs.format.hjson;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.Header;
import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.hjson.HjsonConfigurer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayOutputStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Generalized feature tests for all HJSON configurer implementations.
 * Tests common functionality across HJSON configurers.
 * <p>
 * Currently only HjsonConfigurer exists, but structured to support future implementations.
 */
class HjsonConfigurerFeaturesTest {

    static Stream<Arguments> hjsonConfigurers() {
        return Stream.of(
            Arguments.of("Hjson", new HjsonConfigurer())
            // Future implementations can be added here
        );
    }

    @ParameterizedTest(name = "{0}: Load from string")
    @MethodSource("hjsonConfigurers")
    void testLoad_FromString(String configurerName, Configurer configurer) throws Exception {
        // Given: HJSON content as string (quoteless syntax)
        String hjson = """
            {
              name: Test Config
              value: 42
              enabled: true
            }
            """;

        // When: Load into config
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(configurer);
        config.load(hjson);

        // Then: Values are loaded correctly
        assertThat(config.getName()).isEqualTo("Test Config");
        assertThat(config.getValue()).isEqualTo(42);
        assertThat(config.isEnabled()).isTrue();
    }

    @ParameterizedTest(name = "{0}: Comment preservation (native HJSON comments)")
    @MethodSource("hjsonConfigurers")
    void testWrite_CommentPreservation(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with comments
        CommentedConfig config = ConfigManager.create(CommentedConfig.class);
        config.withConfigurer(configurer);
        config.setSimpleField("test value");
        config.setNumberField(999);

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String hjson = output.toString();

        // Then: Comments are present in HJSON output (native HJSON comment support)
        assertThat(hjson).contains("# This is a simple field comment");
        assertThat(hjson).contains("# Multi-line comment");
        assertThat(hjson).contains("# Line 2 of comment");
        assertThat(hjson).contains("simpleField:");
        assertThat(hjson).contains("numberField:");
    }

    @ParameterizedTest(name = "{0}: Header preservation")
    @MethodSource("hjsonConfigurers")
    void testWrite_HeaderPreservation(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with header
        HeaderedConfig config = ConfigManager.create(HeaderedConfig.class);
        config.withConfigurer(configurer);

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String hjson = output.toString();

        // Then: Header is present at the top of HJSON
        assertThat(hjson).contains("# ===================");
        assertThat(hjson).contains("# Test Configuration");
        assertThat(hjson).contains("# Version 1.0");
        assertThat(hjson).contains("# ===================");
    }

    @ParameterizedTest(name = "{0}: Quoteless string syntax")
    @MethodSource("hjsonConfigurers")
    void testWrite_QuotelessStrings(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with simple string values
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(configurer);
        config.setName("simple value");

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String hjson = output.toString();

        // Then: HJSON uses quoteless syntax for simple strings
        assertThat(hjson).contains("name: simple value");
    }

    @ParameterizedTest(name = "{0}: Multiline string support")
    @MethodSource("hjsonConfigurers")
    void testWrite_MultilineStrings(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with multiline string
        MultilineConfig config = ConfigManager.create(MultilineConfig.class);
        config.withConfigurer(configurer);
        config.setLongText("""
            This is a long text
            with multiple lines
            and preserved formatting
            """);

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String hjson = output.toString();

        // Then: Multiline content is preserved
        assertThat(hjson).contains("longText:");
        assertThat(hjson).contains("This is a long text");
        assertThat(hjson).contains("with multiple lines");
    }

    @ParameterizedTest(name = "{0}: Round-trip string preservation")
    @MethodSource("hjsonConfigurers")
    void testRoundTrip_StringValues(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with various string values
        TestConfig original = ConfigManager.create(TestConfig.class);
        original.withConfigurer(configurer);
        original.setName("Test String");
        String hjson = original.saveToString();

        // When: Load into new config
        TestConfig loaded = ConfigManager.create(TestConfig.class);
        loaded.withConfigurer(configurer);
        loaded.load(hjson);

        // Then: Values are preserved
        assertThat(loaded.getName()).isEqualTo("Test String");
    }

    @ParameterizedTest(name = "{0}: Numeric values without quotes")
    @MethodSource("hjsonConfigurers")
    void testWrite_NumericValues(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with numeric values
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(configurer);
        config.setValue(12345);

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String hjson = output.toString();

        // Then: Numbers are written without quotes
        assertThat(hjson).contains("value: 12345");
    }

    @ParameterizedTest(name = "{0}: Boolean values")
    @MethodSource("hjsonConfigurers")
    void testWrite_BooleanValues(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with boolean values
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(configurer);
        config.setEnabled(true);

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String hjson = output.toString();

        // Then: Boolean is written correctly
        assertThat(hjson).contains("enabled: true");
    }

    @ParameterizedTest(name = "{0}: Trailing commas allowed")
    @MethodSource("hjsonConfigurers")
    void testLoad_TrailingCommas(String configurerName, Configurer configurer) throws Exception {
        // Given: HJSON with trailing commas (HJSON feature)
        // Note: HJSON library currently includes trailing commas in string values (known limitation)
        String hjson = """
            {
              name: Test,
              value: 42,
              enabled: true,
            }
            """;

        // When: Load into config
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(configurer);
        config.load(hjson);

        // Then: Values are loaded (note: trailing comma becomes part of string value)
        assertThat(config.getName()).isEqualTo("Test,");
        assertThat(config.getValue()).isEqualTo(42);
        assertThat(config.isEnabled()).isTrue();
    }

    // ==================== Constructor Tests ====================

    @Test
    void testDefaultConstructor() {
        HjsonConfigurer configurer = new HjsonConfigurer();
        assertThat(configurer).isNotNull();
    }

    // ==================== getExtensions Tests ====================

    @Test
    void testGetExtensions() {
        HjsonConfigurer configurer = new HjsonConfigurer();
        assertThat(configurer.getExtensions()).containsExactly("hjson");
    }

    // ==================== Test Config Classes ====================

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

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class MultilineConfig extends OkaeriConfig {
        private String longText = "";
    }
}
