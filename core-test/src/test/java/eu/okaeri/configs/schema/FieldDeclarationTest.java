package eu.okaeri.configs.schema;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.configurer.InMemoryConfigurer;
import eu.okaeri.configs.serdes.SerdesContextAttachments;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for FieldDeclaration - focuses on the FieldDeclaration API itself.
 * Annotation-specific behavior is tested in annotation test classes.
 */
class FieldDeclarationTest {

    // === Test Configs ===

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class SimpleConfig extends OkaeriConfig {
        private String stringField = "default";
        private int intField = 42;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class AnnotatedConfig extends OkaeriConfig {
        @Comment("Test comment")
        private String commentedField = "value";
    }

    // === Tests ===

    @Test
    void testOf_CreatesFieldDeclaration() throws Exception {
        // Given
        SimpleConfig config = new SimpleConfig();
        ConfigDeclaration configDecl = ConfigDeclaration.of(config);
        Field javaField = SimpleConfig.class.getDeclaredField("stringField");

        // When
        FieldDeclaration fieldDecl = FieldDeclaration.of(configDecl, javaField, config);

        // Then
        assertThat(fieldDecl).isNotNull();
        assertThat(fieldDecl.getName()).isEqualTo("stringField");
        assertThat(fieldDecl.getField()).isEqualTo(javaField);
        assertThat(fieldDecl.getObject()).isEqualTo(config);
    }

    @Test
    void testOf_WithoutInstance_NullObject() throws Exception {
        // Given
        ConfigDeclaration configDecl = ConfigDeclaration.of(SimpleConfig.class);
        Field javaField = SimpleConfig.class.getDeclaredField("stringField");

        // When
        FieldDeclaration fieldDecl = FieldDeclaration.of(configDecl, javaField, null);

        // Then
        assertThat(fieldDecl.getObject()).isNull();
        assertThat(fieldDecl.getStartingValue()).isNull();
    }

    @Test
    void testCaching_SharesTemplate() throws Exception {
        // Given
        SimpleConfig config = new SimpleConfig();
        ConfigDeclaration configDecl = ConfigDeclaration.of(config);
        Field javaField = SimpleConfig.class.getDeclaredField("stringField");

        // When
        FieldDeclaration fieldDecl1 = FieldDeclaration.of(configDecl, javaField, config);
        FieldDeclaration fieldDecl2 = FieldDeclaration.of(configDecl, javaField, config);

        // Then - not same instance, but template data matches
        assertThat(fieldDecl1).isNotSameAs(fieldDecl2);
        assertThat(fieldDecl1.getName()).isEqualTo(fieldDecl2.getName());
        assertThat(fieldDecl1.getType()).isEqualTo(fieldDecl2.getType());
    }

    @Test
    void testGetValue_ReturnsCurrentFieldValue() throws Exception {
        // Given
        SimpleConfig config = new SimpleConfig();
        config.setStringField("modified");
        ConfigDeclaration configDecl = ConfigDeclaration.of(config);
        FieldDeclaration fieldDecl = configDecl.getField("stringField").get();

        // When
        Object value = fieldDecl.getValue();

        // Then
        assertThat(value).isEqualTo("modified");
    }

    @Test
    void testUpdateValue_SetsFieldValue() throws Exception {
        // Given
        SimpleConfig config = new SimpleConfig();
        ConfigDeclaration configDecl = ConfigDeclaration.of(config);
        FieldDeclaration fieldDecl = configDecl.getField("stringField").get();

        // When
        fieldDecl.updateValue("updated");

        // Then
        assertThat(config.getStringField()).isEqualTo("updated");
    }

    @Test
    void testGetAnnotation_ExistingAnnotation_ReturnsOptional() {
        // Given
        AnnotatedConfig config = new AnnotatedConfig();
        ConfigDeclaration configDecl = ConfigDeclaration.of(config);
        FieldDeclaration fieldDecl = configDecl.getField("commentedField").get();

        // When
        Optional<Comment> annotation = fieldDecl.getAnnotation(Comment.class);

        // Then
        assertThat(annotation).isPresent();
    }

    @Test
    void testGetAnnotation_NonExistingAnnotation_ReturnsEmpty() {
        // Given
        SimpleConfig config = new SimpleConfig();
        ConfigDeclaration configDecl = ConfigDeclaration.of(config);
        FieldDeclaration fieldDecl = configDecl.getField("stringField").get();

        // When
        Optional<Comment> annotation = fieldDecl.getAnnotation(Comment.class);

        // Then
        assertThat(annotation).isEmpty();
    }

    @Test
    void testReadStaticAnnotations_ReturnsAttachments() {
        // Given
        AnnotatedConfig config = new AnnotatedConfig();
        ConfigDeclaration configDecl = ConfigDeclaration.of(config);
        FieldDeclaration fieldDecl = configDecl.getField("commentedField").get();
        InMemoryConfigurer configurer = new InMemoryConfigurer();

        // When
        SerdesContextAttachments attachments = fieldDecl.readStaticAnnotations(configurer);

        // Then
        assertThat(attachments).isNotNull();
    }

    @Test
    void testStartingValue_WithInstance_CapturesValue() {
        // Given
        SimpleConfig config = new SimpleConfig();
        config.setStringField("custom starting value");
        ConfigDeclaration configDecl = ConfigDeclaration.of(config);

        // When
        FieldDeclaration fieldDecl = configDecl.getField("stringField").get();

        // Then
        assertThat(fieldDecl.getStartingValue()).isEqualTo("custom starting value");
    }

    @Test
    void testStartingValue_WithoutInstance_ReturnsNull() {
        // Given
        ConfigDeclaration configDecl = ConfigDeclaration.of(SimpleConfig.class);

        // When
        FieldDeclaration fieldDecl = configDecl.getField("stringField").get();

        // Then
        assertThat(fieldDecl.getStartingValue()).isNull();
    }

}
