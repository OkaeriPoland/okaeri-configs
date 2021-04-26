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
import java.util.*;

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
        if (this.getConfigurer() != null) configurer.setRegistry(this.getConfigurer().getRegistry());
        this.setConfigurer(configurer);
        return this;
    }

    public OkaeriConfig withConfigurer(Configurer configurer, OkaeriSerdesPack... serdesPack) {
        if (this.getConfigurer() != null) configurer.setRegistry(this.getConfigurer().getRegistry());
        this.setConfigurer(configurer);
        Arrays.stream(serdesPack).forEach(this.getConfigurer()::register);
        return this;
    }

    public OkaeriConfig withSerdesPack(OkaeriSerdesPack serdesPack) {
        this.getConfigurer().register(serdesPack);
        return this;
    }

    public OkaeriConfig withBindFile(File bindFile) {
        this.setBindFile(bindFile);
        return this;
    }

    public OkaeriConfig withBindFile(String pathname) {
        this.setBindFile(new File(pathname));
        return this;
    }

    public OkaeriConfig saveDefaults() throws OkaeriException {

        if (this.getBindFile() == null) {
            throw new InitializationException("bindFile cannot be null");
        }

        if (this.getBindFile().exists()) {
            return this;
        }

        return this.save();
    }

    public void set(String key, Object value) {

        if (this.getConfigurer() == null) {
            throw new InitializationException("configurer cannot be null");
        }

        FieldDeclaration field = this.getDeclaration().getField(key).orElse(null);
        if (field != null) {
            value = this.getConfigurer().resolveType(value, GenericsDeclaration.of(value), field.getType().getType(), field.getType());
            field.updateValue(value);
        }

        GenericsDeclaration fieldGenerics = (field == null) ? null : field.getType();
        this.getConfigurer().setValue(key, value, fieldGenerics, field);
    }

    public Object get(String key) {

        if (this.getConfigurer() == null) {
            throw new InitializationException("configurer cannot be null");
        }

        FieldDeclaration field = this.getDeclaration().getField(key).orElse(null);
        if (field != null) {
            return field.getValue();
        }

        return this.getConfigurer().getValue(key);
    }

    public <T> T get(String key, Class<T> clazz) {

        if (this.getConfigurer() == null) {
            throw new InitializationException("configurer cannot be null");
        }

        FieldDeclaration field = this.getDeclaration().getField(key).orElse(null);
        if (field != null) {
            return this.getConfigurer().resolveType(field.getValue(), field.getType(), clazz, GenericsDeclaration.of(clazz));
        }

        return this.getConfigurer().getValue(key, clazz, null);
    }

    public OkaeriConfig save() throws OkaeriException {
        return this.save(this.getBindFile());
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public OkaeriConfig save(File file) throws OkaeriException {

        if (file == null) {
            throw new OkaeriException("cannot use #save(File) with null file");
        }

        try {
            File parentFile = file.getParentFile();
            if (parentFile != null) parentFile.mkdirs();
            return this.save(new PrintStream(new FileOutputStream(file, false), true, StandardCharsets.UTF_8.name()));
        } catch (FileNotFoundException | UnsupportedEncodingException exception) {
            throw new OkaeriException("failed #save using file " + file, exception);
        }
    }

    public String saveToString() throws OkaeriException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        this.save(outputStream);
        return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
    }

    public OkaeriConfig save(OutputStream outputStream) throws OkaeriException {

        if (this.getConfigurer() == null) {
            throw new InitializationException("configurer cannot be null");
        }

        for (FieldDeclaration field : this.getDeclaration().getFields()) {
            if (!this.getConfigurer().isValid(field, field.getValue())) {
                throw new ValidationException(this.getConfigurer().getClass() + " marked " + field.getName() + " as invalid without throwing an exception");
            }
            try {
                this.getConfigurer().setValue(field.getName(), field.getValue(), field.getType(), field);
            } catch (Exception exception) {
                throw new OkaeriException("failed to #setValue for " + field.getName(), exception);
            }
        }

        try {
            this.getConfigurer().write(outputStream, this.getDeclaration());
        } catch (Exception exception) {
            throw new OkaeriException("failed #write", exception);
        }

        return this;
    }

    public Map<String, Object> asMap(Configurer configurer, boolean conservative) throws OkaeriException {

        Map<String, Object> map = new LinkedHashMap<>();

        // fetch by declaration
        for (FieldDeclaration field : this.getDeclaration().getFields()) {
            Object simplified = configurer.simplify(field.getValue(), field.getType(), conservative);
            map.put(field.getName(), simplified);
        }

        // no configurer, no additional properties possible
        if (this.getConfigurer() == null) {
            return map;
        }

        // fetch remaining, non-declared
        for (String keyName : this.getConfigurer().getAllKeys()) {

            if (map.containsKey(keyName)) {
                continue;
            }

            Object value = this.getConfigurer().getValue(keyName);
            Object simplified = configurer.simplify(value, GenericsDeclaration.of(value), conservative);
            map.put(keyName, simplified);
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

        if (this.getConfigurer() == null) {
            throw new InitializationException("configurer cannot be null");
        }

        ByteArrayInputStream inputStream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
        return this.load(inputStream);
    }

    public OkaeriConfig load(InputStream inputStream) {

        if (this.getConfigurer() == null) {
            throw new InitializationException("configurer cannot be null");
        }

        try {
            this.getConfigurer().load(inputStream, this.getDeclaration());
        } catch (Exception exception) {
            throw new OkaeriException("failed #load", exception);
        }

        return this.update();
    }

    public OkaeriConfig load() throws OkaeriException {
        return this.load(this.getBindFile());
    }

    public OkaeriConfig load(File file) throws OkaeriException {

        if (file == null) {
            throw new OkaeriException("cannot use #load(File) with null file");
        }

        try {
            return this.load(new FileInputStream(file));
        } catch (FileNotFoundException exception) {
            throw new OkaeriException("failed #load using file " + file, exception);
        }
    }

    public OkaeriConfig update() throws OkaeriException {

        if (this.getDeclaration() == null) {
            throw new InitializationException("declaration cannot be null: config not initialized");
        }

        for (FieldDeclaration field : this.getDeclaration().getFields()) {

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
                        value = this.getConfigurer().resolveType(property, GenericsDeclaration.of(property), genericType.getType(), genericType);
                    } catch (Exception exception) {
                        throw new OkaeriException("failed to #resolveType for @Variable { " + variable.value() + " }", exception);
                    }
                    if (!this.getConfigurer().isValid(field, value)) {
                        throw new ValidationException(this.getConfigurer().getClass() + " marked " + field.getName() + " as invalid without throwing an exception");
                    }
                    field.updateValue(value);
                    field.setVariableHide(true);
                    updateValue = false;
                }
            }

            if (!this.getConfigurer().keyExists(fieldName)) {
                continue;
            }

            Object value;
            try {
                value = this.getConfigurer().getValue(fieldName, type, genericType);
            } catch (Exception exception) {
                throw new OkaeriException("failed to #getValue for " + fieldName, exception);
            }

            if (updateValue) {
                if (!this.getConfigurer().isValid(field, value)) {
                    throw new ValidationException(this.getConfigurer().getClass() + " marked " + field.getName() + " as invalid without throwing an exception");
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
        this.setDeclaration(ConfigDeclaration.of(this));
        return this;
    }
}
