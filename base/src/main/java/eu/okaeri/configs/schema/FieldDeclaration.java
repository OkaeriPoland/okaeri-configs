package eu.okaeri.configs.schema;

import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.CustomKey;
import lombok.Data;

import java.lang.reflect.Field;

@Data
public class FieldDeclaration {

    public static FieldDeclaration from(Field field, Object object) {

        FieldDeclaration declaration = new FieldDeclaration();
        boolean accessible = field.isAccessible();
        field.setAccessible(true);

        CustomKey annotation = field.getAnnotation(CustomKey.class);
        declaration.setName((((annotation == null) || "".equals(annotation.value())) ? field.getName() : annotation.value()));

        Comment comment = field.getAnnotation(Comment.class);
        declaration.setComment(comment.value());

        declaration.setField(field);
        declaration.setObject(object);
        field.setAccessible(accessible);

        return declaration;
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
    private Class<?> type;

    private Field field;
    private Object object;
}
