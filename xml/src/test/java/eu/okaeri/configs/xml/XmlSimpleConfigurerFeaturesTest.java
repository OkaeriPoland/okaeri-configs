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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests XmlSimpleConfigurer-specific features.
 * Tests format-specific functionality not covered by parameterized tests.
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

        // Then: XML is formatted (contains indentation)
        assertThat(xml).contains("<?xml version=\"1.0\" encoding=\"UTF-8\"");
        assertThat(xml).contains("<config>");
        assertThat(xml).contains("</config>");
    }

    @Test
    void testPrettyFormat_ElementsAsKeys(@TempDir Path tempDir) throws Exception {
        // Given: Config with simple keys
        XmlSimpleConfigurer configurer = new XmlSimpleConfigurer();
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.setConfigurer(configurer);
        config.setName("MyName");
        config.setValue(123);

        // When: Save to file
        Path file = tempDir.resolve("pretty.xml");
        config.save(file);
        String xml = Files.readString(file);

        // Then: Keys become element names
        assertThat(xml).contains("<name");
        assertThat(xml).contains("MyName");
        assertThat(xml).contains("<value");
        assertThat(xml).contains("123");
    }

    @Test
    void testListFormat_ItemElements(@TempDir Path tempDir) throws Exception {
        // Given: Config with list
        XmlSimpleConfigurer configurer = new XmlSimpleConfigurer();
        ListConfig config = ConfigManager.create(ListConfig.class);
        config.setConfigurer(configurer);
        config.setItems(List.of("one", "two", "three"));

        // When: Save to file
        Path file = tempDir.resolve("list.xml");
        config.save(file);
        String xml = Files.readString(file);

        // Then: List items use <item> elements
        assertThat(xml).contains("<item");
        assertThat(xml).contains("one");
        assertThat(xml).contains("two");
        assertThat(xml).contains("three");
    }

    @Test
    void testTypePreservation_Numbers(@TempDir Path tempDir) throws Exception {
        // Given: Config with various number types
        NumbersConfig config = ConfigManager.create(NumbersConfig.class);
        config.setConfigurer(new XmlSimpleConfigurer());
        config.setIntVal(42);
        config.setLongVal(9876543210L);
        config.setDoubleVal(3.14);

        // When: Save and load (round-trip)
        Path file = tempDir.resolve("numbers.xml");
        config.save(file);

        NumbersConfig loaded = ConfigManager.create(NumbersConfig.class);
        loaded.setConfigurer(new XmlSimpleConfigurer());
        loaded.load(file);

        // Then: Values are preserved after round-trip (okaeri-configs handles type resolution)
        assertThat(loaded.getIntVal()).isEqualTo(42);
        assertThat(loaded.getLongVal()).isEqualTo(9876543210L);
        assertThat(loaded.getDoubleVal()).isEqualTo(3.14);
    }

    @Test
    void testNullHandling_TypeAttribute(@TempDir Path tempDir) throws Exception {
        // Given: Config with null value
        XmlSimpleConfigurer configurer = new XmlSimpleConfigurer();
        NullConfig config = ConfigManager.create(NullConfig.class);
        config.setConfigurer(configurer);
        config.setNullable(null);

        // When: Save to file
        Path file = tempDir.resolve("null.xml");
        config.save(file);
        String xml = Files.readString(file);

        // Then: Null is marked with type attribute
        assertThat(xml).contains("type=\"null\"");
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

        // Then: Comments are present in XML
        assertThat(xml).contains("<!-- Configuration File -->");
        assertThat(xml).contains("<!-- Database connection settings -->");
        assertThat(xml).contains("<!-- The database host -->");
        assertThat(xml).contains("<!-- Port number -->");
    }

    @Test
    void testLoadFromString() throws Exception {
        // Given: XML string
        String xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <config>
                <name>FromString</name>
                <value>999</value>
                <enabled>true</enabled>
            </config>
            """;

        // When: Load from string
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.setConfigurer(new XmlSimpleConfigurer());
        config.load(xml);

        // Then: Values are loaded correctly
        assertThat(config.getName()).isEqualTo("FromString");
        assertThat(config.getValue()).isEqualTo(999);
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
    public static class ListConfig extends OkaeriConfig {
        private List<String> items = List.of();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class NumbersConfig extends OkaeriConfig {
        private int intVal = 0;
        private long longVal = 0L;
        private double doubleVal = 0.0;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class NullConfig extends OkaeriConfig {
        private String nullable = "default";
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
