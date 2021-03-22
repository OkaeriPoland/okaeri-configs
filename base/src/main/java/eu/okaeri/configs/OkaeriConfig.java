package eu.okaeri.configs;

import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.schema.FieldDeclaration;
import eu.okaeri.configs.schema.GenericsDeclaration;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import java.io.File;
import java.io.IOException;


public abstract class OkaeriConfig {

    @Getter @Setter private File bindFile;
    @Getter @Setter private Configurer configurer;
    private ConfigDeclaration declaration;

    public OkaeriConfig() {
        this.declaration = ConfigDeclaration.from(this);
    }

    public OkaeriConfig withConfigurer(Configurer configurer) {
        this.configurer = configurer;
        return this;
    }

    public OkaeriConfig withBindFile(File bindFile) {
        this.bindFile = bindFile;
        return this;
    }

    public OkaeriConfig withBindFile(String pathname) {
        this.bindFile = new File(pathname);
        return this;
    }

    public OkaeriConfig saveDefaults() throws IOException, IllegalAccessException {

        if (this.bindFile == null) {
            throw new IllegalAccessException("bindFile cannot be null");
        }

        if (this.bindFile.exists()) {
            return this;
        }

        return this.save();
    }

    @SneakyThrows
    public void set(String key, Object value) {
        if (this.configurer == null) {
            throw new IllegalAccessException("configurer cannot be null");
        }
        this.configurer.setValue(key, value, this.declaration.getFieldDeclarationOrNull(key));
    }

    @SneakyThrows
    public Object get(String key) {
        if (this.configurer == null) {
            throw new IllegalAccessException("configurer cannot be null");
        }
        return this.configurer.getValue(key);
    }

    @SneakyThrows
    public <T> T get(String key, Class<T> clazz) {
        if (this.configurer == null) {
            throw new IllegalAccessException("configurer cannot be null");
        }
        return this.configurer.getValue(key, clazz, null);
    }

    public OkaeriConfig save() throws IllegalAccessException, IOException {

        if (this.bindFile == null) {
            throw new IllegalAccessException("bindFile cannot be null");
        }

        if (this.configurer == null) {
            throw new IllegalAccessException("configurer cannot be null");
        }

        for (FieldDeclaration field : this.declaration.getFields()) {
            this.configurer.setValue(field.getName(), field.getValue(), field.getType());
        }

        this.configurer.writeToFile(this.bindFile, this.declaration);
        return this;
    }

    public OkaeriConfig load(boolean update) throws IllegalAccessException, IOException {
        this.load();
        if (update) {
            this.save();
        }
        return this;
    }

    public OkaeriConfig load() throws IllegalAccessException, IOException {

        if (this.bindFile == null) {
            throw new IllegalAccessException("bindFile cannot be null");
        }

        if (this.configurer == null) {
            throw new IllegalAccessException("configurer cannot be null");
        }

        this.configurer.loadFromFile(this.bindFile, this.declaration);

        for (FieldDeclaration field : this.declaration.getFields()) {
            String fieldName = field.getName();
            if (!this.configurer.keyExists(fieldName)) {
                continue;
            }
            GenericsDeclaration type = field.getType();
            GenericsDeclaration genericType = field.getType();
            Object value = this.configurer.getValue(fieldName, type.getType(), genericType);
            field.updateValue(value);
        }

        return this;
    }
}
