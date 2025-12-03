package eu.okaeri.configs.configurer;

import eu.okaeri.configs.schema.FieldDeclaration;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.SerdesContext;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class InMemoryWrappedConfigurer extends WrappedConfigurer {

    private final Map<String, Object> map;

    public InMemoryWrappedConfigurer(@NonNull Configurer configurer, @NonNull Map<String, Object> map) {
        super(configurer);
        this.map = map;
    }

    /**
     * Override to NOT propagate rawContent to the wrapped configurer.
     * <p>
     * Subconfigs should not overwrite the root config's rawContent.
     * When subconfigs need rawContent for error reporting, they access
     * the parent's rawContent via getRawContent() delegation chain.
     *
     * @param rawContent the raw content to set locally
     */
    @Override
    public void setRawContent(String rawContent) {
        super.rawContent = rawContent;
    }

    @Override
    public List<String> getAllKeys() {
        return Collections.unmodifiableList(new ArrayList<>(this.map.keySet()));
    }

    @Override
    public boolean keyExists(@NonNull String key) {
        return this.map.containsKey(key);
    }

    @Override
    public Object getValue(@NonNull String key) {
        return this.map.get(key);
    }

    @Override
    public <T> T getValue(@NonNull String key, @NonNull Class<T> clazz, GenericsDeclaration genericType, @NonNull SerdesContext serdesContext) {
        Object value = this.getValue(key);
        if (value == null) return null;
        return this.resolveType(value, GenericsDeclaration.of(value), clazz, genericType, serdesContext);
    }

    @Override
    public void setValue(@NonNull String key, Object value, GenericsDeclaration type, FieldDeclaration field) {
        this.map.put(key, value);
    }

    @Override
    public void setValueUnsafe(@NonNull String key, Object value) {
        this.map.put(key, value);
    }
}
