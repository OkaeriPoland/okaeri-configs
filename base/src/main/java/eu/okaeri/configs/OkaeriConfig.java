package eu.okaeri.configs;

import eu.okaeri.configs.schema.FieldDeclaration;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


public abstract class OkaeriConfig {

    @Getter @Setter private File bindFile;
    @Getter @Setter private OkaeriConfigurer configurer;
    private List<FieldDeclaration> fields;

    public OkaeriConfig() {
        this.fields = Arrays.stream(this.getClass().getDeclaredFields())
                .map(field -> FieldDeclaration.from(field, this))
                .collect(Collectors.toList());
    }

    public void save() throws IllegalAccessException, IOException {

        if (this.bindFile == null) {
            throw new IllegalAccessException("bindFile cannot be null");
        }

        if (this.configurer == null) {
            throw new IllegalAccessException("configurer cannot be null");
        }

        for (FieldDeclaration field : this.fields) {
            this.configurer.setValue(field.getName(), field.getValue());
        }

        this.configurer.writeToFile(this.bindFile);
    }

    public void load() throws IllegalAccessException, IOException {

        if (this.bindFile == null) {
            throw new IllegalAccessException("bindFile cannot be null");
        }

        if (this.configurer == null) {
            throw new IllegalAccessException("configurer cannot be null");
        }

        this.configurer.loadFromFile(this.bindFile);

        for (FieldDeclaration field : this.fields) {
            Object value = this.configurer.getValue(field.getName(), field.getType());
            field.updateValue(value);
        }
    }
}
