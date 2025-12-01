package eu.okaeri.configs.serdes;

import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.schema.FieldDeclaration;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

import java.lang.annotation.Annotation;
import java.util.Optional;

@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SerdesContext {

    @NonNull private final Configurer configurer;
    private final FieldDeclaration field;
    private final SerdesContextAttachments attachments;
    @NonNull private final ConfigPath path;

    public static SerdesContext of(@NonNull Configurer configurer) {
        return of(configurer, null, new SerdesContextAttachments(), ConfigPath.root());
    }

    public static SerdesContext of(@NonNull Configurer configurer, FieldDeclaration field) {
        return of(configurer, field, (field == null) ? new SerdesContextAttachments() : field.readStaticAnnotations(configurer), ConfigPath.root());
    }

    public static SerdesContext of(@NonNull Configurer configurer, FieldDeclaration field, @NonNull SerdesContextAttachments attachments) {
        return new SerdesContext(configurer, field, attachments, ConfigPath.root());
    }

    public static SerdesContext of(@NonNull Configurer configurer, FieldDeclaration field, @NonNull SerdesContextAttachments attachments, @NonNull ConfigPath path) {
        return new SerdesContext(configurer, field, attachments, path);
    }

    public static SerdesContext.Builder builder() {
        return new SerdesContext.Builder();
    }

    public <T extends Annotation> Optional<T> getConfigAnnotation(@NonNull Class<T> type) {
        return (this.getConfigurer().getParent() == null)
            ? Optional.empty()
            : Optional.ofNullable(this.getConfigurer().getParent().getClass().getAnnotation(type));
    }

    public <T extends Annotation> Optional<T> getFieldAnnotation(@NonNull Class<T> type) {
        return (this.getField() == null)
            ? Optional.empty()
            : this.getField().getAnnotation(type);
    }

    @SuppressWarnings("unchecked")
    public <T extends SerdesContextAttachment> Optional<T> getAttachment(Class<T> type) {
        T attachment = (T) this.attachments.get(type);
        return Optional.ofNullable(attachment);
    }

    public <T extends SerdesContextAttachment> T getAttachment(Class<T> type, T defaultValue) {
        return this.getAttachment(type).orElse(defaultValue);
    }

    // ==================== Path Navigation ====================

    /**
     * Creates a new context with the path updated to include a property name.
     *
     * @param name the property name
     * @return new context with updated path
     */
    public SerdesContext withProperty(@NonNull String name) {
        return new SerdesContext(this.configurer, this.field, this.attachments, this.path.property(name));
    }

    /**
     * Creates a new context with the path updated to include a list/array index.
     *
     * @param index the index (0-based)
     * @return new context with updated path
     */
    public SerdesContext withIndex(int index) {
        return new SerdesContext(this.configurer, this.field, this.attachments, this.path.index(index));
    }

    /**
     * Creates a new context with the path updated to include a map key.
     *
     * @param key the map key
     * @return new context with updated path
     */
    public SerdesContext withKey(@NonNull Object key) {
        return new SerdesContext(this.configurer, this.field, this.attachments, this.path.key(key));
    }

    /**
     * Creates a new context with the specified path.
     *
     * @param path the new path
     * @return new context with the given path
     */
    public SerdesContext withPath(@NonNull ConfigPath path) {
        return new SerdesContext(this.configurer, this.field, this.attachments, path);
    }

    /**
     * Creates a new context with the specified field declaration but same path.
     *
     * @param field the field declaration
     * @return new context with updated field
     */
    public SerdesContext withField(FieldDeclaration field) {
        SerdesContextAttachments newAttachments = (field == null) ? new SerdesContextAttachments() : field.readStaticAnnotations(this.configurer);
        return new SerdesContext(this.configurer, field, newAttachments, this.path);
    }

    private static class Builder {

        private final SerdesContextAttachments attachments = new SerdesContextAttachments();
        private Configurer configurer;
        private FieldDeclaration field;
        private ConfigPath path = ConfigPath.root();

        public Builder configurer(Configurer configurer) {
            this.configurer = configurer;
            return this;
        }

        public Builder field(FieldDeclaration field) {
            this.field = field;
            return this;
        }

        public Builder path(ConfigPath path) {
            this.path = path;
            return this;
        }

        public <A extends SerdesContextAttachment> Builder attach(Class<A> type, A attachment) {
            if (this.attachments.containsKey(type)) {
                throw new IllegalArgumentException("cannot override SerdesContext attachment of type " + type);
            }
            this.attachments.put(type, attachment);
            return this;
        }

        public SerdesContext build() {
            return SerdesContext.of(this.configurer, this.field, this.attachments, this.path);
        }

        @Deprecated
        public SerdesContext create() {
            return this.build();
        }
    }
}
