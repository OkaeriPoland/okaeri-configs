package eu.okaeri.configs.configurer;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.serdes.ConfigPath;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for WrappedConfigurer to ensure proper isolation of instance state.
 */
class WrappedConfigurerTest {

    /**
     * Verifies that basePath is NOT shared between parent and child configurers.
     * This was a bug where @Delegate forwarded setBasePath to the parent.
     */
    @Test
    void testBasePath_NotSharedBetweenParentAndChild() {
        // Given: A parent configurer with a basePath
        TestConfigurer parentConfigurer = new TestConfigurer();
        parentConfigurer.setBasePath(ConfigPath.of("parent"));

        // When: Creating a child wrapped configurer and setting its basePath
        Map<String, Object> childData = new LinkedHashMap<>();
        InMemoryWrappedConfigurer childConfigurer = new InMemoryWrappedConfigurer(parentConfigurer, childData);
        childConfigurer.setBasePath(ConfigPath.of("parent").property("child"));

        // Then: Parent's basePath should be unchanged
        assertThat(parentConfigurer.getBasePath().toString()).isEqualTo("parent");
        // And: Child's basePath should be set correctly
        assertThat(childConfigurer.getBasePath().toString()).isEqualTo("parent.child");
    }

    /**
     * Verifies that parent (OkaeriConfig reference) is NOT shared between configurers.
     */
    @Test
    void testParent_NotSharedBetweenParentAndChild() {
        // Given: A parent configurer with a parent config
        TestConfigurer parentConfigurer = new TestConfigurer();
        OkaeriConfig parentConfig = new TestConfig();
        parentConfigurer.setParent(parentConfig);

        // When: Creating a child wrapped configurer and setting its parent
        Map<String, Object> childData = new LinkedHashMap<>();
        InMemoryWrappedConfigurer childConfigurer = new InMemoryWrappedConfigurer(parentConfigurer, childData);
        OkaeriConfig childConfig = new TestConfig();
        childConfigurer.setParent(childConfig);

        // Then: Parent configurer should still reference the original parent
        assertThat(parentConfigurer.getParent()).isSameAs(parentConfig);
        // And: Child configurer should reference the child config
        assertThat(childConfigurer.getParent()).isSameAs(childConfig);
    }

    /**
     * Verifies that rawContent is NOT shared between configurers.
     */
    @Test
    void testRawContent_NotSharedBetweenParentAndChild() {
        // Given: A parent configurer with raw content
        TestConfigurer parentConfigurer = new TestConfigurer();
        parentConfigurer.setRawContent("parent content");

        // When: Creating a child wrapped configurer and setting its raw content
        Map<String, Object> childData = new LinkedHashMap<>();
        InMemoryWrappedConfigurer childConfigurer = new InMemoryWrappedConfigurer(parentConfigurer, childData);
        childConfigurer.setRawContent("child content");

        // Then: Parent's raw content should be unchanged
        assertThat(parentConfigurer.getRawContent()).isEqualTo("parent content");
        // And: Child's raw content should be set correctly
        assertThat(childConfigurer.getRawContent()).isEqualTo("child content");
    }

    /**
     * Verifies that rawContent set on a WrappedConfigurer can be retrieved via getRawContent().
     * This tests for field shadowing bugs where setter and getter access different fields.
     */
    @Test
    void testRawContent_SetAndGetOnWrappedConfigurer() {
        // Given: A wrapped configurer
        TestConfigurer innerConfigurer = new TestConfigurer();
        WrappedConfigurer wrappedConfigurer = new WrappedConfigurer(innerConfigurer);

        // When: Setting raw content on the wrapped configurer
        wrappedConfigurer.setRawContent("test content");

        // Then: getRawContent should return the same content
        // This fails if there's a field shadowing bug (setter sets one field, getter reads another)
        assertThat(wrappedConfigurer.getRawContent()).isEqualTo("test content");
    }

    /**
     * Verifies that a child WrappedConfigurer can access parent's rawContent when its own is null.
     * This is needed for subconfigs to access the root config's YAML content for error reporting.
     */
    @Test
    void testRawContent_ChildCanAccessParentRawContentWhenOwnIsNull() {
        // Given: A parent configurer with raw content
        TestConfigurer parentConfigurer = new TestConfigurer();
        parentConfigurer.setRawContent("parent yaml content");

        // When: Creating a child wrapped configurer WITHOUT setting its raw content
        Map<String, Object> childData = new LinkedHashMap<>();
        InMemoryWrappedConfigurer childConfigurer = new InMemoryWrappedConfigurer(parentConfigurer, childData);
        // Note: We intentionally don't call childConfigurer.setRawContent()

        // Then: Child should be able to access parent's raw content for error reporting
        // This is the key scenario for error reporting: subconfigs need root's rawContent
        assertThat(childConfigurer.getRawContent()).isEqualTo("parent yaml content");
    }

