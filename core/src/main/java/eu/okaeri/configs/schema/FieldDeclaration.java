package eu.okaeri.configs.schema;

import eu.okaeri.configs.annotation.*;
import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.exception.OkaeriException;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.serdes.SerdesAnnotationResolver;
import eu.okaeri.configs.serdes.SerdesContextAttachment;
import eu.okaeri.configs.serdes.SerdesContextAttachments;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

@Data
public class FieldDeclaration {

    private static final Logger LOGGER = Logger.getLogger(FieldDeclaration.class.getSimpleName());
    private static final Map<CacheEntry, FieldDeclaration> DECLARATION_CACHE = new ConcurrentHashMap<>();
    private static final Set<String> FINAL_WARNS = ConcurrentHashMap.newKeySet();

    private Object startingValue;
    private String name;
    private String[] comment;
    private GenericsDeclaration type;
    private Variable variable;
    private boolean variableHide;
    private Field field;
    private Object object;
    private ObjectSerializer<?> customSerializer;

    @SneakyThrows
    public static FieldDeclaration of(@NonNull ConfigDeclaration config, @NonNull Field field, Object object) {

        CacheEntry cache = new CacheEntry(config.getType(), field.getName());
        FieldDeclaration template = DECLARATION_CACHE.computeIfAbsent(cache, (entry) -> {

            FieldDeclaration declaration = new FieldDeclaration();

            // Try to make field accessible - return null if not possible (e.g., java.base module fields)
            try {
                field.setAccessible(true);
            } catch (Exception exception) {
                return null;
            }

            if (field.getAnnotation(Exclude.class) != null) {
                return null;
            }

            if (Modifier.isTransient(field.getModifiers())) {
                return null;
            }

            if (Modifier.isStatic(field.getModifiers())) {
                return null;
            }

            if ("serialVersionUID".equals(field.getName())) {
                return null;
            }

            CustomKey customKey = field.getAnnotation(CustomKey.class);
            if (customKey != null) {
                declaration.setName("".equals(customKey.value()) ? field.getName() : customKey.value());
            } else if (config.getNameStrategy() != null) {

                Names nameStrategy = config.getNameStrategy();
                NameStrategy strategy = nameStrategy.strategy();
                NameModifier modifier = nameStrategy.modifier();

                String name = strategy.getRegex().matcher(field.getName()).replaceAll(strategy.getReplacement());
                if (modifier == NameModifier.TO_UPPER_CASE) {
                    name = name.toUpperCase(Locale.ROOT);
                } else if (modifier == NameModifier.TO_LOWER_CASE) {
                    name = name.toLowerCase(Locale.ROOT);
                }

                declaration.setName(name);
            } else {
                declaration.setName(field.getName());
            }

            Variable variable = field.getAnnotation(Variable.class);
            declaration.setVariable(variable);
            declaration.setComment(readComments(field));
            declaration.setType(GenericsDeclaration.of(field.getGenericType()));
            declaration.setField(field);

            Serdes serdesAnnotation = field.getAnnotation(Serdes.class);
            if (serdesAnnotation != null) {
                try {
                    ObjectSerializer<?> serializer = serdesAnnotation.serializer().newInstance();
                    Class<?> fieldType = declaration.getType().getType();
                    if (!serializer.supports(fieldType)) {
                        throw new OkaeriException("Serializer " + serdesAnnotation.serializer().getName() +
                            " does not support field type " + fieldType.getName() +
                            " for field " + field.getName());
                    }
                    declaration.setCustomSerializer(serializer);
                } catch (InstantiationException | IllegalAccessException exception) {
                    throw new OkaeriException("Failed to instantiate serializer " +
                        serdesAnnotation.serializer().getName() + " for field " + field.getName() +
                        ". Ensure it has a public no-args constructor.", exception);
                }
            }

            return declaration;
        });

        if (template == null) {
            return null;
        }

        FieldDeclaration declaration = new FieldDeclaration();
        Object startingValue = (object == null) ? null : template.getField().get(object);
        declaration.setStartingValue(startingValue);

        declaration.setName(template.getName());
        declaration.setComment(template.getComment());
        declaration.setType(template.getType());
        declaration.setVariable(template.getVariable());
        declaration.setField(template.getField());
        declaration.setObject(object);
        declaration.setCustomSerializer(template.getCustomSerializer());

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

    public void updateValue(Object value) throws OkaeriException {
        try {
            if (Modifier.isFinal(this.getField().getModifiers()) && FINAL_WARNS.add(this.getField().toString())) {
                LOGGER.warning(this.getField() + ": final fields (especially with default value) " +
                    "may prevent loading of the data. Removal of the final modifier is strongly advised.");
            }
            this.getField().setAccessible(true);
            this.getField().set(this.getObject(), value);
        } catch (IllegalAccessException exception) {
            throw new OkaeriException("failed to #updateValue", exception);
        }
    }

    public Object getValue() throws OkaeriException {

        if (this.isVariableHide()) {
            return this.getStartingValue();
        }

        try {
            this.getField().setAccessible(true);
            return this.getField().get(this.getObject());
        } catch (IllegalAccessException exception) {
            throw new OkaeriException("failed to #getValue", exception);
        }
    }

    public <T extends Annotation> Optional<T> getAnnotation(@NonNull Class<T> type) {
        return Optional.ofNullable(this.getField().getAnnotation(type));
    }

    public SerdesContextAttachments readStaticAnnotations(@NonNull Configurer configurer) {
        SerdesContextAttachments attachments = new SerdesContextAttachments();

        // Process field-level annotations first (these take precedence)
        for (Annotation annotation : this.getField().getAnnotations()) {
            SerdesAnnotationResolver<Annotation, SerdesContextAttachment> annotationResolver = configurer.getRegistry().getAnnotationResolver(annotation);
            if (annotationResolver == null) {
                continue;
            }
            Optional<? extends SerdesContextAttachment> attachmentOptional = annotationResolver.resolveAttachment(this.getField(), annotation);
            if (!attachmentOptional.isPresent()) {
                continue;
            }
            SerdesContextAttachment attachment = attachmentOptional.get();
            Class<? extends SerdesContextAttachment> attachmentType = attachment.getClass();
            attachments.put(attachmentType, attachment);
        }

        // Fallback to class-level annotations for attachment types not found on field
        Class<?> declaringClass = this.getField().getDeclaringClass();
        for (Annotation classAnnotation : declaringClass.getAnnotations()) {
            SerdesAnnotationResolver<Annotation, SerdesContextAttachment> annotationResolver = configurer.getRegistry().getAnnotationResolver(classAnnotation);
            if (annotationResolver == null) {
                continue;
            }

            // Try class-level resolution
            Optional<? extends SerdesContextAttachment> classAttachmentOptional = annotationResolver.resolveClassAttachment(declaringClass, classAnnotation);
            if (!classAttachmentOptional.isPresent()) {
                continue;
            }

            SerdesContextAttachment classAttachment = classAttachmentOptional.get();
            Class<? extends SerdesContextAttachment> attachmentType = classAttachment.getClass();

            // Only add if field doesn't already have this attachment type (field-level wins)
            if (!attachments.containsKey(attachmentType)) {
                attachments.put(attachmentType, classAttachment);
            }
        }

        return attachments;
    }

    @Data
    @RequiredArgsConstructor
    private static class CacheEntry {
        private final Class<?> type;
        private final String fieldName;
    }
}
