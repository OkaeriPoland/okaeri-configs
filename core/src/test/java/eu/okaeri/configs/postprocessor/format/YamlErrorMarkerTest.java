package eu.okaeri.configs.postprocessor.format;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class YamlErrorMarkerTest {

    @Test
    void testSimpleKeyValue() {
        String yaml = """
            name: John
            age: 30""";

        String marker = YamlErrorMarker.format(yaml, "age", "config.yml");

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

        String marker = YamlErrorMarker.format(yaml, "database.port", "config.yml");

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

        String marker = YamlErrorMarker.format(yaml, "servers[1].port", "app.yml");

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

        String marker = YamlErrorMarker.format(yaml, "settings", "config.yml");

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

        String marker = YamlErrorMarker.format(yaml, "key12", null);

        // Should handle double-digit line numbers with proper alignment
        assertThat(marker).contains("12 |");
        assertThat(marker).contains("   |"); // padding for 2-digit number
    }

    @Test
    void testWithoutSourceFile() {
        String yaml = "name: test";

        String marker = YamlErrorMarker.format(yaml, "name", null);

        assertThat(marker).startsWith(" --> 1:7");
        assertThat(marker).doesNotContain("null");
    }

    @Test
    void testPathNotFound() {
        String yaml = "name: test";

        String marker = YamlErrorMarker.format(yaml, "nonexistent.path", "config.yml");

        assertThat(marker).isEmpty();
    }

    @Test
    void testWithHintMessage() {
        String yaml = """
            database:
              port: invalid""";

        String marker = YamlErrorMarker.format(yaml, "database.port", "config.yml", "expected Integer");

        assertThat(marker).isEqualTo("""
             --> config.yml:2:9
              |
            2 |   port: invalid
              |         ^^^^^^^ expected Integer""");
    }
}
