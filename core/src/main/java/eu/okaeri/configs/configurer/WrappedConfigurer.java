package eu.okaeri.configs.configurer;

import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.schema.FieldDeclaration;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.OkaeriSerdesPack;
import eu.okaeri.configs.serdes.SerdesContext;
import eu.okaeri.configs.serdes.SerdesRegistry;
import lombok.NonNull;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
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
    public SerdesRegistry getRegistry() {
        return this.wrapped.getRegistry();
    }

    @Override
    public void setRegistry(@NonNull SerdesRegistry registry) {
        this.wrapped.setRegistry(registry);
    }

    @Override
    public void register(@NonNull OkaeriSerdesPack pack) {
        this.wrapped.register(pack);
    }

    @Override
    public void setValue(@NonNull String key, Object value, GenericsDeclaration genericType, FieldDeclaration field) {
        this.wrapped.setValue(key, value, genericType, field);
    }

    @Override
    public Object getValue(@NonNull String key) {
        return this.wrapped.getValue(key);
    }

    @Override
    public boolean isToStringObject(@NonNull Object object, GenericsDeclaration genericType) {
        return this.wrapped.isToStringObject(object, genericType);
    }

    @Override
    public Object simplifyCollection(@NonNull Collection<?> value, GenericsDeclaration genericType, @NonNull SerdesContext serdesContext, boolean conservative) {
        return this.wrapped.simplifyCollection(value, genericType, serdesContext, conservative);
    }

    @Override
    public Object simplifyMap(@NonNull Map<Object, Object> value, GenericsDeclaration genericType, @NonNull SerdesContext serdesContext, boolean conservative) {
        return this.wrapped.simplifyMap(value, genericType, serdesContext, conservative);
    }

    @Override
    public Object simplify(Object value, GenericsDeclaration genericType, @NonNull SerdesContext serdesContext, boolean conservative) {
        return this.wrapped.simplify(value, genericType, serdesContext, conservative);
    }

    @Override
    public <T> T getValue(@NonNull String key, @NonNull Class<T> clazz, GenericsDeclaration genericType, @NonNull SerdesContext serdesContext) {
        return this.wrapped.getValue(key, clazz, genericType, serdesContext);
    }

    @Override
    public <T> T resolveType(Object object, GenericsDeclaration genericSource, @NonNull Class<T> targetClazz, GenericsDeclaration genericTarget, @NonNull SerdesContext serdesContext) {
        return this.wrapped.resolveType(object, genericSource, targetClazz, genericTarget, serdesContext);
    }

    @Override
    public Object createInstance(@NonNull Class<?> clazz) {
        return this.wrapped.createInstance(clazz);
    }

    @Override
    public boolean keyExists(@NonNull String key) {
        return this.wrapped.keyExists(key);
    }

    @Override
    public boolean isValid(@NonNull FieldDeclaration declaration, Object value) {
        return this.wrapped.isValid(declaration, value);
    }

    @Override
    public List<String> getAllKeys() {
        return this.wrapped.getAllKeys();
    }

    @Override
    public void write(@NonNull OutputStream outputStream, @NonNull ConfigDeclaration declaration) throws Exception {
        this.wrapped.write(outputStream, declaration);
    }

    @Override
    public void load(@NonNull InputStream inputStream, @NonNull ConfigDeclaration declaration) throws Exception {
        this.wrapped.load(inputStream, declaration);
    }
}
