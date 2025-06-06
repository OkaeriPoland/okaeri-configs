package eu.okaeri.configs;

import eu.okaeri.configs.annotation.Variable;
import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.exception.InitializationException;
import eu.okaeri.configs.exception.OkaeriException;
import eu.okaeri.configs.exception.ValidationException;
import eu.okaeri.configs.migrate.ConfigMigration;
import eu.okaeri.configs.migrate.builtin.NamedMigration;
import eu.okaeri.configs.migrate.view.RawConfigView;
import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.schema.FieldDeclaration;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.OkaeriSerdesPack;
import eu.okaeri.configs.serdes.SerdesContext;
import eu.okaeri.configs.serdes.SerdesRegistry;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Logger;

public abstract class OkaeriConfig {

    @Setter
    private Logger logger = Logger.getLogger(this.getClass().getSimpleName());

    @Getter
    @Setter(AccessLevel.PROTECTED)
    private Path bindFile;

    @Getter
    private Configurer configurer;

    @Setter(AccessLevel.PROTECTED)
    private ConfigDeclaration declaration;

    @Getter
    @Setter(AccessLevel.PROTECTED)
    private boolean removeOrphans = false;

    /**
     * Creates a new config instance. Note that the declaration initialization
     * is now deferred until first access via {@link #getDeclaration()}.
     * <p>
     * This ensures all field initializers are properly set before reflection
     * reads their values, preventing null {@code startingValue} in field declarations.
     */
    public OkaeriConfig() {}

    /**
     * Replaces the current configurer with the provided.
     * Sets parent field of the configurer to this instance.
     *
     * @param configurer new configurer
     */
    public void setConfigurer(@NonNull Configurer configurer) {
        this.configurer = configurer;
        this.configurer.setParent(this);
    }

    /**
     * Replaces the current configurer with the provided, assigning previous
     * {@link SerdesRegistry} to the new one if available.
     * <p>
     * This method allows easy change of the configuration backend without
     * the need to re-register serdes packs. To preserve the registry of
     * the new configurer use {@link #setConfigurer(Configurer)}.
     *
     * @param configurer the new configurer
     * @return this instance
     */
    public OkaeriConfig withConfigurer(@NonNull Configurer configurer) {
        if (this.getConfigurer() != null) configurer.setRegistry(this.getConfigurer().getRegistry());
        this.setConfigurer(configurer);
        return this;
    }

    /**
     * Replaces the current configurer with the provided, assigning previous
     * {@link SerdesRegistry} to the new one if available.
     * <p>
     * Registers provided serdes packs in the configurer afterwards.
     * To preserve the registry of the new configurer use {@link #setConfigurer(Configurer)}.
     *
     * @param configurer the new configurer
     * @param serdesPack the array of serdes packs to be registered
     * @return this instance
     */
    public OkaeriConfig withConfigurer(@NonNull Configurer configurer, @NonNull OkaeriSerdesPack... serdesPack) {
        if (this.getConfigurer() != null) configurer.setRegistry(this.getConfigurer().getRegistry());
        this.setConfigurer(configurer);
        Arrays.stream(serdesPack).forEach(this.getConfigurer()::register);
        return this;
    }

    /**
     * Registers provided serdes pack in the current configurer.
     *
     * @param serdesPack the serdes pack
     * @return this instance
     */
    public OkaeriConfig withSerdesPack(@NonNull OkaeriSerdesPack serdesPack) {

        if (this.getConfigurer() == null) {
            throw new InitializationException("configurer cannot be null");
        }

        this.getConfigurer().register(serdesPack);
        return this;
    }

    /**
     * Sets related configuration {@link Path} from {@link File}.
     *
     * @param bindFile the bind file
     * @return this instance
     */
    public OkaeriConfig withBindFile(@NonNull File bindFile) {
        this.setBindFile(bindFile.toPath());
        return this;
    }

