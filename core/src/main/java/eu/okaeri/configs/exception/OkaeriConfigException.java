package eu.okaeri.configs.exception;

import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.ConfigPath;
import lombok.Getter;

/**
 * Exception with structured error information for configuration errors.
 * Provides detailed context about what went wrong and where.
 * <p>
 * Example output:
 * <pre>
 * Failed to load value at 'database.connections[0].port'
 *   Expected: Integer
 *   Actual:   "not_a_number" (String)
 * </pre>
 */
@Getter
public class OkaeriConfigException extends OkaeriException {

    private final ConfigPath path;
    private final GenericsDeclaration expectedType;
    private final Class<?> actualType;
    private final Object actualValue;

    public OkaeriConfigException(String message, ConfigPath path, GenericsDeclaration expectedType, Class<?> actualType, Object actualValue, Throwable cause) {
        super(buildMessage(message, path, expectedType, actualType, actualValue), cause);
        this.path = path;
        this.expectedType = expectedType;
        this.actualType = actualType;
        this.actualValue = actualValue;
    }

    public OkaeriConfigException(String message, ConfigPath path, GenericsDeclaration expectedType, Object actualValue, Throwable cause) {
        this(message, path, expectedType, (actualValue != null) ? actualValue.getClass() : null, actualValue, cause);
    }

    public OkaeriConfigException(ConfigPath path, GenericsDeclaration expectedType, Object actualValue, Throwable cause) {
        this(null, path, expectedType, actualValue, cause);
    }

    public OkaeriConfigException(String message, ConfigPath path, Throwable cause) {
        this(message, path, null, null, null, cause);
    }

    public OkaeriConfigException(String message, ConfigPath path) {
        this(message, path, null);
    }

    private static String buildMessage(String customMessage, ConfigPath path, GenericsDeclaration expectedType, Class<?> actualType, Object actualValue) {
        StringBuilder sb = new StringBuilder();

        // Custom message or default
        if ((customMessage != null) && !customMessage.isEmpty()) {
            sb.append(customMessage);
        } else {
            sb.append("Failed to load configuration value");
        }

        // Details section (path, expected, actual)
        boolean hasPath = (path != null) && !path.isEmpty();
        boolean hasTypeInfo = (expectedType != null) || (actualType != null);

        if (hasPath || hasTypeInfo) {
            sb.append("\n");

            if (hasPath) {
                sb.append("  At:       ").append(path).append("\n");
            }

            if (expectedType != null) {
                sb.append("  Expected: ").append(formatType(expectedType)).append("\n");
            }

            if (actualType != null) {
                sb.append("  Actual:   ");
                if (actualValue != null) {
                    sb.append(formatValue(actualValue));
                    sb.append(" (").append(actualType.getSimpleName()).append(")");
                } else {
                    sb.append("null");
                }
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
                GenericsDeclaration subtype = type.getSubtype().get(i);
                sb.append(formatType(subtype));
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
        if (value instanceof String) {
            str = "\"" + value + "\"";
        } else if (value instanceof Character) {
            str = "'" + value + "'";
        } else {
            str = String.valueOf(value);
        }

        // Truncate very long values
        int maxLen = 50;
        if (str.length() > maxLen) {
            str = str.substring(0, maxLen - 3) + "...";
        }

        return str;
    }

    /**
     * Returns a short description of the error suitable for logging.
     *
     * @return short description
     */
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

    /**
     * Creates an exception builder for fluent construction.
     *
     * @return new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String message;
        private ConfigPath path;
        private GenericsDeclaration expectedType;
        private Class<?> actualType;
        private Object actualValue;
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

        public Builder cause(Throwable cause) {
            this.cause = cause;
            return this;
        }

        public OkaeriConfigException build() {
            return new OkaeriConfigException(this.message, this.path, this.expectedType, this.actualType, this.actualValue, this.cause);
        }
    }
}
