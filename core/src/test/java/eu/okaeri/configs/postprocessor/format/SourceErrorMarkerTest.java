package eu.okaeri.configs.postprocessor.format;

import eu.okaeri.configs.serdes.ConfigPath;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SourceErrorMarkerTest {

    @Test
    void testSimpleKeyValue() {
        String yaml = """
            name: John
            age: 30""";

        YamlWalker walker = YamlWalker.of(yaml);
        String marker = SourceErrorMarker.format(walker, ConfigPath.parse("age"), "config.yml");

        assertThat(marker).isEqualTo("""
             --> config.yml:2:6
              |
            2 | age: 30
              |      ^^""");
    }

    @Test
    void testNestedPath() {
        String yaml = """
            database:
              host: localhost
              port: 5432""";

        YamlWalker walker = YamlWalker.of(yaml);
        String marker = SourceErrorMarker.format(walker, ConfigPath.parse("database.port"), "config.yml");

        assertThat(marker).isEqualTo("""
             --> config.yml:3:9
              |
            3 |   port: 5432
              |         ^^^^""");
    }

    @Test
    void testListItem() {
        String yaml = """
            servers:
              - host: server1.com
                port: 8080
              - host: server2.com
                port: invalid""";

        YamlWalker walker = YamlWalker.of(yaml);
        String marker = SourceErrorMarker.format(walker, ConfigPath.parse("servers[1].port"), "app.yml");

        assertThat(marker).isEqualTo("""
             --> app.yml:5:11
              |
            5 |     port: invalid
              |           ^^^^^^^""");
    }

    @Test
    void testKeyOnlyValue() {
        String yaml = """
            settings:
              enabled: true""";

        YamlWalker walker = YamlWalker.of(yaml);
        String marker = SourceErrorMarker.format(walker, ConfigPath.parse("settings"), "config.yml");

        // Should underline the key since there's no value
        assertThat(marker).contains("settings");
        assertThat(marker).contains("^");
    }

    @Test
    void testDoubleDigitLineNumber() {
        StringBuilder yamlBuilder = new StringBuilder();
        for (int i = 1; i <= 15; i++) {
            yamlBuilder.append("key").append(i).append(": value").append(i).append("\n");
        }
        String yaml = yamlBuilder.toString().trim();

        YamlWalker walker = YamlWalker.of(yaml);
        String marker = SourceErrorMarker.format(walker, ConfigPath.parse("key12"), null);

        // Should handle double-digit line numbers with proper alignment
        assertThat(marker).contains("12 |");
        assertThat(marker).contains("   |"); // padding for 2-digit number
    }

    @Test
    void testWithoutSourceFile() {
        String yaml = "name: test";

        YamlWalker walker = YamlWalker.of(yaml);
        String marker = SourceErrorMarker.format(walker, ConfigPath.parse("name"), null);

        assertThat(marker).startsWith(" --> 1:7");
        assertThat(marker).doesNotContain("null");
    }

    @Test
    void testPathNotFound() {
        String yaml = "name: test";

        YamlWalker walker = YamlWalker.of(yaml);
        String marker = SourceErrorMarker.format(walker, ConfigPath.parse("nonexistent.path"), "config.yml");

        assertThat(marker).isEmpty();
    }

    @Test
    void testWithHintMessage() {
        String yaml = """
            database:
              port: invalid""";

        YamlWalker walker = YamlWalker.of(yaml);
        String marker = SourceErrorMarker.format(walker, ConfigPath.parse("database.port"), "config.yml", "expected Integer");

        assertThat(marker).isEqualTo("""
             --> config.yml:2:9
              |
            2 |   port: invalid
              |         ^^^^^^^ expected Integer""");
    }
}
