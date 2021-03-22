package eu.okaeri.configs.schema;

import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.Comments;
import eu.okaeri.configs.annotation.CustomKey;
import eu.okaeri.configs.annotation.Exclude;
import lombok.Data;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Data
public class FieldDeclaration {

    public static FieldDeclaration from(Field field, Object object) {

        FieldDeclaration declaration = new FieldDeclaration();
        boolean accessible = field.isAccessible();
        field.setAccessible(true);

        if (field.getAnnotation(Exclude.class) != null) {
            return null;
        }

        CustomKey customKey = field.getAnnotation(CustomKey.class);
        declaration.setName((((customKey == null) || "".equals(customKey.value())) ? field.getName() : customKey.value()));

        declaration.setComment(readComments(field));
        declaration.setField(field);
        declaration.setObject(object);
        declaration.setType(GenericsDeclaration.from(field.getGenericType()));
        field.setAccessible(accessible);

        return declaration;
    }

    private static String[] readComments(Field field) {

        Comments comments = field.getAnnotation(Comments.class);
        if (comments != null) {
            List<String> commentList = new ArrayList<>();
            for (Comment comment : comments.value()) {
                commentList.addAll(Arrays.asList(comment.value()));
            }
            return commentList.toArray(new String[0]);
        }

        Comment comment = field.getAnnotation(Comment.class);
        if (comment != null) {
            return comment.value();
        }

        return null;
    }

    public void updateValue(Object value) throws IllegalAccessException {
        boolean accessible = this.field.isAccessible();
        this.field.setAccessible(true);
        this.field.set(this.object, value);
        this.field.setAccessible(accessible);
    }

    public Object getValue() throws IllegalAccessException {
        boolean accessible = this.field.isAccessible();
        this.field.setAccessible(true);
        Object value = this.field.get(this.object);
        this.field.setAccessible(accessible);
        return value;
    }

    private String name;
    private String[] comment;
    private GenericsDeclaration type;

    private Field field;
    private Object object;
}
