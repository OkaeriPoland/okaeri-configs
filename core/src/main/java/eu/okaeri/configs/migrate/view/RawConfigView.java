package eu.okaeri.configs.migrate.view;

import eu.okaeri.configs.OkaeriConfig;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

@AllArgsConstructor
@RequiredArgsConstructor
public class RawConfigView {

    private final OkaeriConfig config;
    private String nestedSeparator = "\\.";

    public boolean exists(@NonNull String key) {
        Map<String, Object> document = this.config.asMap(this.config.getConfigurer(), true);
        return this.valueExists(document, key);
    }

    public Object get(@NonNull String key) {
        Map<String, Object> document = this.config.asMap(this.config.getConfigurer(), true);
        return this.valueExtract(document, key);
    }

    public Object set(@NonNull String key, Object value) {

        Map<String, Object> document = this.config.asMap(this.config.getConfigurer(), true);
        Object old = this.valuePut(document, key, value);

        // mutate parent
        this.config.load(document);

        return old;
    }

    public Object remove(@NonNull String key) {

        Map<String, Object> document = this.config.asMap(this.config.getConfigurer(), true);
        Object old = this.valueRemove(document, key);

        // top-level keys may require manual remove
        if (key.split(this.nestedSeparator).length == 1) {
            this.config.getConfigurer().remove(key);
        }

        // mutate parent
        this.config.load(document);

        return old;
    }

    protected boolean valueExists(Map document, String path) {
        String[] split = path.split(this.nestedSeparator);
        for (int i = 0; i < split.length; i++) {
            String part = split[i];
            // last element reached
            if (i == (split.length - 1)) {
                return document.containsKey(part);
            }
            // sub-map
            Object element = document.get(part);
            if (element instanceof Map) {
                document = (Map) element;
                continue;
            }
            // cannot go deeper
            return false;
        }
        return false;
    }

    protected Object valueExtract(Map document, String path) {
        String[] split = path.split(this.nestedSeparator);
        for (int i = 0; i < split.length; i++) {
            String part = split[i];
            Object element = document.get(part);
            // last element reached
            if (i == (split.length - 1)) {
                return element;
            }
            // sub-map
            if (element instanceof Map) {
                document = (Map) element;
                continue;
            }
            // cannot go deeper
            String elementStr = (element == null) ? "null" : element.getClass().getSimpleName();
            throw new IllegalArgumentException("Cannot extract '" + path + "': " +
                "not deep enough (ended at index " + i + " [" + part + ":" + elementStr + "])");
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    protected Object valuePut(Map document, String path, Object value) {
        String[] split = path.split(this.nestedSeparator);
        for (int i = 0; i < split.length; i++) {
            String part = split[i];
            // last element reached
            if (i == (split.length - 1)) {
                return document.put(part, value);
            }
            // sub-map
            Object element = document.get(part);
            if (element instanceof Map) {
                document = (Map) element;
                continue;
            }
            // cannot go deeper and element is not null
            if (element != null) {
                String elementStr = element.getClass().getSimpleName();
                throw new IllegalArgumentException("Cannot insert '" + path + "': " +
                    "type conflict (ended at index " + i + " [" + part + ":" + elementStr + "])");
            }
            // no level, insert empty map
            Map map = new LinkedHashMap<>();
            document.put(part, map);
            document = map;
        }
        throw new IllegalArgumentException("Cannot put '" + path + "'");
    }

    protected Object valueRemove(Map document, String path) {
        String[] split = path.split(this.nestedSeparator);
        for (int i = 0; i < split.length; i++) {
            String part = split[i];
            // last element reached
            if (i == (split.length - 1)) {
                return document.remove(part);
            }
            // sub-map
            Object element = document.get(part);
            if (element instanceof Map) {
                document = (Map) element;
                continue;
            }
            // cannot go deeper
            return null;
        }
        return null;
    }
}