    /**
     * Sets related configuration {@link Path}.
     * Same effect as {@link #withBindFile(File)}.
     *
     * @param path the bind file path
     * @return this instance
     */
    public OkaeriConfig withBindFile(@NonNull Path path) {
        this.setBindFile(path);
        return this;
    }

    /**
     * Sets related configuration {@link Path} using its pathname.
     * Same as {@link #withBindFile(Path)} with {@code Paths.get(pathname)}.
     *
     * @param pathname the bind file path
     * @return this instance
     */
    public OkaeriConfig withBindFile(@NonNull String pathname) {
        this.setBindFile(Paths.get(pathname));
        return this;
    }

    /**
     * Replaces the current local logger with the provided.
     *
     * @param logger the logger
     * @return this instance
     */
    public OkaeriConfig withLogger(@NonNull Logger logger) {
        this.setLogger(logger);
        return this;
    }

    /**
     * Sets the state of automatic orphan removal
     * - True: orphaned keys would be explicitly removed
     * - False: it is up to configurer to manage orphaned keys
     *
     * @param removeOrphans new state
     * @return this instance
     */
    public OkaeriConfig withRemoveOrphans(boolean removeOrphans) {
        this.setRemoveOrphans(removeOrphans);
        return this;
    }

    /**
     * Saves current configuration state to the bindFile if file does not exist.
     *
     * @return this instance
     * @throws OkaeriException if bindFile is null or saving fails
     */
    public OkaeriConfig saveDefaults() throws OkaeriException {

        if (this.getBindFile() == null) {
            throw new InitializationException("bindFile cannot be null");
        }

        if (Files.exists(this.getBindFile())) {
            return this;
        }

        return this.save();
    }

    /**
     * Updates configuration value by its raw key. Attempts serialization/deserialization and other
     * transformation techniques if necessary to match field type (e.g. String to int).
     *
     * @param key   target key
     * @param value target value
     * @throws OkaeriException if {@link #configurer} is null or value processing fails
     */
    public void set(@NonNull String key, Object value) throws OkaeriException {

        if (this.getConfigurer() == null) {
            throw new InitializationException("configurer cannot be null");
        }

        FieldDeclaration field = this.getDeclaration().getField(key).orElse(null);
        if (field != null) {
            value = this.getConfigurer().resolveType(value, GenericsDeclaration.of(value), field.getType().getType(), field.getType(), SerdesContext.of(this.configurer, field));
            field.updateValue(value);
        }

        GenericsDeclaration fieldGenerics = (field == null) ? null : field.getType();
        this.getConfigurer().setValue(key, value, fieldGenerics, field);
    }

    /**
     * Gets configuration value by its raw key.
     *
     * @param key target key
     * @return the resolved value
     * @throws OkaeriException if {@link #configurer} is null or getting the value fails
     */
    public Object get(@NonNull String key) throws OkaeriException {

        if (this.getConfigurer() == null) {
            throw new InitializationException("configurer cannot be null");
        }

        FieldDeclaration field = this.getDeclaration().getField(key).orElse(null);
        if (field != null) {
            return field.getValue();
        }

        return this.getConfigurer().getValue(key);
    }

    /**
     * Gets configuration value by its raw key. Attempts serialization/deserialization and other
     * transformation techniques if necessary to match provided class type (e.g. int = String).
     *
     * @param key   target key
     * @param clazz target class for value
     * @param <T>   target value type
     * @return the resolved value
     * @throws OkaeriException if {@link #configurer} is null or the value processing fails
     */
    public <T> T get(@NonNull String key, @NonNull Class<T> clazz) throws OkaeriException {
        return this.get(key, GenericsDeclaration.of(clazz));
    }

