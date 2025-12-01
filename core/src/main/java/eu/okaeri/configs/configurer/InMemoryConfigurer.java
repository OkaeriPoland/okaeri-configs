package eu.okaeri.configs.configurer;

import eu.okaeri.configs.annotation.Serdes;
import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.schema.FieldDeclaration;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.SerdesContext;
import eu.okaeri.configs.serdes.SerdesRegistry;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

@NoArgsConstructor
public class InMemoryConfigurer extends Configurer {

    private Map<String, Object> map = new LinkedHashMap<>();

    public InMemoryConfigurer(@NonNull Map<String, Object> map) {
        this.map = new LinkedHashMap<>(map);
    }

    /**
     * Sets a value in the in-memory map after applying serialization transformations.
     * <p>
     * This method calls {@link #simplify(Object, GenericsDeclaration, SerdesContext, boolean)}
     * before storing the value to ensure proper handling of:
     * <ul>
     *   <li>Custom per-field serializers specified via {@link Serdes}</li>
     *   <li>Type transformations registered in the {@link SerdesRegistry}</li>
     *   <li>Complex object serialization (nested configs, collections, etc.)</li>
     * </ul>
     * <p>
     * This follows the same pattern as other configurers (e.g., YamlSnakeYamlConfigurer)
     * to maintain consistency across different storage backends.
     *
     * @param key   the configuration key
     * @param value the raw value to store
     * @param type  the generic type information for the value
     * @param field the field declaration containing metadata and potential custom serializer
     */
    @Override
    public void setValue(@NonNull String key, Object value, GenericsDeclaration type, FieldDeclaration field) {
        Object simplified = this.simplify(value, type, SerdesContext.of(this, field), true);
        this.map.put(key, simplified);
    }

    @Override
    public void setValueUnsafe(@NonNull String key, Object value) {
        this.map.put(key, value);
    }

    @Override
    public Object getValue(@NonNull String key) {
        return this.map.get(key);
    }

    @Override
    public Object remove(@NonNull String key) {
        return this.map.remove(key);
    }

    @Override
    public boolean keyExists(@NonNull String key) {
        return this.map.containsKey(key);
    }

    @Override
    public List<String> getAllKeys() {
        return Collections.unmodifiableList(new ArrayList<>(this.map.keySet()));
    }

    @Override
    public void load(@NonNull InputStream inputStream, @NonNull ConfigDeclaration declaration) throws Exception {
    }

    @Override
    public void write(@NonNull OutputStream outputStream, @NonNull ConfigDeclaration declaration) throws Exception {
    }
}
