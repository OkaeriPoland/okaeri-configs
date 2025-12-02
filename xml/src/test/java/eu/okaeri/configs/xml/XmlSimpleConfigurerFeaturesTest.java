package eu.okaeri.configs.xml;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.Header;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests XmlSimpleConfigurer-specific features.
 * Only tests for backend-specific functionality not covered by parameterized tests in core-test.
 */
class XmlSimpleConfigurerFeaturesTest {

    @Test
    void testCustomIndent(@TempDir Path tempDir) throws Exception {
        // Given: Configurer with custom indent
        XmlSimpleConfigurer configurer = new XmlSimpleConfigurer();
        configurer.setIndent(4);

        TestConfig config = ConfigManager.create(TestConfig.class);
        config.setConfigurer(configurer);
        config.setName("Test");
        config.setValue(42);

        // When: Save to file
        Path file = tempDir.resolve("indent.xml");
        config.save(file);
        String xml = Files.readString(file);

        // Then: XML is formatted with proper structure
        assertThat(xml).contains("<config>");
        assertThat(xml).contains("<name>Test</name>");
        assertThat(xml).contains("<value>42</value>");
        assertThat(xml).contains("</config>");
    }

    @Test
    void testComments_HeaderAndFieldComments(@TempDir Path tempDir) throws Exception {
        // Given: Config with header and field comments
        CommentedConfig config = ConfigManager.create(CommentedConfig.class);
        config.setConfigurer(new XmlSimpleConfigurer());
        config.setDatabaseHost("localhost");
        config.setDatabasePort(5432);

        // When: Save to file
        Path file = tempDir.resolve("commented.xml");
        config.save(file);
        String xml = Files.readString(file);

        // Then: Comments are present in XML (XML supports comments unlike Properties)
        assertThat(xml).contains("<!-- Configuration File -->");
        assertThat(xml).contains("<!-- Database connection settings -->");
        assertThat(xml).contains("<!-- The database host -->");
        assertThat(xml).contains("<!-- Port number -->");
    }

    // ==================== Constructor Tests ====================

    @Test
    void testDefaultConstructor() {
        XmlSimpleConfigurer configurer = new XmlSimpleConfigurer();
        assertThat(configurer).isNotNull();
    }

    @Test
    void testConstructorWithMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("key", "value");
        XmlSimpleConfigurer configurer = new XmlSimpleConfigurer(map);
        assertThat(configurer).isNotNull();
        assertThat(configurer.getValue("key")).isEqualTo("value");
    }

    // ==================== getExtensions Tests ====================

    @Test
    void testGetExtensions() {
        XmlSimpleConfigurer configurer = new XmlSimpleConfigurer();
        assertThat(configurer.getExtensions()).containsExactly("xml");
    }

    // ==================== Test Config Classes ====================

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class TestConfig extends OkaeriConfig {
        private String name = "default";
        private int value = 0;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @Header("Configuration File")
    @Header("Database connection settings")
    public static class CommentedConfig extends OkaeriConfig {
        @Comment("The database host")
        private String databaseHost = "localhost";

        @Comment("Port number")
        private int databasePort = 3306;
    }
}
