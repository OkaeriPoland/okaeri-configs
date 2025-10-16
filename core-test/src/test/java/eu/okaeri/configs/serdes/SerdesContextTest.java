package eu.okaeri.configs.serdes;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.Header;
import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.schema.FieldDeclaration;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for SerdesContext - context information for serializers and transformers.
 */
class SerdesContextTest {

    private Configurer configurer;

    @BeforeEach
    void setUp() {
        this.configurer = new YamlSnakeYamlConfigurer();
    }

    // === FACTORY METHOD TESTS ===

    @Test
    void testOf_ConfigurerOnly_CreatesContext() {
        SerdesContext context = SerdesContext.of(this.configurer);

        assertThat(context.getConfigurer()).isSameAs(this.configurer);
        assertThat(context.getField()).isNull();
        assertThat(context.getAttachments()).isNotNull();
    }

    @Test
    void testOf_ConfigurerAndField_CreatesContext() {
        TestConfig config = ConfigManager.create(TestConfig.class);
        ConfigDeclaration declaration = ConfigDeclaration.of(config);
        FieldDeclaration field = declaration.getField("testField").get();

        SerdesContext context = SerdesContext.of(this.configurer, field);

        assertThat(context.getConfigurer()).isSameAs(this.configurer);
        assertThat(context.getField()).isSameAs(field);
    }

    @Test
    void testOf_ConfigurerFieldAndAttachments_CreatesContext() {
        TestConfig config = ConfigManager.create(TestConfig.class);
        ConfigDeclaration declaration = ConfigDeclaration.of(config);
        FieldDeclaration field = declaration.getField("testField").get();
        SerdesContextAttachments attachments = new SerdesContextAttachments();

        SerdesContext context = SerdesContext.of(this.configurer, field, attachments);

        assertThat(context.getConfigurer()).isSameAs(this.configurer);
        assertThat(context.getField()).isSameAs(field);
        assertThat(context.getAttachments()).isSameAs(attachments);
    }

    @Test
    void testOf_NullField_CreatesContextWithoutField() {
        SerdesContext context = SerdesContext.of(this.configurer, null);

        assertThat(context.getConfigurer()).isSameAs(this.configurer);
        assertThat(context.getField()).isNull();
    }

    // === BUILDER TESTS (SKIPPED - Builder is private) ===
    // Note: SerdesContext.Builder is a private inner class, so we cannot test it directly.
    // Builder functionality is tested indirectly through factory methods above.

    // === CONFIG ANNOTATION TESTS ===

    @Test
    void testGetConfigAnnotation_AnnotationPresent_ReturnsAnnotation() {
        AnnotatedConfig config = ConfigManager.create(AnnotatedConfig.class);
        config.withConfigurer(this.configurer);
        this.configurer.setParent(config);

        SerdesContext context = SerdesContext.of(this.configurer);

        Optional<Header> header = context.getConfigAnnotation(Header.class);
        assertThat(header).isPresent();
        assertThat(header.get().value()).containsExactly("Test Header");
    }

    @Test
    void testGetConfigAnnotation_AnnotationAbsent_ReturnsEmpty() {
        AnnotatedConfig config = ConfigManager.create(AnnotatedConfig.class);
        config.withConfigurer(this.configurer);
        this.configurer.setParent(config);

        SerdesContext context = SerdesContext.of(this.configurer);

        // Deprecated is not present on the config
        Optional<Deprecated> deprecated = context.getConfigAnnotation(Deprecated.class);
        assertThat(deprecated).isEmpty();
    }

    @Test
    void testGetConfigAnnotation_NoParent_ReturnsEmpty() {
        // Configurer has no parent
        SerdesContext context = SerdesContext.of(this.configurer);

        Optional<Header> header = context.getConfigAnnotation(Header.class);
        assertThat(header).isEmpty();
    }

    // === FIELD ANNOTATION TESTS ===

    @Test
    void testGetFieldAnnotation_AnnotationPresent_ReturnsAnnotation() {
        AnnotatedConfig config = ConfigManager.create(AnnotatedConfig.class);
        ConfigDeclaration declaration = ConfigDeclaration.of(config);
        FieldDeclaration field = declaration.getField("commentedField").get();

        SerdesContext context = SerdesContext.of(this.configurer, field);

        Optional<Comment> comment = context.getFieldAnnotation(Comment.class);
        assertThat(comment).isPresent();
        assertThat(comment.get().value()).containsExactly("Field comment");
    }

    @Test
    void testGetFieldAnnotation_AnnotationAbsent_ReturnsEmpty() {
        AnnotatedConfig config = ConfigManager.create(AnnotatedConfig.class);
        ConfigDeclaration declaration = ConfigDeclaration.of(config);
        FieldDeclaration field = declaration.getField("testField").get();

        SerdesContext context = SerdesContext.of(this.configurer, field);

        // Comment is not present on testField
        Optional<Comment> comment = context.getFieldAnnotation(Comment.class);
        assertThat(comment).isEmpty();
    }

