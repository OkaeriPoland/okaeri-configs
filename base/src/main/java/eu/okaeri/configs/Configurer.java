package eu.okaeri.configs;

import eu.okaeri.configs.schema.ConfigDeclaration;

import java.io.File;
import java.io.IOException;

public interface Configurer {

    String getCommentPrefix();

    String getSectionSeparator();

    void setValue(String key, Object value);

    Object getValue(String key);

    default <T> T getValue(String key, Class<T> clazz) {
        Object value = this.getValue(key);
        if (value == null) return null;
        return clazz.cast(value);
    }

    default Byte getValueAsByte(String key) {
        return this.getValue(key, Byte.class);
    }

    default Character getValueAsCharacter(String key) {
        return this.getValue(key, Character.class);
    }

    default Short getValueAsShort(String key) {
        return this.getValue(key, Short.class);
    }

    default Integer getValueAsInteger(String key) {
        return this.getValue(key, Integer.class);
    }

    default Long getValueAsLong(String key) {
        return this.getValue(key, Long.class);
    }

    default Float getValueAsFloat(String key) {
        return this.getValue(key, Float.class);
    }

    default Double getValueAsDouble(String key) {
        return this.getValue(key, Double.class);
    }

    default Boolean getValueAsBoolean(String key) {
        return this.getValue(key, Boolean.class);
    }

    default String getValueAsString(String key) {
        return this.getValue(key, String.class);
    }

    void writeToFile(File file, ConfigDeclaration declaration) throws IOException;

    void loadFromFile(File file, ConfigDeclaration declaration) throws IOException;
}
