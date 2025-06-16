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

@Data
public class ConfigDeclaration {

    private static final Map<Class<?>, ConfigDeclaration> DECLARATION_CACHE = new ConcurrentHashMap<>();

    private Names nameStrategy;
    private String[] header;
    private Map<String, FieldDeclaration> fieldMap;
    private Map<String, FieldDeclaration> excludedFieldMap;
    private Map<String, FieldDeclaration> readOnlyFieldMap;
    private boolean real;
    private Class<?> type;

    public static ConfigDeclaration of(@NonNull Class<?> clazz, OkaeriConfig config) {
        return of(clazz, (Object) config);
    }

    public static ConfigDeclaration of(@NonNull Class<?> clazz, Object object) {
        ConfigDeclaration template = getOrCreateTemplateDeclaration(clazz);

        ConfigDeclaration declaration = createDeclarationFromTemplate(template);

        populateFieldMaps(clazz, declaration, object);

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

    private static ConfigDeclaration getOrCreateTemplateDeclaration(Class<?> clazz) {
        return DECLARATION_CACHE.computeIfAbsent(clazz, klass -> {
            ConfigDeclaration declaration = new ConfigDeclaration();
            declaration.setNameStrategy(readNames(klass));
            declaration.setHeader(readHeader(klass));
            declaration.setReal(OkaeriConfig.class.isAssignableFrom(klass));
            declaration.setType(klass);
            return declaration;
        });
    }

    private static ConfigDeclaration createDeclarationFromTemplate(ConfigDeclaration template) {
        ConfigDeclaration declaration = new ConfigDeclaration();
        declaration.setNameStrategy(template.getNameStrategy());
        declaration.setHeader(template.getHeader());
        declaration.setReal(template.isReal());
        declaration.setType(template.getType());
        return declaration;
    }

    private static void populateFieldMaps(Class<?> clazz, ConfigDeclaration declaration, Object object) {
        Map<String, FieldDeclaration> fieldMap = new LinkedHashMap<>();
        Map<String, FieldDeclaration> excludedMap = new LinkedHashMap<>();
        Map<String, FieldDeclaration> readOnlyMap = new LinkedHashMap<>();

        collectFields(clazz, declaration, object, fieldMap, excludedMap, readOnlyMap);
        collectIncludedFields(clazz, declaration, object, fieldMap, excludedMap, readOnlyMap);


        declaration.setFieldMap(fieldMap);
        declaration.setExcludedFieldMap(excludedMap);
        declaration.setReadOnlyFieldMap(readOnlyMap);
    }

    private static void collectIncludedFields(Class<?> clazz, ConfigDeclaration declaration, Object object,
                                              Map<String, FieldDeclaration> fieldMap,
                                              Map<String, FieldDeclaration> excludedMap,
                                              Map<String, FieldDeclaration> readOnlyMap) {
        Arrays.stream(clazz.getDeclaredAnnotationsByType(Include.class))
            .forEach(include -> collectFields(include.value(), declaration, object, fieldMap, excludedMap, readOnlyMap));
    }

    private static void collectFields(Class<?> clazz, ConfigDeclaration declaration, Object object,
                                      Map<String, FieldDeclaration> fieldMap,
                                      Map<String, FieldDeclaration> excludedMap,
                                      Map<String, FieldDeclaration> readOnlyMap) {
        Arrays.stream(clazz.getDeclaredFields())
            .filter(field -> !field.getName().startsWith("this$"))
            .map(field -> FieldDeclaration.of(declaration, field, object))
            .filter(Objects::nonNull)
            .forEach(fd -> addToAppropriateMap(fd, fieldMap, excludedMap, readOnlyMap));
    }

    private static void addToAppropriateMap(FieldDeclaration fieldDecl,
                                            Map<String, FieldDeclaration> fieldMap,
                                            Map<String, FieldDeclaration> excludedMap,
                                            Map<String, FieldDeclaration> readOnlyMap) {
        String name = fieldDecl.getName();
        switch (fieldDecl.getFieldType()) {
            case EXCLUDED:
                excludedMap.putIfAbsent(name, fieldDecl);
                break;
            case READ_ONLY:
                readOnlyMap.putIfAbsent(name, fieldDecl);
                break;
            case NORMAL:
            default:
                fieldMap.putIfAbsent(name, fieldDecl);
                break;
        }
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

    public Collection<FieldDeclaration> getExcludedFields() {
        return this.excludedFieldMap.values();
    }

    public Collection<FieldDeclaration> getReadOnlyFields() {
        return this.readOnlyFieldMap.values();
    }
}
