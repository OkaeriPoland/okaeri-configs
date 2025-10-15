package eu.okaeri.configs.annotations;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.Comments;
import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.schema.FieldDeclaration;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for @Comment and @Comments annotations.
 * 
 * Verifies:
 * - Single @Comment with one line
 * - Single @Comment with multiple lines
 * - @Comments with multiple @Comment annotations
 * - Comment is included in field declaration
 * - Repeating @Comment on same field
 * - No comment when annotation absent
 * 
 * Note: YAML comment formatting tests (# prefix, positioning) are in yaml-snakeyaml module.
 */
class CommentAnnotationTest {

    // Test configs

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class SingleLineCommentConfig extends OkaeriConfig {
        @Comment("This is a single line comment")
        private String commentedField = "value1";
        
        private String uncommentedField = "value2";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class MultiLineCommentConfig extends OkaeriConfig {
        @Comment({"Line 1 of comment", "Line 2 of comment", "Line 3 of comment"})
        private String multiLineField = "value";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class MultipleCommentsConfig extends OkaeriConfig {
        @Comments({
            @Comment("First comment group"),
            @Comment("Second comment group"),
            @Comment({"Third comment - Line 1", "Third comment - Line 2"})
        })
        private String multiCommentField = "value";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class MixedCommentsConfig extends OkaeriConfig {
        @Comment("Comment on field 1")
        private String field1 = "value1";
        
        private String field2 = "value2";
        
        @Comment({"Multi-line comment", "on field 3"})
        private String field3 = "value3";
    }

    // Tests

    @Test
    void testComment_SingleLine_InDeclaration() {
        // Given
        SingleLineCommentConfig config = ConfigManager.create(SingleLineCommentConfig.class);

        // When
        ConfigDeclaration declaration = config.getDeclaration();
        FieldDeclaration field = declaration.getField("commentedField").orElse(null);

        // Then
        assertThat(field).isNotNull();
        assertThat(field.getComment()).isNotNull();
        assertThat(field.getComment()).containsExactly("This is a single line comment");
    }

    @Test
    void testComment_MultiLine_InDeclaration() {
        // Given
        MultiLineCommentConfig config = ConfigManager.create(MultiLineCommentConfig.class);

        // When
        ConfigDeclaration declaration = config.getDeclaration();
        FieldDeclaration field = declaration.getField("multiLineField").orElse(null);

        // Then
        assertThat(field).isNotNull();
        assertThat(field.getComment()).isNotNull();
        assertThat(field.getComment()).containsExactly(
            "Line 1 of comment",
            "Line 2 of comment",
            "Line 3 of comment"
        );
    }

    @Test
    void testComments_MultipleAnnotations_InDeclaration() {
        // Given
        MultipleCommentsConfig config = ConfigManager.create(MultipleCommentsConfig.class);

        // When
        ConfigDeclaration declaration = config.getDeclaration();
        FieldDeclaration field = declaration.getField("multiCommentField").orElse(null);

        // Then
        assertThat(field).isNotNull();
        assertThat(field.getComment()).isNotNull();
        assertThat(field.getComment()).containsExactly(
            "First comment group",
            "Second comment group",
            "Third comment - Line 1",
            "Third comment - Line 2"
        );
    }

    @Test
    void testComment_NoAnnotation_NullInDeclaration() {
        // Given
        SingleLineCommentConfig config = ConfigManager.create(SingleLineCommentConfig.class);

        // When
        ConfigDeclaration declaration = config.getDeclaration();
        FieldDeclaration field = declaration.getField("uncommentedField").orElse(null);

        // Then
        assertThat(field).isNotNull();
        assertThat(field.getComment()).isNull();
    }

    @Test
    void testComment_MixedFields_AllDeclarationsCorrect() {
        // Given
        MixedCommentsConfig config = ConfigManager.create(MixedCommentsConfig.class);

        // When
        ConfigDeclaration declaration = config.getDeclaration();

        // Then
        FieldDeclaration field1 = declaration.getField("field1").orElse(null);
        assertThat(field1).isNotNull();
        assertThat(field1.getComment()).containsExactly("Comment on field 1");

        FieldDeclaration field2 = declaration.getField("field2").orElse(null);
        assertThat(field2).isNotNull();
        assertThat(field2.getComment()).isNull();

        FieldDeclaration field3 = declaration.getField("field3").orElse(null);
        assertThat(field3).isNotNull();
        assertThat(field3.getComment()).containsExactly("Multi-line comment", "on field 3");
    }

    @Test
    void testComment_EmptyValue_HandledCorrectly() {
        // Given
        @Data
        @EqualsAndHashCode(callSuper = false)
        class EmptyCommentConfig extends OkaeriConfig {
            @Comment
            private String field = "value";
        }

        // When
        EmptyCommentConfig config = ConfigManager.create(EmptyCommentConfig.class);
        ConfigDeclaration declaration = config.getDeclaration();
        FieldDeclaration field = declaration.getField("field").orElse(null);

        // Then
        assertThat(field).isNotNull();
        assertThat(field.getComment()).isNotNull();
        assertThat(field.getComment()).containsExactly("");
    }

    @Test
    void testComment_DeclarationCaching_WorksCorrectly() {
        // Given
        SingleLineCommentConfig config1 = ConfigManager.create(SingleLineCommentConfig.class);
        SingleLineCommentConfig config2 = ConfigManager.create(SingleLineCommentConfig.class);

        // When
        FieldDeclaration field1 = config1.getDeclaration().getField("commentedField").orElse(null);
        FieldDeclaration field2 = config2.getDeclaration().getField("commentedField").orElse(null);

        // Then - Both should have the same comment from cached template
        assertThat(field1).isNotNull();
        assertThat(field1.getComment()).containsExactly("This is a single line comment");
        assertThat(field2).isNotNull();
        assertThat(field2.getComment()).containsExactly("This is a single line comment");
    }
}