    /**
     * Verifies rawContent propagation through double-wrapped configurers.
     * This mimics setups where a validator wraps a configurer (e.g., OkaeriValidator wraps YamlBukkitConfigurer),
     * then InMemoryWrappedConfigurer wraps OkaeriValidator for subconfigs.
     */
    @Test
    void testRawContent_DoubleWrappedConfigurer() {
        // Given: A base configurer wrapped in another WrappedConfigurer (like OkaeriValidator)
        TestConfigurer baseConfigurer = new TestConfigurer();
        WrappedConfigurer validator = new WrappedConfigurer(baseConfigurer);
        validator.setRawContent("yaml content from file");

        // When: Creating a subconfig's configurer that wraps the validator
        Map<String, Object> childData = new LinkedHashMap<>();
        InMemoryWrappedConfigurer subconfigConfigurer = new InMemoryWrappedConfigurer(validator, childData);

        // Then: The subconfig should be able to access the rawContent through the chain
        assertThat(subconfigConfigurer.getRawContent()).isEqualTo("yaml content from file");
    }

    /**
     * Verifies that setRawContent propagates to the inner wrapped configurer.
     * This is critical because format-specific methods like createSourceWalker() are delegated
     * to the wrapped configurer, and they call getRawContent() on themselves (not on the wrapper).
     */
    @Test
    void testRawContent_PropagatedToInnerConfigurer() {
        // Given: A base configurer wrapped in a WrappedConfigurer
        TestConfigurer baseConfigurer = new TestConfigurer();
        WrappedConfigurer validator = new WrappedConfigurer(baseConfigurer);

        // When: Setting rawContent on the outer wrapper
        validator.setRawContent("yaml content");

        // Then: The inner configurer should also have the rawContent
        // This is critical for createSourceWalker() which is delegated and calls this.getRawContent()
        assertThat(baseConfigurer.getRawContent()).isEqualTo("yaml content");
    }

    /**
     * Verifies that registry IS shared between parent and child configurers.
     * This is intentional - serializers should be the same across all configs.
     */
    @Test
    void testRegistry_SharedBetweenParentAndChild() {
        // Given: A parent configurer with a registry
        TestConfigurer parentConfigurer = new TestConfigurer();

        // When: Creating a child wrapped configurer
        Map<String, Object> childData = new LinkedHashMap<>();
        InMemoryWrappedConfigurer childConfigurer = new InMemoryWrappedConfigurer(parentConfigurer, childData);

        // Then: Both should share the same registry
        assertThat(childConfigurer.getRegistry()).isSameAs(parentConfigurer.getRegistry());
    }

    /**
     * Verifies that deeply nested configurers maintain correct isolated paths.
     * This tests the scenario that caused the original bug: paths accumulating
     * across multiple levels of nesting.
     */
    @Test
    void testDeepNesting_PathsRemainIsolated() {
        // Given: A chain of nested configurers
        TestConfigurer root = new TestConfigurer();
        root.setBasePath(ConfigPath.root());

        InMemoryWrappedConfigurer level1 = new InMemoryWrappedConfigurer(root, new LinkedHashMap<>());
        level1.setBasePath(ConfigPath.of("level1"));

        InMemoryWrappedConfigurer level2 = new InMemoryWrappedConfigurer(level1, new LinkedHashMap<>());
        level2.setBasePath(ConfigPath.of("level1").property("level2"));

        InMemoryWrappedConfigurer level3 = new InMemoryWrappedConfigurer(level2, new LinkedHashMap<>());
        level3.setBasePath(ConfigPath.of("level1").property("level2").property("level3"));

        // Then: Each configurer should have its own isolated path
        assertThat(root.getBasePath().toString()).isEqualTo("<root>");
        assertThat(level1.getBasePath().toString()).isEqualTo("level1");
        assertThat(level2.getBasePath().toString()).isEqualTo("level1.level2");
        assertThat(level3.getBasePath().toString()).isEqualTo("level1.level2.level3");
    }

    // Test helpers

    private static class TestConfig extends OkaeriConfig {
    }

    private static class TestConfigurer extends Configurer {
        private final Map<String, Object> data = new LinkedHashMap<>();

        @Override
        public void setValue(String key, Object value, eu.okaeri.configs.schema.GenericsDeclaration type, eu.okaeri.configs.schema.FieldDeclaration field) {
            this.data.put(key, value);
        }

        @Override
        public void setValueUnsafe(String key, Object value) {
            this.data.put(key, value);
        }

        @Override
        public Object getValue(String key) {
            return this.data.get(key);
        }

        @Override
        public Object remove(String key) {
            return this.data.remove(key);
        }

        @Override
        public void write(java.io.OutputStream outputStream, eu.okaeri.configs.schema.ConfigDeclaration declaration) {
        }

        @Override
        public void load(java.io.InputStream inputStream, eu.okaeri.configs.schema.ConfigDeclaration declaration) {
        }
    }
}
