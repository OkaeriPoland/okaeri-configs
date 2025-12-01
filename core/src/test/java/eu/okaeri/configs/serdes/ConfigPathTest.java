package eu.okaeri.configs.serdes;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigPathTest {

    @Test
    void testRoot() {
        ConfigPath path = ConfigPath.root();
        assertThat(path.isEmpty()).isTrue();
        assertThat(path.size()).isZero();
        assertThat(path.toString()).isEqualTo("<root>");
    }

    @Test
    void testSimpleProperty() {
        ConfigPath path = ConfigPath.of("database");
        assertThat(path.isEmpty()).isFalse();
        assertThat(path.size()).isEqualTo(1);
        assertThat(path.toString()).isEqualTo("database");
    }

    @Test
    void testNestedProperty() {
        ConfigPath path = ConfigPath.of("database").property("host");
        assertThat(path.size()).isEqualTo(2);
        assertThat(path.toString()).isEqualTo("database.host");
    }

    @Test
    void testDeeplyNestedProperty() {
        ConfigPath path = ConfigPath.of("app")
            .property("database")
            .property("primary")
            .property("host");
        assertThat(path.toString()).isEqualTo("app.database.primary.host");
    }

    @Test
    void testListIndex() {
        ConfigPath path = ConfigPath.of("servers").index(0);
        assertThat(path.toString()).isEqualTo("servers[0]");
    }

    @Test
    void testListIndexWithProperty() {
        ConfigPath path = ConfigPath.of("servers").index(0).property("name");
        assertThat(path.toString()).isEqualTo("servers[0].name");
    }

    @Test
    void testMapKeyString() {
        ConfigPath path = ConfigPath.of("settings").key("api-key");
        assertThat(path.toString()).isEqualTo("settings[\"api-key\"]");
    }

    @Test
    void testMapKeyInteger() {
        ConfigPath path = ConfigPath.of("limits").key(100);
        assertThat(path.toString()).isEqualTo("limits[100]");
    }

    @Test
    void testComplexPath() {
        // Example: config.servers[0].endpoints["api"].limits[2].maxRequests
        ConfigPath path = ConfigPath.of("config")
            .property("servers")
            .index(0)
            .property("endpoints")
            .key("api")
            .property("limits")
            .index(2)
            .property("maxRequests");
        assertThat(path.toString()).isEqualTo("config.servers[0].endpoints[\"api\"].limits[2].maxRequests");
    }

    @Test
    void testParent() {
        ConfigPath path = ConfigPath.of("database").property("host");
        ConfigPath parent = path.parent();
        assertThat(parent.toString()).isEqualTo("database");
    }

    @Test
    void testParentOfSingleNode() {
        ConfigPath path = ConfigPath.of("database");
        ConfigPath parent = path.parent();
        assertThat(parent.isEmpty()).isTrue();
    }

    @Test
    void testParentOfRoot() {
        ConfigPath path = ConfigPath.root();
        ConfigPath parent = path.parent();
        assertThat(parent.isEmpty()).isTrue();
    }

    @Test
    void testLastNode() {
        ConfigPath path = ConfigPath.of("database").property("host");
        ConfigPath.PathNode lastNode = path.getLastNode();
        assertThat(lastNode).isInstanceOf(ConfigPath.PropertyNode.class);
        assertThat(((ConfigPath.PropertyNode) lastNode).getName()).isEqualTo("host");
    }

    @Test
    void testLastNodeOfRoot() {
        ConfigPath path = ConfigPath.root();
        assertThat(path.getLastNode()).isNull();
    }

    @Test
    void testImmutability() {
        ConfigPath original = ConfigPath.of("database");
        ConfigPath extended = original.property("host");

        // Original should not be modified
        assertThat(original.size()).isEqualTo(1);
        assertThat(original.toString()).isEqualTo("database");

        // Extended should have both
        assertThat(extended.size()).isEqualTo(2);
        assertThat(extended.toString()).isEqualTo("database.host");
    }

    @Test
    void testEscapingInMapKey() {
        ConfigPath path = ConfigPath.of("settings").key("key\"with\"quotes");
        assertThat(path.toString()).isEqualTo("settings[\"key\\\"with\\\"quotes\"]");
    }

    @Test
    void testEscapingBackslashInMapKey() {
        ConfigPath path = ConfigPath.of("settings").key("path\\to\\file");
        assertThat(path.toString()).isEqualTo("settings[\"path\\\\to\\\\file\"]");
    }
}
