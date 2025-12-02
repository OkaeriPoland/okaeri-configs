package eu.okaeri.configs.schema;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Header;
import eu.okaeri.configs.annotation.Headers;
import eu.okaeri.configs.annotation.Names;
import eu.okaeri.configs.annotation.Include;
import lombok.Data;
import lombok.NonNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Data
public class ConfigDeclaration {

    private static final Map<Class<?>, ConfigDeclaration> DECLARATION_CACHE = new ConcurrentHashMap<>();

    private Names nameStrategy;
    private String[] header;
    private Map<String, FieldDeclaration> fieldMap;
    private boolean real;
    private Class<?> type;

    public static ConfigDeclaration of(@NonNull Class<?> clazz, OkaeriConfig config) {
        return of(clazz, (Object) config);
    }

    public static ConfigDeclaration of(@NonNull Class<?> clazz, Object object) {

        ConfigDeclaration template = DECLARATION_CACHE.computeIfAbsent(clazz, (klass) -> {
            ConfigDeclaration declaration = new ConfigDeclaration();
            declaration.setNameStrategy(readNames(klass));
            declaration.setHeader(readHeader(klass));
            declaration.setReal(OkaeriConfig.class.isAssignableFrom(klass));
            declaration.setType(klass);
            return declaration;
        });

        ConfigDeclaration declaration = new ConfigDeclaration();
        declaration.setNameStrategy(template.getNameStrategy());
        declaration.setHeader(template.getHeader());
        declaration.setReal(template.isReal());
        declaration.setType(template.getType());
        declaration.setFieldMap(readFields(clazz, declaration, object));

        Include[] subs = clazz.getDeclaredAnnotationsByType(Include.class);
        for (Include sub : subs) {
            if (!sub.value().isAssignableFrom(clazz)) {
                throw new IllegalArgumentException(
                    "@Include can only include classes that " + clazz.getName() + " extends. " +
                    "Cannot include fields from " + sub.value().getName() + " because it is not a superclass of " + clazz.getName()
                );
            }
            Map<String, FieldDeclaration> subFields = readFields(sub.value(), declaration, object);
            subFields.forEach((key, value) -> {
                if (declaration.getFieldMap().containsKey(key)) {
                    return;
                }
                declaration.getFieldMap().put(key, value);
            });
        }

        return declaration;
    }

    public static ConfigDeclaration of(@NonNull OkaeriConfig config) {
        return of(config.getClass(), config);
    }

    public static ConfigDeclaration of(@NonNull Object object) {
        return of(object.getClass(), object);
    }

    public static ConfigDeclaration of(@NonNull Class<?> clazz) {
        return of(clazz, null);
    }

    private static String[] readHeader(@NonNull Class<?> clazz) {

        Headers headers = clazz.getAnnotation(Headers.class);
        if (headers != null) {
            List<String> headerList = new ArrayList<>();
            for (Header header : headers.value()) {
                headerList.addAll(Arrays.asList(header.value()));
            }
            return headerList.toArray(new String[0]);
        }

        Header header = clazz.getAnnotation(Header.class);
        if (header != null) {
            return header.value();
        }

        return null;
    }

    private static Names readNames(@NonNull Class<?> clazz) {
        Names names = clazz.getAnnotation(Names.class);
        while (names == null) {
            clazz = clazz.getEnclosingClass();
            if (clazz == null) {
                return null;
            }
            names = clazz.getAnnotation(Names.class);
        }
        return names;
    }

    private static LinkedHashMap<String, FieldDeclaration> readFields(@NonNull Class<?> clazz, @NonNull ConfigDeclaration declaration, Object object) {
        return Arrays.stream(clazz.getDeclaredFields())
            .filter(field -> !field.getName().startsWith("this$"))
            .map(field -> FieldDeclaration.of(declaration, field, object))
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(
                FieldDeclaration::getName,
                field -> field,
                (u, v) -> {
                    throw new IllegalStateException("Duplicate key/field (by name)!\nLeft: " + u + "\nRight: " + v);
                },
                LinkedHashMap::new
            ));
    }

    public Optional<FieldDeclaration> getField(@NonNull String key) {
        return Optional.ofNullable(this.fieldMap.get(key));
    }

    public GenericsDeclaration getGenericsOrNull(@NonNull String key) {
        return this.getField(key)
            .map(FieldDeclaration::getType)
            .orElse(null);
    }

    public Collection<FieldDeclaration> getFields() {
        return this.fieldMap.values();
    }

    /**
     * Finds how many leading parts of a dotted path are OkaeriConfig fields.
     * Used for determining section depth in formats like TOML and INI.
     * <p>
     * Example with config having {@code SubConfig sub} field which has {@code String name}:
     * <ul>
     *   <li>{@code findConfigDepth(["sub", "name"], 10)} → 1 (sub is config, name is not)</li>
     *   <li>{@code findConfigDepth(["sub", "name"], 0)} → 0 (maxDepth reached)</li>
     *   <li>{@code findConfigDepth(["unknown", "x"], 10)} → 0 (unknown field)</li>
     * </ul>
     *
     * @param parts    path parts to check
     * @param maxDepth maximum depth to search
     * @return number of consecutive OkaeriConfig fields from the start
     */
    public int findConfigDepth(@NonNull String[] parts, int maxDepth) {
        ConfigDeclaration currentDecl = this;
        int depth = 0;

        for (int i = 0; (i < (parts.length - 1)) && (i < maxDepth); i++) {
            Optional<FieldDeclaration> field = currentDecl.getField(parts[i]);
            if (!field.isPresent()) {
                break;
            }

            GenericsDeclaration fieldType = field.get().getType();
            if (fieldType.isConfig()) {
                currentDecl = ConfigDeclaration.of(fieldType.getType());
                depth = i + 1;
            } else {
                break;
            }
        }

        return depth;
    }

    /**
     * Resolves the ConfigDeclaration for a nested field value.
     * Handles direct OkaeriConfig fields, List&lt;Config&gt;, and Map&lt;K, Config&gt;.
     * <p>
     * Example:
     * <ul>
     *   <li>Field type {@code SubConfig} → returns SubConfig declaration</li>
     *   <li>Field type {@code List<SubConfig>}, value is List → returns SubConfig declaration</li>
     *   <li>Field type {@code Map<String, SubConfig>}, value is Map → returns SubConfig declaration</li>
     *   <li>Field type {@code String} → returns null</li>
     * </ul>
     *
     * @param key   the field name
     * @param value the field value (used to distinguish List vs Map when type is ambiguous)
     * @return the nested ConfigDeclaration, or null if not a config type
     */
    public ConfigDeclaration resolveNestedDeclaration(@NonNull String key, Object value) {
        Optional<FieldDeclaration> field = this.getField(key);
        if (!field.isPresent()) {
            return null;
        }

        GenericsDeclaration fieldType = field.get().getType();

        // Direct subconfig
        if (fieldType.isConfig()) {
            return ConfigDeclaration.of(fieldType.getType());
        }

        // List of configs
        if (value instanceof List) {
            GenericsDeclaration elementType = fieldType.getSubtypeAtOrNull(0);
            if ((elementType != null) && elementType.isConfig()) {
                return ConfigDeclaration.of(elementType.getType());
            }
        }

        // Map of configs
        if (value instanceof Map) {
            GenericsDeclaration valueType = fieldType.getSubtypeAtOrNull(1);
            if ((valueType != null) && valueType.isConfig()) {
                return ConfigDeclaration.of(valueType.getType());
            }
        }

        return null;
    }
}