    /**
     * Gets configuration value by its raw key. Attempts serialization/deserialization and other
     * transformation techniques if necessary to match provided class type (e.g. int = String).
     *
     * @param key   target key
     * @param generics target type for value
     * @param <T>   target value type
     * @return the resolved value
     * @throws OkaeriException if {@link #configurer} is null or the value processing fails
     */
    @SuppressWarnings("unchecked")
    public <T> T get(@NonNull String key, @NonNull GenericsDeclaration generics) throws OkaeriException {

        if (this.getConfigurer() == null) {
            throw new InitializationException("configurer cannot be null");
        }

        FieldDeclaration field = this.getDeclaration().getField(key).orElse(null);
        if (field != null) {
            return (T) this.getConfigurer().resolveType(field.getValue(), field.getType(), generics.getType(), generics, SerdesContext.of(this.configurer, field));
        }

        return (T) this.getConfigurer().getValue(key, generics.getType(), null, SerdesContext.of(this.configurer));
    }

    /**
     * Saves current configuration state to the bindFile.
     *
     * @return this instance
     * @throws OkaeriException if {@link #configurer} or {@link #bindFile} is null or saving fails
     */
    public OkaeriConfig save() throws OkaeriException {
        return this.save(this.getBindFile());
    }

    /**
     * Saves current configuration state to the specific file.
     *
     * @param file target file
     * @return this instance
     * @throws OkaeriException if {@link #configurer} is null or saving fails
     */
    public OkaeriConfig save(@NonNull File file) throws OkaeriException {
        try {
            File parentFile = file.getParentFile();
            if (parentFile != null) parentFile.mkdirs();
            return this.save(new PrintStream(new FileOutputStream(file, false), true, StandardCharsets.UTF_8.name()));
        } catch (FileNotFoundException | UnsupportedEncodingException exception) {
            throw new OkaeriException("failed #save using file " + file, exception);
        }
    }

    /**
     * Saves current configuration state to the specific path.
     *
     * @param path target path
     * @return this instance
     * @throws OkaeriException if {@link #configurer} is null or saving fails
     */
    public OkaeriConfig save(@NonNull Path path) throws OkaeriException {
        return this.save(path.toFile());
    }

