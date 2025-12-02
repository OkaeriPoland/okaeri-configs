package eu.okaeri.configs.properties;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests PropertiesConfigurer-specific features.
 * Only tests for backend-specific functionality not covered by parameterized tests in core-test.
 */
class PropertiesConfigurerFeaturesTest {

    @Test
    void testCustomCommentPrefix() throws Exception {
        // Given: Configurer with custom comment prefix
        PropertiesConfigurer configurer = new PropertiesConfigurer();
        configurer.setCommentPrefix("#> ");

        CommentedConfig config = ConfigManager.create(CommentedConfig.class);
        config.setConfigurer(configurer);

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String properties = output.toString();

        // Then: Custom comment prefix is used
        assertThat(properties).contains("#> This is a field comment");
        assertThat(properties).doesNotContain("# This is a field comment");
    }

    @Test
    void testCustomSimpleListMaxLineLength() throws Exception {
        // Given: Configurer with custom line length threshold (very short)
        PropertiesConfigurer configurer = new PropertiesConfigurer();
        configurer.setSimpleListMaxLineLength(20); // Force index format for most lists

        ListConfig config = ConfigManager.create(ListConfig.class);
        config.setConfigurer(configurer);
        config.setItems(List.of("alpha", "beta", "gamma"));

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String properties = output.toString();

        // Then: Index format used (exceeds 20 char threshold)
        assertThat(properties).contains("items.0=alpha");
        assertThat(properties).contains("items.1=beta");
        assertThat(properties).contains("items.2=gamma");
    }

    @Test
    void testLargeSimpleListMaxLineLength() throws Exception {
        // Given: Configurer with large line length threshold
        PropertiesConfigurer configurer = new PropertiesConfigurer();
        configurer.setSimpleListMaxLineLength(200); // Allow longer comma lists

        ListConfig config = ConfigManager.create(ListConfig.class);
        config.setConfigurer(configurer);
        config.setItems(List.of("very-long-item-one", "very-long-item-two", "very-long-item-three", "very-long-item-four"));

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String properties = output.toString();

        // Then: Comma format used (within 200 char threshold)
        assertThat(properties).contains("items=very-long-item-one,very-long-item-two,very-long-item-three,very-long-item-four");
    }

    // ==================== Constructor Tests ====================

    @Test
    void testDefaultConstructor() {
        PropertiesConfigurer configurer = new PropertiesConfigurer();
        assertThat(configurer).isNotNull();
    }

    @Test
    void testConstructorWithMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("key", "value");
        PropertiesConfigurer configurer = new PropertiesConfigurer(map);
        assertThat(configurer).isNotNull();
        assertThat(configurer.getValue("key")).isEqualTo("value");
    }

    // ==================== getExtensions Tests ====================

    @Test
    void testGetExtensions() {
        PropertiesConfigurer configurer = new PropertiesConfigurer();
        assertThat(configurer.getExtensions()).containsExactly("properties");
    }

    // ==================== Test Config Classes ====================

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class CommentedConfig extends OkaeriConfig {
        @Comment("This is a field comment")
        private String field = "default";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class ListConfig extends OkaeriConfig {
        private List<String> items = List.of();
    }
}
