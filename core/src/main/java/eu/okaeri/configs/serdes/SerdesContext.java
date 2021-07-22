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

    public static SerdesContext of(@NonNull Configurer configurer) {
        return of(configurer, null, new SerdesContextAttachments());
    }

    public static SerdesContext of(@NonNull Configurer configurer, FieldDeclaration field) {
        return of(configurer, field, (field == null) ? new SerdesContextAttachments() : field.readStaticAnnotations(configurer));
    }

    public static SerdesContext of(@NonNull Configurer configurer, FieldDeclaration field, @NonNull SerdesContextAttachments attachments) {
        return new SerdesContext(configurer, null, attachments);
    }

    public static SerdesContext.Builder builder() {
        return new SerdesContext.Builder();
    }

    @NonNull private final Configurer configurer;
    private final FieldDeclaration field;
    private final SerdesContextAttachments attachments;

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

    private static class Builder {

        private Configurer configurer;
        private FieldDeclaration field;
        private final SerdesContextAttachments attachments = new SerdesContextAttachments();

        public void configurer(Configurer configurer) {
            this.configurer = configurer;
        }

        public void field(FieldDeclaration field) {
            this.field = field;
        }

        public <A extends SerdesContextAttachment> Builder attach(Class<A> type, A attachment) {
            if (this.attachments.containsKey(type)) {
                throw new IllegalArgumentException("cannot override SerdesContext attachment of type " + type);
            }
            this.attachments.put(type, attachment);
            return this;
        }

        public SerdesContext create() {
            return SerdesContext.of(this.configurer, this.field, this.attachments);
        }
    }
}