    /**
     * Saves current configuration state as {@link String}.
     *
     * @return saved configuration text
     * @throws OkaeriException if {@link #configurer} is null or saving fails
     */
    public String saveToString() throws OkaeriException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        this.save(outputStream);
        return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
    }

    /**
     * Saves current configuration state to the provided {@link OutputStream}.
     *
     * @param outputStream target output
     * @return this instance
     * @throws OkaeriException if {@link #configurer} is null or saving fails
     */
    public OkaeriConfig save(@NonNull OutputStream outputStream) throws OkaeriException {

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
            Set<String> orphans = this.getConfigurer().sort(this.getDeclaration());
            if (!orphans.isEmpty() && this.isRemoveOrphans()) {
                this.logger.warning("Removed orphaned (undeclared) keys: " + orphans);
                orphans.forEach(orphan -> this.getConfigurer().remove(orphan));
            }
            this.getConfigurer().write(outputStream, this.getDeclaration());
        } catch (Exception exception) {
            throw new OkaeriException("failed #write", exception);
        }

        return this;
    }

    /**
     * Converts current configuration state to map. Values are subject to the simplification process.
     * <p>
     * Result may depend on the used Configurer backend. General rule is that non-conservative simplification
     * provides almost everything as Strings while conservative one preserves most of the primitive types.
     * <p>
     * Notable differences:
     * - some formats may not support maps with non-string keys
     * - some formats may not support primitives like char
     *
     * @param configurer   configurer to be used for simplification
     * @param conservative should basic types be preserved
     * @return resulting map
     * @throws OkaeriException if simplification process fails
     */
    public Map<String, Object> asMap(@NonNull Configurer configurer, boolean conservative) throws OkaeriException {

        Map<String, Object> map = new LinkedHashMap<>();

        // fetch by declaration
        for (FieldDeclaration field : this.getDeclaration().getFields()) {
            Object simplified = configurer.simplify(field.getValue(), field.getType(), SerdesContext.of(configurer, field), conservative);
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
            Object simplified = configurer.simplify(value, GenericsDeclaration.of(value), SerdesContext.of(configurer, null), conservative);
            map.put(keyName, simplified);
        }

        return map;
    }

    /**
     * Loads new state to the configuration from its {@link #bindFile}.
     *
     * @param update should configuration be saved afterwards
     * @return this instance
     * @throws OkaeriException if {@link #configurer} or {@link #bindFile} is null or loading fails
     */
    public OkaeriConfig load(boolean update) throws OkaeriException {
        this.load();
        if (update) {
            this.save();
        }
        return this;
    }

    /**
     * Loads new state to the configuration from the provided {@link String}.
     *
     * @param data text representation of the configuration
     * @return this instance
     * @throws OkaeriException if {@link #configurer} is null or loading fails
     */
    public OkaeriConfig load(@NonNull String data) throws OkaeriException {

        if (this.getConfigurer() == null) {
            throw new InitializationException("configurer cannot be null");
        }

        ByteArrayInputStream inputStream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
        return this.load(inputStream);
    }

    /**
     * Loads new state to the configuration from the provided {@link InputStream}.
     *
     * @param inputStream source input
     * @return this instance
     * @throws OkaeriException if {@link #configurer} is null or loading fails
     */
    public OkaeriConfig load(@NonNull InputStream inputStream) throws OkaeriException {

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

    /**
     * Loads new state to the configuration from its {@link #bindFile}.
     *
     * @return this instance
     * @throws OkaeriException if {@link #configurer} or {@link #bindFile} is null or loading fails
     */
    public OkaeriConfig load() throws OkaeriException {
        return this.load(this.getBindFile());
    }

    /**
     * Loads new state to the configuration from the specified file.
     *
     * @param file source file
     * @return this instance
     * @throws OkaeriException if {@link #configurer} or {@link #bindFile} is null or loading fails
     */
    public OkaeriConfig load(@NonNull File file) throws OkaeriException {
        try {
            return this.load(new FileInputStream(file));
        } catch (FileNotFoundException exception) {
            throw new OkaeriException("failed #load using file " + file, exception);
        }
    }

    /**
     * Loads new state to the configuration from the specified path.
     *
     * @param path source path
     * @return this instance
     * @throws OkaeriException if {@link #configurer} or {@link #bindFile} is null or loading fails
     */
    public OkaeriConfig load(@NonNull Path path) throws OkaeriException {
        return this.load(path.toFile());
    }

    /**
     * Loads new state to the configuration from the specified map.
     *
     * @param map source map
     * @return this instance
     * @throws OkaeriException if {@link #configurer} is null or loading fails
     */
    public OkaeriConfig load(@NonNull Map<String, Object> map) throws OkaeriException {

        if (this.getConfigurer() == null) {
            throw new InitializationException("configurer cannot be null");
        }

        map.forEach(this::set);
        return this;
    }

    /**
     * Loads new state to the configuration from the other config.
     *
     * @param otherConfig source config
     * @return this instance
     * @throws OkaeriException if {@link #configurer} is null or loading fails
     */
    public OkaeriConfig load(@NonNull OkaeriConfig otherConfig) throws OkaeriException {

        if (this.getConfigurer() == null) {
            throw new InitializationException("configurer cannot be null");
        }

        return this.load(otherConfig.asMap(this.getConfigurer(), true));
    }

    /**
     * Performs migrations and confirms the data is still loadable afterwards,
     * then executes {@link #save()}.
     *
     * @param migrations migrations to be performed
     * @return this instance
     * @throws OkaeriException if {@link #configurer} is null or migration fails
     */
    public OkaeriConfig migrate(@NonNull ConfigMigration... migrations) throws OkaeriException {
       return this.migrate(
           (performed) -> {
               try {
                   this.load(this.saveToString());
               } catch (OkaeriException exception) {
                   throw new OkaeriException("failed #migrate due to load error after migrations (not saving)", exception);
               }
               this.save();
           },
           migrations
       );
    }

    /**
     * Performs migrations and invokes consumer if performed migrations
     * count is greater than zero.
     *
     * @param callback performed migrations count consumer
     * @param migrations migrations to be performed
     * @return this instance
     * @throws OkaeriException if {@link #configurer} is null or migration fails
     */
    public OkaeriConfig migrate(@NonNull Consumer<Long> callback, @NonNull ConfigMigration... migrations) throws OkaeriException {
        RawConfigView view = new RawConfigView(this);
        long performed = Arrays.stream(migrations)
            .filter(migration -> {
                try {
                    return migration.migrate(this, view);
                } catch (Exception exception) {
                    throw new OkaeriException("migrate failure in " + migration.getClass().getName(), exception);
                }
            })
            .peek(migration -> {
                if (migration instanceof NamedMigration) {
                    String name = migration.getClass().getSimpleName();
                    String description = ((NamedMigration) migration).getDescription();
                    this.logger.info(name + ": " + description);
                }
            })
            .count();
        if (performed > 0) {
            callback.accept(performed);
        }
        return this;
    }

    /**
     * Updates state of the configuration with values provided by the Configurer,
     * also applying {@link Variable} annotation to the fields if present.
     *
     * @return this instance
     * @throws OkaeriException if {@link #declaration} is null or update fails
     */
    public OkaeriConfig update() throws OkaeriException {

        if (this.getDeclaration() == null) {
            throw new InitializationException("declaration cannot be null: config not initialized");
        }

        for (FieldDeclaration field : this.getDeclaration().getFields()) {

            String fieldName = field.getName();
            GenericsDeclaration genericType = field.getType();
            Class<?> type = field.getType().getType();
            Object fieldValue = field.getValue();
            Variable variable = field.getVariable();
            boolean updateValue = true;

            if (variable != null) {

                String rawProperty = System.getProperty(variable.value());
                String property = (rawProperty == null) ? System.getenv(variable.value()) : rawProperty;

                if (property != null) {

                    Object value;
                    try {
                        value = this.getConfigurer().resolveType(property, GenericsDeclaration.of(property), genericType.getType(), genericType, SerdesContext.of(this.configurer, field));
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
                value = this.getConfigurer().getValue(fieldName, type, genericType, SerdesContext.of(this.configurer, field));
                if (OkaeriConfig.class.isAssignableFrom(type)) {
                    ((OkaeriConfig) value).setExcludedFieldsBy((OkaeriConfig) fieldValue);
                }
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

    /**
     * Gets the configuration declaration, initializing it if necessary.
     * <p>
     * The declaration is lazily initialized on first access to ensure all field values
     * are properly set before being read via reflection. This prevents null
     * {@code startingValue} issues in field declarations.
     *
     * @return the configuration declaration (never null after first access)
     * @see #updateDeclaration()
     */
    public ConfigDeclaration getDeclaration() {
        if (this.declaration == null) {
            this.updateDeclaration();
        }
        return this.declaration;
    }

    /**
     * Updates the configuration declaration with the new one.
     * Useful in situations where constructor may not be invoked.
     *
     * @return this instance
     */
    public OkaeriConfig updateDeclaration() {
        this.setDeclaration(ConfigDeclaration.of(this));
        return this;
    }

    public OkaeriConfig setExcludedFieldsBy(OkaeriConfig source) {
        for (FieldDeclaration field : this.getDeclaration().getExcludedFields()) {
            Field targetField = field.getField();
            targetField.setAccessible(true);
            try {
                field.updateValue(targetField.get(source));
            } catch (IllegalAccessException e) {
                this.logger.warning("Unable to access field: " + targetField.getName() + " due to access restrictions.");
            }
        }
        return this;
    }
}