    @Test
    void testGetFieldAnnotation_NoField_ReturnsEmpty() {
        SerdesContext context = SerdesContext.of(this.configurer, null);

        Optional<Comment> comment = context.getFieldAnnotation(Comment.class);
        assertThat(comment).isEmpty();
    }

    // === ATTACHMENT TESTS ===

    @Test
    void testGetAttachment_AttachmentPresent_ReturnsAttachment() {
        TestAttachment attachment = new TestAttachment("test data");
        SerdesContextAttachments attachments = new SerdesContextAttachments();
        attachments.put(TestAttachment.class, attachment);

        SerdesContext context = SerdesContext.of(this.configurer, null, attachments);

        Optional<TestAttachment> retrieved = context.getAttachment(TestAttachment.class);
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get()).isSameAs(attachment);
    }

    @Test
    void testGetAttachment_AttachmentAbsent_ReturnsEmpty() {
        SerdesContext context = SerdesContext.of(this.configurer);

        Optional<TestAttachment> retrieved = context.getAttachment(TestAttachment.class);
        assertThat(retrieved).isEmpty();
    }

    @Test
    void testGetAttachment_WithDefault_AttachmentPresent_ReturnsAttachment() {
        TestAttachment attachment = new TestAttachment("actual");
        TestAttachment defaultAttachment = new TestAttachment("default");
        SerdesContextAttachments attachments = new SerdesContextAttachments();
        attachments.put(TestAttachment.class, attachment);

        SerdesContext context = SerdesContext.of(this.configurer, null, attachments);

        TestAttachment retrieved = context.getAttachment(TestAttachment.class, defaultAttachment);
        assertThat(retrieved).isSameAs(attachment);
    }

    @Test
    void testGetAttachment_WithDefault_AttachmentAbsent_ReturnsDefault() {
        TestAttachment defaultAttachment = new TestAttachment("default");
        SerdesContext context = SerdesContext.of(this.configurer);

        TestAttachment retrieved = context.getAttachment(TestAttachment.class, defaultAttachment);
        assertThat(retrieved).isSameAs(defaultAttachment);
    }

    // === COMPLEX SCENARIOS TESTS ===

    @Test
    void testFullContext_AllFieldsPopulated() {
        AnnotatedConfig config = ConfigManager.create(AnnotatedConfig.class);
        config.withConfigurer(this.configurer);
        this.configurer.setParent(config);

        ConfigDeclaration declaration = ConfigDeclaration.of(config);
        FieldDeclaration field = declaration.getField("commentedField").get();

        TestAttachment attachment = new TestAttachment("data");
        SerdesContextAttachments attachments = new SerdesContextAttachments();
        attachments.put(TestAttachment.class, attachment);

        SerdesContext context = SerdesContext.of(this.configurer, field, attachments);

        // Verify all components
        assertThat(context.getConfigurer()).isSameAs(this.configurer);
        assertThat(context.getField()).isSameAs(field);
        assertThat(context.getConfigAnnotation(Header.class)).isPresent();
        assertThat(context.getFieldAnnotation(Comment.class)).isPresent();
        assertThat(context.getAttachment(TestAttachment.class)).isPresent();
    }

    @Test
    void testGetConfigurer_ReturnsConfigurer() {
        SerdesContext context = SerdesContext.of(this.configurer);

        assertThat(context.getConfigurer()).isSameAs(this.configurer);
    }

    @Test
    void testGetField_WithField_ReturnsField() {
        TestConfig config = ConfigManager.create(TestConfig.class);
        ConfigDeclaration declaration = ConfigDeclaration.of(config);
        FieldDeclaration field = declaration.getField("testField").get();

        SerdesContext context = SerdesContext.of(this.configurer, field);

        assertThat(context.getField()).isSameAs(field);
    }

    @Test
    void testGetField_WithoutField_ReturnsNull() {
        SerdesContext context = SerdesContext.of(this.configurer);

        assertThat(context.getField()).isNull();
    }

    // === TEST CONFIGS ===

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class TestConfig extends OkaeriConfig {
        private String testField = "value";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @Header("Test Header")
    public static class AnnotatedConfig extends OkaeriConfig {
        private String testField = "value";

        @Comment("Field comment")
        private String commentedField = "commented";
    }

    // === TEST ATTACHMENT ===

    public static class TestAttachment implements SerdesContextAttachment {
        private final String data;

        public TestAttachment(String data) {
            this.data = data;
        }

        public String getData() {
            return this.data;
        }
    }
}
