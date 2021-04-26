package eu.okaeri.configs.schema;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Header;
import eu.okaeri.configs.annotation.Headers;
import eu.okaeri.configs.annotation.Names;
import lombok.Data;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Data
public class ConfigDeclaration {

    private static final Map<Class<?>, ConfigDeclaration> DECLARATION_CACHE = new ConcurrentHashMap<>();

    public static ConfigDeclaration of(Class<?> clazz, OkaeriConfig config) {

        ConfigDeclaration template = DECLARATION_CACHE.computeIfAbsent(clazz, (klass) -> {
            ConfigDeclaration declaration = new ConfigDeclaration();
            declaration.setNameStrategy(klass.getAnnotation(Names.class));
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

        declaration.setFieldMap(Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> !"this$0".equals(field.getName()))
                .map(field -> FieldDeclaration.of(declaration, field, config))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(FieldDeclaration::getName, field -> field)));

        return declaration;
    }

    public static ConfigDeclaration of(OkaeriConfig config) {
        return of(config.getClass(), config);
    }

    public static ConfigDeclaration of(Class<?> clazz) {
        return of(clazz, null);
    }

    private static String[] readHeader(Class<?> clazz) {

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

    public Optional<FieldDeclaration> getField(String key) {
        return Optional.ofNullable(this.fieldMap.get(key));
    }

    public GenericsDeclaration getGenericsOrNull(String key) {
        return this.getField(key)
                .map(FieldDeclaration::getType)
                .orElse(null);
    }

    public Collection<FieldDeclaration> getFields() {
        return this.fieldMap.values();
    }

    private Names nameStrategy;
    private String[] header;
    private Map<String, FieldDeclaration> fieldMap;
    private boolean real;
    private Class<?> type;
}
