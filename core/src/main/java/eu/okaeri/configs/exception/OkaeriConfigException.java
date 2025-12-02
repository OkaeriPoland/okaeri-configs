package eu.okaeri.configs.exception;

import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.format.SourceErrorMarker;
import eu.okaeri.configs.format.SourceWalker;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.ConfigPath;
import lombok.Getter;

import java.util.regex.PatternSyntaxException;

@Getter
public class OkaeriConfigException extends OkaeriException {

    private final ConfigPath path;
    private final GenericsDeclaration expectedType;
    private final Class<?> actualType;
    private final Object actualValue;
    private final String sourceFile;
    private final Configurer configurer;
    private final String errorCode;

    private OkaeriConfigException(String message, ConfigPath path, GenericsDeclaration expectedType, Class<?> actualType, Object actualValue, String sourceFile, Configurer configurer, String errorCode, Throwable cause) {
        super(buildMessage(message, path, expectedType, actualType, actualValue, sourceFile, configurer, errorCode, cause), cause);
        this.path = path;
        this.expectedType = expectedType;
        this.actualType = actualType;
        this.actualValue = actualValue;
        this.sourceFile = sourceFile;
        this.configurer = configurer;
        this.errorCode = errorCode;
    }

    public String getRawContent() {
        return (this.configurer != null) ? this.configurer.getRawContent() : null;
    }

    public String getShortDescription() {
        StringBuilder sb = new StringBuilder();
        if ((this.path != null) && !this.path.isEmpty()) {
            sb.append("at '").append(this.path).append("'");
        }
        if (this.expectedType != null) {
            if (sb.length() > 0) sb.append(": ");
            sb.append("expected ").append(formatType(this.expectedType));
        }
        if (this.actualType != null) {
            sb.append(", got ").append(this.actualType.getSimpleName());
        }
        return sb.toString();
    }

    private static String buildMessage(String customMessage, ConfigPath path, GenericsDeclaration expectedType, Class<?> actualType, Object actualValue, String sourceFile, Configurer configurer, String errorCode, Throwable cause) {
        StringBuilder sb = new StringBuilder();

        // Error code header like Rust's error[E0080]:
        if ((errorCode != null) && !errorCode.isEmpty()) {
            sb.append("error[").append(errorCode).append("]: ");
        }

        // Message: 'path' to ExpectedType from ActualType
        if ((customMessage != null) && !customMessage.isEmpty()) {
            sb.append(customMessage);
        } else {
            sb.append("Failed to load");
        }

        boolean hasPath = (path != null) && !path.isEmpty();
        if (hasPath) {
            sb.append(" '").append(path).append("'");
        }

        if (expectedType != null) {
            sb.append(" to ").append(formatType(expectedType));
        }

        if (actualType != null) {
            sb.append(" from ").append(actualType.getSimpleName());
        }

        // Extract hint from cause
        String hint = null;
        int valueOffset = -1;
        int valueLength = 1;
        int contextLinesBefore = ValueIndexedException.DEFAULT_CONTEXT_BEFORE;
        int contextLinesAfter = ValueIndexedException.DEFAULT_CONTEXT_AFTER;

        if (cause instanceof ValueIndexedException) {
            // Generic indexed exception - preferred way
            ValueIndexedException vie = (ValueIndexedException) cause;
            hint = vie.getMessage();
            valueOffset = vie.getStartIndex();
            valueLength = vie.getLength();
            contextLinesBefore = vie.getContextLinesBefore();
            contextLinesAfter = vie.getContextLinesAfter();
        } else if (cause instanceof PatternSyntaxException) {
            // Backwards compatibility for direct PatternSyntaxException
            PatternSyntaxException pse = (PatternSyntaxException) cause;
            hint = pse.getDescription();
            valueOffset = pse.getIndex();
        } else if ((cause != null) && (cause.getMessage() != null)) {
            hint = cause.getMessage();
        }

        // Add source marker if walker is available
        SourceWalker walker = (configurer != null) ? configurer.createSourceWalker() : null;
        String rawContent = (configurer != null) ? configurer.getRawContent() : null;
        boolean hasSourceMarker = false;
        if ((walker != null) && hasPath) {
            String marker = SourceErrorMarker.format(walker, path, sourceFile, hint, valueOffset, valueLength, rawContent, contextLinesBefore, contextLinesAfter);
            if (!marker.isEmpty()) {
                sb.append("\n").append(marker);
                hasSourceMarker = true;
            }
        }

        // Show actual value when no source marker
        if (!hasSourceMarker && (actualValue != null)) {
            sb.append(": ").append(formatValue(actualValue));
            // Include hint if available (e.g., valid enum values)
            if ((hint != null) && !hint.isEmpty()) {
                sb.append(" (").append(hint).append(")");
            }
        }

        return sb.toString();
    }

    private static String formatType(GenericsDeclaration type) {
        if (type == null) {
            return "?";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(type.getType().getSimpleName());
        if (type.hasSubtypes()) {
            sb.append("<");
            for (int i = 0; i < type.getSubtype().size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(formatType(type.getSubtype().get(i)));
            }
            sb.append(">");
        }
        return sb.toString();
    }

    private static String formatValue(Object value) {
        if (value == null) {
            return "null";
        }
        String str;
        try {
            if (value instanceof String) {
                str = "\"" + value + "\"";
            } else if (value instanceof Character) {
                str = "'" + value + "'";
            } else {
                str = String.valueOf(value);
            }
        } catch (Exception e) {
            str = "<" + value.getClass().getSimpleName() + ">";
        }
        if (str.length() > 50) {
            str = str.substring(0, 47) + "...";
        }
        return str;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String message;
        private ConfigPath path;
        private GenericsDeclaration expectedType;
        private Class<?> actualType;
        private Object actualValue;
        private String sourceFile;
        private Configurer configurer;
        private String errorCode;
        private Throwable cause;

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder path(ConfigPath path) {
            this.path = path;
            return this;
        }

        public Builder expectedType(GenericsDeclaration expectedType) {
            this.expectedType = expectedType;
            return this;
        }

        public Builder expectedType(Class<?> expectedType) {
            this.expectedType = GenericsDeclaration.of(expectedType);
            return this;
        }

        public Builder actualType(Class<?> actualType) {
            this.actualType = actualType;
            return this;
        }

        public Builder actualValue(Object actualValue) {
            this.actualValue = actualValue;
            if ((actualValue != null) && (this.actualType == null)) {
                this.actualType = actualValue.getClass();
            }
            return this;
        }

        public Builder sourceFile(String sourceFile) {
            this.sourceFile = sourceFile;
            return this;
        }

        public Builder configurer(Configurer configurer) {
            this.configurer = configurer;
            return this;
        }

        public Builder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public Builder errorCode(Class<?> serdesClass) {
            this.errorCode = (serdesClass != null) ? serdesClass.getSimpleName() : null;
            return this;
        }

        public Builder cause(Throwable cause) {
            this.cause = cause;
            return this;
        }

        public OkaeriConfigException build() {
            return new OkaeriConfigException(this.message, this.path, this.expectedType, this.actualType, this.actualValue, this.sourceFile, this.configurer, this.errorCode, this.cause);
        }
    }
}
