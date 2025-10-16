package eu.okaeri.configs.annotations;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Header;
import eu.okaeri.configs.annotation.Headers;
import eu.okaeri.configs.schema.ConfigDeclaration;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for @Header and @Headers annotations.
 * <p>
 * Verifies:
 * - Single @Header with one line
 * - Single @Header with multiple lines
 * - @Headers with multiple @Header annotations
 * - Header is included in declaration
 * - No header when annotation absent
 * <p>
 * Note: YAML comment formatting tests (# prefix, positioning) are in yaml-snakeyaml module.
 */
class HeaderAnnotationTest {

    // Test configs

    @Data
    @EqualsAndHashCode(callSuper = false)
    @Header("Single line header")
    public static class SingleLineHeaderConfig extends OkaeriConfig {
        private String testField = "value";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @Header({"Line 1", "Line 2", "Line 3"})
    public static class MultiLineHeaderConfig extends OkaeriConfig {
        private String testField = "value";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @Headers({
        @Header("Header Group 1"),
        @Header("Header Group 2"),
        @Header({"Header Group 3 - Line 1", "Header Group 3 - Line 2"})
    })
    public static class MultipleHeadersConfig extends OkaeriConfig {
        private String testField = "value";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class NoHeaderConfig extends OkaeriConfig {
        private String testField = "value";
    }

    // Tests

    @Test
    void testHeader_SingleLine_InDeclaration() {
        // Given
        SingleLineHeaderConfig config = ConfigManager.create(SingleLineHeaderConfig.class);

        // When
        ConfigDeclaration declaration = config.getDeclaration();

        // Then
        assertThat(declaration).isNotNull();
        assertThat(declaration.getHeader()).isNotNull();
        assertThat(declaration.getHeader()).containsExactly("Single line header");
    }

    @Test
    void testHeader_MultiLine_InDeclaration() {
        // Given
        MultiLineHeaderConfig config = ConfigManager.create(MultiLineHeaderConfig.class);

        // When
        ConfigDeclaration declaration = config.getDeclaration();

        // Then
        assertThat(declaration).isNotNull();
        assertThat(declaration.getHeader()).isNotNull();
        assertThat(declaration.getHeader()).containsExactly("Line 1", "Line 2", "Line 3");
    }

    @Test
    void testHeaders_MultipleAnnotations_InDeclaration() {
        // Given
        MultipleHeadersConfig config = ConfigManager.create(MultipleHeadersConfig.class);

        // When
        ConfigDeclaration declaration = config.getDeclaration();

        // Then
        assertThat(declaration).isNotNull();
        assertThat(declaration.getHeader()).isNotNull();
        assertThat(declaration.getHeader()).containsExactly(
            "Header Group 1",
            "Header Group 2",
            "Header Group 3 - Line 1",
            "Header Group 3 - Line 2"
        );
    }

    @Test
    void testHeader_NoAnnotation_NullInDeclaration() {
        // Given
        NoHeaderConfig config = ConfigManager.create(NoHeaderConfig.class);

        // When
        ConfigDeclaration declaration = config.getDeclaration();

        // Then
        assertThat(declaration).isNotNull();
        assertThat(declaration.getHeader()).isNull();
    }

    @Test
    void testHeader_DeclarationCaching_WorksCorrectly() {
        // Given
        SingleLineHeaderConfig config1 = ConfigManager.create(SingleLineHeaderConfig.class);
        SingleLineHeaderConfig config2 = ConfigManager.create(SingleLineHeaderConfig.class);

        // When
        ConfigDeclaration declaration1 = config1.getDeclaration();
        ConfigDeclaration declaration2 = config2.getDeclaration();

        // Then - Both should have the same header from cached template
        assertThat(declaration1.getHeader()).containsExactly("Single line header");
        assertThat(declaration2.getHeader()).containsExactly("Single line header");
    }
}
