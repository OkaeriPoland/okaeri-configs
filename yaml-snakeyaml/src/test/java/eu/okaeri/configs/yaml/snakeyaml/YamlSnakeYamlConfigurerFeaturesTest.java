package eu.okaeri.configs.yaml.snakeyaml;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests YamlSnakeYamlConfigurer-specific features.
 * Only tests for backend-specific functionality not covered by parameterized tests.
 */
class YamlSnakeYamlConfigurerFeaturesTest {

    @Test
    void testCustomCommentPrefix() throws Exception {
        // Given: Configurer with custom comment prefix
        YamlSnakeYamlConfigurer configurer = new YamlSnakeYamlConfigurer();
        configurer.setCommentPrefix("#> ");

        CommentedConfig config = ConfigManager.create(CommentedConfig.class);
        config.withConfigurer(configurer);

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String yaml = output.toString();

        // Then: Custom comment prefix is used
        assertThat(yaml).contains("#> This is a simple field comment");
        assertThat(yaml).doesNotContain("# This is a simple field comment");
    }

    // ==================== Constructor Tests ====================

    @Test
    void testDefaultConstructor() {
        YamlSnakeYamlConfigurer configurer = new YamlSnakeYamlConfigurer();
        assertThat(configurer).isNotNull();
    }

    @Test
    void testConstructorWithYaml() {
        Yaml yaml = new Yaml();
        YamlSnakeYamlConfigurer configurer = new YamlSnakeYamlConfigurer(yaml);
        assertThat(configurer).isNotNull();
    }


    // ==================== getExtensions Tests ====================

    @Test
    void testGetExtensions() {
        YamlSnakeYamlConfigurer configurer = new YamlSnakeYamlConfigurer();
        assertThat(configurer.getExtensions()).containsExactly("yml", "yaml");
    }

    // ==================== Test Config Classes ====================

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class CommentedConfig extends OkaeriConfig {
        @Comment("This is a simple field comment")
        private String simpleField = "default";

        @Comment({"Multi-line comment", "Line 2 of comment"})
        private int numberField = 42;
    }
}
