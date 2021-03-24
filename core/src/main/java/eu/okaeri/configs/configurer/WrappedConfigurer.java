package eu.okaeri.configs.configurer;

import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.OkaeriSerdesPack;
import eu.okaeri.configs.serdes.TransformerRegistry;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

public class WrappedConfigurer extends Configurer {

    private final Configurer parent;

    public WrappedConfigurer(Configurer parent) {
        this.parent = parent;
    }

    public Configurer getParent() {
        return this.parent;
    }

    @Override
    public void setRegistry(TransformerRegistry registry) {
        this.parent.setRegistry(registry);
    }

    @Override
    public TransformerRegistry getRegistry() {
        return this.parent.getRegistry();
    }

    @Override
    public void register(OkaeriSerdesPack pack) {
        this.parent.register(pack);
    }

    @Override
    public void setValue(String key, Object value, GenericsDeclaration genericType) {
        this.parent.setValue(key, value, genericType);
    }

    @Override
    public Object getValue(String key) {
        return this.parent.getValue(key);
    }

    @Override
    public boolean isToStringObject(Object object) {
        return this.parent.isToStringObject(object);
    }

    @Override
    public Object simplifyCollection(Collection<?> value, GenericsDeclaration genericType) {
        return this.parent.simplifyCollection(value, genericType);
    }

    @Override
    public Object simplifyMap(Map<Object, Object> value, GenericsDeclaration genericType) {
        return this.parent.simplifyMap(value, genericType);
    }

    @Override
    public Object simplify(Object value, GenericsDeclaration genericType) {
        return this.parent.simplify(value, genericType);
    }

    @Override
    public <T> T getValue(String key, Class<T> clazz, GenericsDeclaration genericType) {
        return this.parent.getValue(key, clazz, genericType);
    }

    @Override
    public <T> T resolveType(Object object, GenericsDeclaration genericSource, Class<T> targetClazz, GenericsDeclaration genericTarget) {
        return this.parent.resolveType(object, genericSource, targetClazz, genericTarget);
    }

    @Override
    public Object createInstance(Class<?> clazz) {
        return this.parent.createInstance(clazz);
    }

    @Override
    public boolean keyExists(String key) {
        return this.parent.keyExists(key);
    }

    @Override
    public void writeToFile(File file, ConfigDeclaration declaration) throws IOException {
        this.parent.writeToFile(file, declaration);
    }

    @Override
    public void loadFromFile(File file, ConfigDeclaration declaration) throws IOException {
        this.parent.loadFromFile(file, declaration);
    }
}
