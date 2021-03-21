package eu.okaeri.configs;

import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.transformer.ObjectTransformer;
import eu.okaeri.configs.transformer.TransformerRegistry;

import java.io.File;
import java.io.IOException;

public abstract class Configurer {

    public abstract String getCommentPrefix();

    public abstract String getSectionSeparator();

    public abstract void setValue(String key, Object value);

    public abstract Object getValue(String key);

    public <T> T getValue(String key, Class<T> clazz, GenericsDeclaration genericType) {
        Object value = this.getValue(key);
        if (value == null) return null;
        return this.resolveType(value, clazz, genericType);
    }

    public <T> T resolveType(Object object, Class<T> clazz, GenericsDeclaration genericType) {

        GenericsDeclaration source = new GenericsDeclaration(object.getClass());
        GenericsDeclaration target = (genericType == null) ? new GenericsDeclaration(clazz) : genericType;

        ObjectTransformer transformer = TransformerRegistry.getTransformer(source, target);
        if (transformer == null) {
            return clazz.cast(object);
        }

        //noinspection unchecked
        return clazz.cast(transformer.transform(object));
    }

    public boolean keyExists(String key) {
        return this.getValue(key) != null;
    }

    public abstract void writeToFile(File file, ConfigDeclaration declaration) throws IOException;

    public abstract void loadFromFile(File file, ConfigDeclaration declaration) throws IOException;
}
