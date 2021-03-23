package eu.okaeri.configs;

import eu.okaeri.configs.annotation.Variable;
import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.schema.FieldDeclaration;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.OkaeriSerdesPack;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
public abstract class OkaeriConfig {

    private File bindFile;
    private Configurer configurer;
    private ConfigDeclaration declaration;

    public OkaeriConfig() {
    }

    public OkaeriConfig withConfigurer(Configurer configurer) {
        this.configurer = configurer;
        return this;
    }

    public OkaeriConfig withConfigurer(Configurer configurer, OkaeriSerdesPack... serdesPack) {
        this.configurer = configurer;
        Arrays.stream(serdesPack).forEach(this.configurer::register);
        return this;
    }

    public OkaeriConfig withSerdesPack(OkaeriSerdesPack serdesPack) {
        this.configurer.register(serdesPack);
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

    public Map<String, Object> asMap(Configurer configurer) throws IllegalAccessException {

        Map<String, Object> map = new LinkedHashMap<>();
        for (FieldDeclaration field : this.declaration.getFields()) {
            Object simplified = configurer.simplify(field.getValue(), field.getType());
            map.put(field.getName(), simplified);
        }

        return map;
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
        return this.update();
    }

    public OkaeriConfig update() throws IllegalAccessException {

        for (FieldDeclaration field : this.declaration.getFields()) {

            String fieldName = field.getName();
            GenericsDeclaration genericType = field.getType();
            Class<?> type = field.getType().getType();
            Variable variable = field.getVariable();
            boolean updateValue = true;

            if (variable != null) {
                String property = this.getPropertyOrEnv(variable.value());
                if (property != null) {
                    Object value = this.configurer.resolveType(property, GenericsDeclaration.of(property), genericType.getType(), genericType);
                    field.updateValue(value);
                    updateValue = false;
                }
            }

            if (!this.configurer.keyExists(fieldName)) {
                continue;
            }

            Object value = this.configurer.getValue(fieldName, type, genericType);
            if (updateValue) field.updateValue(value);
            field.setStartingValue(value);
        }

        return this;
    }

    private String getPropertyOrEnv(String name) {
        String property = System.getProperty(name);
        return (property == null) ? System.getenv(name) : property;
    }

    public OkaeriConfig updateDeclaration() {
        this.declaration = ConfigDeclaration.of(this);
        return this;
    }
}
