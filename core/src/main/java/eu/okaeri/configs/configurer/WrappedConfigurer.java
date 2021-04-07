package eu.okaeri.configs.configurer;

import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.schema.FieldDeclaration;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.OkaeriSerdesPack;
import eu.okaeri.configs.serdes.TransformerRegistry;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Map;

public class WrappedConfigurer extends Configurer {

    private final Configurer wrapped;

    public WrappedConfigurer(Configurer wrapped) {
        this.wrapped = wrapped;
    }

    public Configurer getWrapped() {
        return this.wrapped;
    }

    @Override
    public void setRegistry(TransformerRegistry registry) {
        this.wrapped.setRegistry(registry);
    }

    @Override
    public TransformerRegistry getRegistry() {
        return this.wrapped.getRegistry();
    }

    @Override
    public void register(OkaeriSerdesPack pack) {
        this.wrapped.register(pack);
    }

    @Override
    public void setValue(String key, Object value, GenericsDeclaration genericType, FieldDeclaration field) {
        this.wrapped.setValue(key, value, genericType, field);
    }

    @Override
    public Object getValue(String key) {
        return this.wrapped.getValue(key);
    }

    @Override
    public boolean isToStringObject(Object object) {
        return this.wrapped.isToStringObject(object);
    }

    @Override
    public Object simplifyCollection(Collection<?> value, GenericsDeclaration genericType, boolean conservative) {
        return this.wrapped.simplifyCollection(value, genericType, conservative);
    }

    @Override
    public Object simplifyMap(Map<Object, Object> value, GenericsDeclaration genericType, boolean conservative) {
        return this.wrapped.simplifyMap(value, genericType, conservative);
    }

    @Override
    public Object simplify(Object value, GenericsDeclaration genericType, boolean conservative) {
        return this.wrapped.simplify(value, genericType, conservative);
    }

    @Override
    public <T> T getValue(String key, Class<T> clazz, GenericsDeclaration genericType) {
        return this.wrapped.getValue(key, clazz, genericType);
    }

    @Override
    public <T> T resolveType(Object object, GenericsDeclaration genericSource, Class<T> targetClazz, GenericsDeclaration genericTarget) {
        return this.wrapped.resolveType(object, genericSource, targetClazz, genericTarget);
    }

    @Override
    public Object createInstance(Class<?> clazz) {
        return this.wrapped.createInstance(clazz);
    }

    @Override
    public boolean keyExists(String key) {
        return this.wrapped.keyExists(key);
    }

    @Override
    public boolean isValid(FieldDeclaration declaration, Object value) {
        return this.wrapped.isValid(declaration, value);
    }

    @Override
    public void write(OutputStream outputStream, ConfigDeclaration declaration) throws Exception {
        this.wrapped.write(outputStream, declaration);
    }

    @Override
    public void load(InputStream inputStream, ConfigDeclaration declaration) throws Exception {
        this.wrapped.load(inputStream, declaration);
    }
}
