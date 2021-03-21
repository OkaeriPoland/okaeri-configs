package eu.okaeri.configs.schema;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Header;
import lombok.Data;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Data
public class ConfigDeclaration {

    public static ConfigDeclaration from(OkaeriConfig config) {

        ConfigDeclaration declaration = new ConfigDeclaration();
        Class<? extends OkaeriConfig> clazz = config.getClass();

        Header header = clazz.getAnnotation(Header.class);
        if (header != null) {
            declaration.setHeader(header.value());
        }

        declaration.setFields(Arrays.stream(clazz.getDeclaredFields())
                .map(field -> FieldDeclaration.from(field, config))
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));

        return declaration;
    }

    private String[] header;
    private List<FieldDeclaration> fields;
}
