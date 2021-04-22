package eu.okaeri.configs;

import eu.okaeri.configs.annotation.Variable;
import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.exception.InitializationException;
import eu.okaeri.configs.exception.OkaeriException;
import eu.okaeri.configs.exception.ValidationException;
import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.schema.FieldDeclaration;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.OkaeriSerdesPack;
import lombok.Getter;
import lombok.Setter;

import java.io.*;
import java.nio.charset.StandardCharsets;
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
        this.updateDeclaration();
    }

    public void setConfigurer(Configurer configurer) {
        this.configurer = configurer;
        this.configurer.setParent(this);
    }

    public OkaeriConfig withConfigurer(Configurer configurer) {
        if (this.configurer != null) configurer.setRegistry(this.configurer.getRegistry());
        this.setConfigurer(configurer);
        return this;
    }

    public OkaeriConfig withConfigurer(Configurer configurer, OkaeriSerdesPack... serdesPack) {
        if (this.configurer != null) configurer.setRegistry(this.configurer.getRegistry());
        this.setConfigurer(configurer);
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

    public OkaeriConfig saveDefaults() throws OkaeriException {

        if (this.bindFile == null) {
            throw new InitializationException("bindFile cannot be null");
        }

        if (this.bindFile.exists()) {
            return this;
        }

        return this.save();
    }

    public void set(String key, Object value) {
        if (this.configurer == null) {
            throw new InitializationException("configurer cannot be null");
        }
        FieldDeclaration field = this.declaration.getField(key).orElse(null);
        GenericsDeclaration fieldGenerics = this.declaration.getGenericsOrNull(key);
        this.configurer.setValue(key, value, fieldGenerics, field);
    }

    public Object get(String key) {
        if (this.configurer == null) {
            throw new InitializationException("configurer cannot be null");
        }
        return this.configurer.getValue(key);
    }

    public <T> T get(String key, Class<T> clazz) {
        if (this.configurer == null) {
            throw new InitializationException("configurer cannot be null");
        }
        return this.configurer.getValue(key, clazz, null);
    }

    public OkaeriConfig save() throws OkaeriException {

        if (this.bindFile == null) {
            throw new OkaeriException("cannot use #save() without bindFile specified");
        }

        try {
            File parentFile = this.bindFile.getParentFile();
            if (parentFile != null) parentFile.mkdirs();
            return this.save(new FileOutputStream(this.bindFile, false));
        } catch (FileNotFoundException exception) {
            throw new OkaeriException("failed #save using file " + this.bindFile, exception);
        }
    }

    public String saveToString() throws OkaeriException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        this.save(outputStream);
        return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
    }

    public OkaeriConfig save(OutputStream outputStream) throws OkaeriException {

        if (this.configurer == null) {
            throw new InitializationException("configurer cannot be null");
        }

        for (FieldDeclaration field : this.declaration.getFields()) {
            if (!this.configurer.isValid(field, field.getValue())) {
                throw new ValidationException(this.configurer.getClass() + " marked " + field.getName() + " as invalid without throwing an exception");
            }
            try {
                this.configurer.setValue(field.getName(), field.getValue(), field.getType(), field);
            } catch (Exception exception) {
                throw new OkaeriException("failed to #setValue for " + field.getName(), exception);
            }
        }

        try {
            this.configurer.write(outputStream, this.declaration);
        } catch (Exception exception) {
            throw new OkaeriException("failed #write", exception);
        }

        return this;
    }

    public Map<String, Object> asMap(Configurer configurer, boolean conservative) throws OkaeriException {

        Map<String, Object> map = new LinkedHashMap<>();
        for (FieldDeclaration field : this.declaration.getFields()) {
            Object simplified = configurer.simplify(field.getValue(), field.getType(), conservative);
            map.put(field.getName(), simplified);
        }

        return map;
    }

    public OkaeriConfig load(boolean update) throws OkaeriException {
        this.load();
        if (update) {
            this.save();
        }
        return this;
    }

    public OkaeriConfig load(String data) {

        if (this.configurer == null) {
            throw new InitializationException("configurer cannot be null");
        }

        ByteArrayInputStream inputStream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
        return this.load(inputStream);
    }

    public OkaeriConfig load(InputStream inputStream) {

        if (this.configurer == null) {
            throw new InitializationException("configurer cannot be null");
        }

        try {
            this.configurer.load(inputStream, this.declaration);
        } catch (Exception exception) {
            throw new OkaeriException("failed #load", exception);
        }

        return this.update();
    }

    public OkaeriConfig load() throws OkaeriException {

        if (this.bindFile == null) {
            throw new OkaeriException("cannot use #load() without bindFile specified");
        }

        try {
            return this.load(new FileInputStream(this.bindFile));
        } catch (FileNotFoundException exception) {
            throw new OkaeriException("failed #load using file " + this.bindFile, exception);
        }
    }

    public OkaeriConfig update() throws OkaeriException {

        if (this.declaration == null) {
            throw new InitializationException("declaration cannot be null: config not initialized");
        }

        for (FieldDeclaration field : this.declaration.getFields()) {

            String fieldName = field.getName();
            GenericsDeclaration genericType = field.getType();
            Class<?> type = field.getType().getType();
            Variable variable = field.getVariable();
            boolean updateValue = true;

            if (variable != null) {
                String property = this.getPropertyOrEnv(variable.value());
                if (property != null) {
                    Object value;
                    try {
                        value = this.configurer.resolveType(property, GenericsDeclaration.of(property), genericType.getType(), genericType);
                    } catch (Exception exception) {
                        throw new OkaeriException("failed to #resolveType for @Variable { " + variable.value() + " }", exception);
                    }
                    if (!this.configurer.isValid(field, value)) {
                        throw new ValidationException(this.configurer.getClass() + " marked " + field.getName() + " as invalid without throwing an exception");
                    }
                    field.updateValue(value);
                    updateValue = false;
                }
            }

            if (!this.configurer.keyExists(fieldName)) {
                continue;
            }

            Object value;
            try {
                value = this.configurer.getValue(fieldName, type, genericType);
            } catch (Exception exception) {
                throw new OkaeriException("failed to #getValue for " + fieldName, exception);
            }

            if (updateValue) {
                if (!this.configurer.isValid(field, value)) {
                    throw new ValidationException(this.configurer.getClass() + " marked " + field.getName() + " as invalid without throwing an exception");
                }
                field.updateValue(value);
            }

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
