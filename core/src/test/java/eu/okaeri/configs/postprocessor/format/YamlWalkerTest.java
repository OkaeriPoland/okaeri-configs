package eu.okaeri.configs.postprocessor.format;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class YamlWalkerTest {

    @Test
    void testSimpleKeyValue() {
        String yaml = """
            name: John
            age: 30""";
        YamlWalker walker = YamlWalker.of(yaml);

        YamlLine nameLine = walker.findLine("name");
        assertThat(nameLine).isNotNull();
        assertThat(nameLine.getLineNumber()).isEqualTo(1);
        assertThat(nameLine.getKey()).isEqualTo("name");
        assertThat(nameLine.getValue()).isEqualTo("John");
        assertThat(nameLine.getType()).isEqualTo(YamlLineType.KEY_VALUE);

        YamlLine ageLine = walker.findLine("age");
        assertThat(ageLine).isNotNull();
        assertThat(ageLine.getLineNumber()).isEqualTo(2);
        assertThat(ageLine.getKey()).isEqualTo("age");
        assertThat(ageLine.getValue()).isEqualTo("30");
    }

    @Test
    void testNestedObjects() {
        String yaml = """
            database:
              host: localhost
              port: 5432""";
        YamlWalker walker = YamlWalker.of(yaml);

        YamlLine dbLine = walker.findLine("database");
        assertThat(dbLine).isNotNull();
        assertThat(dbLine.getLineNumber()).isEqualTo(1);
        assertThat(dbLine.getType()).isEqualTo(YamlLineType.KEY_ONLY);

        YamlLine hostLine = walker.findLine("database.host");
        assertThat(hostLine).isNotNull();
        assertThat(hostLine.getLineNumber()).isEqualTo(2);
        assertThat(hostLine.getValue()).isEqualTo("localhost");

        YamlLine portLine = walker.findLine("database.port");
        assertThat(portLine).isNotNull();
        assertThat(portLine.getLineNumber()).isEqualTo(3);
        assertThat(portLine.getValue()).isEqualTo("5432");
    }

    @Test
    void testDeeplyNested() {
        String yaml = """
            level1:
              level2:
                level3:
                  value: deep""";
        YamlWalker walker = YamlWalker.of(yaml);

        YamlLine valueLine = walker.findLine("level1.level2.level3.value");
        assertThat(valueLine).isNotNull();
        assertThat(valueLine.getLineNumber()).isEqualTo(4);
        assertThat(valueLine.getValue()).isEqualTo("deep");
    }

    @Test
    void testSimpleList() {
        String yaml = """
            items:
              - first
              - second
              - third""";
        YamlWalker walker = YamlWalker.of(yaml);

        YamlLine item0 = walker.findLine("items[0]");
        assertThat(item0).isNotNull();
        assertThat(item0.getLineNumber()).isEqualTo(2);
        assertThat(item0.getValue()).isEqualTo("first");
        assertThat(item0.getListIndex()).isEqualTo(0);

        YamlLine item1 = walker.findLine("items[1]");
        assertThat(item1).isNotNull();
        assertThat(item1.getLineNumber()).isEqualTo(3);
        assertThat(item1.getValue()).isEqualTo("second");
        assertThat(item1.getListIndex()).isEqualTo(1);

        YamlLine item2 = walker.findLine("items[2]");
        assertThat(item2).isNotNull();
        assertThat(item2.getLineNumber()).isEqualTo(4);
        assertThat(item2.getValue()).isEqualTo("third");
        assertThat(item2.getListIndex()).isEqualTo(2);
    }

    @Test
    void testListOfObjects() {
        String yaml = """
            servers:
              - host: server1.com
                port: 8080
              - host: server2.com
                port: 9090""";
        YamlWalker walker = YamlWalker.of(yaml);

        YamlLine server0Host = walker.findLine("servers[0].host");
        assertThat(server0Host).isNotNull();
        assertThat(server0Host.getLineNumber()).isEqualTo(2);
        assertThat(server0Host.getValue()).isEqualTo("server1.com");

        YamlLine server0Port = walker.findLine("servers[0].port");
        assertThat(server0Port).isNotNull();
        assertThat(server0Port.getLineNumber()).isEqualTo(3);
        assertThat(server0Port.getValue()).isEqualTo("8080");

        YamlLine server1Host = walker.findLine("servers[1].host");
        assertThat(server1Host).isNotNull();
        assertThat(server1Host.getLineNumber()).isEqualTo(4);
        assertThat(server1Host.getValue()).isEqualTo("server2.com");

        YamlLine server1Port = walker.findLine("servers[1].port");
        assertThat(server1Port).isNotNull();
        assertThat(server1Port.getLineNumber()).isEqualTo(5);
        assertThat(server1Port.getValue()).isEqualTo("9090");
    }

    @Test
    void testCommentsAndBlanks() {
        String yaml = """
            # Header comment

            name: value
            # Inline comment""";
        YamlWalker walker = YamlWalker.of(yaml);

        assertThat(walker.getLines()).hasSize(4);
        assertThat(walker.getLines().get(0).getType()).isEqualTo(YamlLineType.COMMENT);
        assertThat(walker.getLines().get(1).getType()).isEqualTo(YamlLineType.BLANK);
        assertThat(walker.getLines().get(2).getType()).isEqualTo(YamlLineType.KEY_VALUE);
        assertThat(walker.getLines().get(3).getType()).isEqualTo(YamlLineType.COMMENT);
    }

    @Test
    void testMultilineValue() {
        String yaml = """
            description: |
              This is a
              multiline value
            name: test""";
        YamlWalker walker = YamlWalker.of(yaml);

        YamlLine descLine = walker.findLine("description");
        assertThat(descLine).isNotNull();
        assertThat(descLine.getLineNumber()).isEqualTo(1);
        assertThat(descLine.getValue()).isEqualTo("|");

        // Multiline content should be tracked
        assertThat(walker.getLines().get(1).getType()).isEqualTo(YamlLineType.MULTILINE_CONTENT);
        assertThat(walker.getLines().get(2).getType()).isEqualTo(YamlLineType.MULTILINE_CONTENT);

        YamlLine nameLine = walker.findLine("name");
        assertThat(nameLine).isNotNull();
        assertThat(nameLine.getLineNumber()).isEqualTo(4);
    }

    @Test
    void testValueColumn() {
        String yaml = """
            short: x
            longer_key: value""";
        YamlWalker walker = YamlWalker.of(yaml);

        YamlLine shortLine = walker.findLine("short");
        assertThat(shortLine.getValueColumn()).isEqualTo(7); // "short: x" -> x at index 7

        YamlLine longerLine = walker.findLine("longer_key");
        assertThat(longerLine.getValueColumn()).isEqualTo(12); // "longer_key: value" -> value at index 12
    }

    @Test
    void testMixedStructure() {
        String yaml = """
            app:
              name: MyApp
              servers:
                - host: primary.com
                  port: 80
                - host: secondary.com
                  port: 443
              settings:
                timeout: 30""";
        YamlWalker walker = YamlWalker.of(yaml);

        assertThat(walker.findLine("app")).isNotNull();
        assertThat(walker.findLine("app.name")).isNotNull();
        assertThat(walker.findLine("app.name").getValue()).isEqualTo("MyApp");

        assertThat(walker.findLine("app.servers[0].host")).isNotNull();
        assertThat(walker.findLine("app.servers[0].host").getValue()).isEqualTo("primary.com");

        assertThat(walker.findLine("app.servers[1].port")).isNotNull();
        assertThat(walker.findLine("app.servers[1].port").getValue()).isEqualTo("443");

        assertThat(walker.findLine("app.settings.timeout")).isNotNull();
        assertThat(walker.findLine("app.settings.timeout").getValue()).isEqualTo("30");
    }
}
