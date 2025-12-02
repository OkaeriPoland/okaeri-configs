package eu.okaeri.configs.postprocessor.format;

import eu.okaeri.configs.format.SourceErrorMarker;
import eu.okaeri.configs.format.yaml.YamlSourceWalker;
import eu.okaeri.configs.serdes.ConfigPath;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SourceErrorMarkerTest {

    @Test
    void testSimpleKeyValue() {
        String yaml = """
            name: John
            age: 30""";

        YamlSourceWalker walker = YamlSourceWalker.of(yaml);
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

        YamlSourceWalker walker = YamlSourceWalker.of(yaml);
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

        YamlSourceWalker walker = YamlSourceWalker.of(yaml);
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

        YamlSourceWalker walker = YamlSourceWalker.of(yaml);
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

        YamlSourceWalker walker = YamlSourceWalker.of(yaml);
        String marker = SourceErrorMarker.format(walker, ConfigPath.parse("key12"), null);

        // Should handle double-digit line numbers with proper alignment
        assertThat(marker).contains("12 |");
        assertThat(marker).contains("   |"); // padding for 2-digit number
    }

    @Test
    void testWithoutSourceFile() {
        String yaml = "name: test";

        YamlSourceWalker walker = YamlSourceWalker.of(yaml);
        String marker = SourceErrorMarker.format(walker, ConfigPath.parse("name"), null);

        assertThat(marker).startsWith(" --> 1:7");
        assertThat(marker).doesNotContain("null");
    }

    @Test
    void testPathNotFound() {
        String yaml = "name: test";

        YamlSourceWalker walker = YamlSourceWalker.of(yaml);
        String marker = SourceErrorMarker.format(walker, ConfigPath.parse("nonexistent.path"), "config.yml");

        assertThat(marker).isEmpty();
    }

    @Test
    void testWithHintMessage() {
        String yaml = """
            database:
              port: invalid""";

        YamlSourceWalker walker = YamlSourceWalker.of(yaml);
        String marker = SourceErrorMarker.format(walker, ConfigPath.parse("database.port"), "config.yml", "expected Integer");

        assertThat(marker).isEqualTo("""
             --> config.yml:2:9
              |
            2 |   port: invalid
              |         ^^^^^^^ expected Integer""");
    }

    @Test
    void testContextLinesBefore() {
        String yaml = """
            first: 1
            second: 2
            third: 3
            target: invalid
            fifth: 5""";

        YamlSourceWalker walker = YamlSourceWalker.of(yaml);
        String marker = SourceErrorMarker.format(walker, ConfigPath.parse("target"), "config.yml", null, -1, 1, yaml, 2, 0);

        assertThat(marker).isEqualTo("""
             --> config.yml:4:9
              |
            2 | second: 2
            3 | third: 3
            4 | target: invalid
              |         ^^^^^^^""");
    }

    @Test
    void testContextLinesAfter() {
        String yaml = """
            first: 1
            target: invalid
            third: 3
            fourth: 4
            fifth: 5""";

        YamlSourceWalker walker = YamlSourceWalker.of(yaml);
        String marker = SourceErrorMarker.format(walker, ConfigPath.parse("target"), "config.yml", "expected Integer", -1, 1, yaml, 0, 2);

        assertThat(marker).isEqualTo("""
             --> config.yml:2:9
              |
            2 | target: invalid
              |         ^^^^^^^ expected Integer
            3 | third: 3
            4 | fourth: 4""");
    }

    @Test
    void testContextLinesBeforeAndAfter() {
        String yaml = """
            first: 1
            second: 2
            third: 3
            target: invalid
            fifth: 5
            sixth: 6
            seventh: 7""";

        YamlSourceWalker walker = YamlSourceWalker.of(yaml);
        String marker = SourceErrorMarker.format(walker, ConfigPath.parse("target"), "config.yml", "expected Integer", -1, 1, yaml, 2, 2);

        assertThat(marker).isEqualTo("""
             --> config.yml:4:9
              |
            2 | second: 2
            3 | third: 3
            4 | target: invalid
              |         ^^^^^^^ expected Integer
            5 | fifth: 5
            6 | sixth: 6""");
    }

    @Test
    void testContextLinesAtStart() {
        String yaml = """
            target: invalid
            second: 2
            third: 3""";

        YamlSourceWalker walker = YamlSourceWalker.of(yaml);
        String marker = SourceErrorMarker.format(walker, ConfigPath.parse("target"), "config.yml", null, -1, 1, yaml, 2, 1);

        // Should not show negative line numbers, just what's available
        assertThat(marker).isEqualTo("""
             --> config.yml:1:9
              |
            1 | target: invalid
              |         ^^^^^^^
            2 | second: 2""");
    }

    @Test
    void testContextLinesAtEnd() {
        String yaml = """
            first: 1
            second: 2
            target: invalid""";

        YamlSourceWalker walker = YamlSourceWalker.of(yaml);
        String marker = SourceErrorMarker.format(walker, ConfigPath.parse("target"), "config.yml", null, -1, 1, yaml, 1, 2);

        // Should not show lines beyond end
        assertThat(marker).isEqualTo("""
             --> config.yml:3:9
              |
            2 | second: 2
            3 | target: invalid
              |         ^^^^^^^""");
    }

    @Test
    void testContextLinesWithDoubleDigits() {
        StringBuilder yamlBuilder = new StringBuilder();
        for (int i = 1; i <= 15; i++) {
            yamlBuilder.append("key").append(i).append(": value").append(i).append("\n");
        }
        String yaml = yamlBuilder.toString().trim();

        YamlSourceWalker walker = YamlSourceWalker.of(yaml);
        String marker = SourceErrorMarker.format(walker, ConfigPath.parse("key10"), null, null, -1, 1, yaml, 2, 2);

        // Should align line numbers properly with context spanning single to double digits
        assertThat(marker).contains(" 8 | key8: value8");
        assertThat(marker).contains(" 9 | key9: value9");
        assertThat(marker).contains("10 | key10: value10");
        assertThat(marker).contains("11 | key11: value11");
        assertThat(marker).contains("12 | key12: value12");
    }
}
