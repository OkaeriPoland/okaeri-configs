package eu.okaeri.configs.yaml.bukkit;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests YamlBukkitConfigurer-specific features.
 * Only tests for backend-specific functionality not covered by parameterized tests.
 */
class YamlBukkitConfigurerFeaturesTest {

    @Test
    void testCustomCommentPrefix() throws Exception {
        // Given: Configurer with custom comment prefix
        YamlBukkitConfigurer configurer = new YamlBukkitConfigurer();
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

    // Test config class

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class CommentedConfig extends OkaeriConfig {
        @Comment("This is a simple field comment")
        private String simpleField = "default";

        @Comment({"Multi-line comment", "Line 2 of comment"})
        private int numberField = 42;
    }
}
