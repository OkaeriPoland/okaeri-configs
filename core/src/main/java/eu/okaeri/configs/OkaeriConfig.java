package eu.okaeri.configs;

import eu.okaeri.configs.annotation.ReadOnly;
import eu.okaeri.configs.annotation.Variable;
import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.exception.OkaeriConfigException;
import eu.okaeri.configs.exception.OkaeriException;
import eu.okaeri.configs.migrate.ConfigMigration;
import eu.okaeri.configs.migrate.builtin.NamedMigration;
import eu.okaeri.configs.migrate.view.RawConfigView;
import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.schema.FieldDeclaration;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.ConfigPath;
import eu.okaeri.configs.serdes.OkaeriSerdes;
import eu.okaeri.configs.serdes.SerdesContext;
import eu.okaeri.configs.serdes.SerdesRegistry;
import lombok.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Logger;

@NoArgsConstructor
public abstract class OkaeriConfig {

    @Getter
    @Setter(AccessLevel.PROTECTED)
    private Path bindFile;

    @Getter
    private Configurer configurer;

    @Setter(AccessLevel.PROTECTED)
    private ConfigDeclaration declaration;

    /**
     * Shared context for the configuration tree.
     * Root configs create their own context, subconfigs share parent's context.
     */
    @Getter
    @Setter
    private ConfigContext context;

    /**
     * Internal state holding the raw configuration data.
     * This is the parsed data from file or the data to be written.
     * Used for orphan preservation and subconfig data storage.
     */
    @Getter
    @Setter
    private Map<String, Object> internalState = new LinkedHashMap<>();

    /**
     * This config's location within the config tree.
     * For root configs this is empty, for subconfigs it's the path from root
     * (e.g., "database.connection"). Used to build full field paths in error messages.
     */
    @Getter
    @Setter
    private ConfigPath internalPath = ConfigPath.root();

    /**
     * Checks if this config is a subconfig (nested within another config).
     * A subconfig has no direct configurer but shares context with the root.
     *
     * @return true if this is a subconfig
     */
    public boolean isSubconfig() {
        return (this.configurer == null) && (this.context != null);
    }

    /**
     * Gets the effective configurer for this config.
     * For root configs, returns the direct configurer.
     * For subconfigs, returns the root config's configurer via context.
     *
     * @return the effective configurer, or null if not configured
     */
    public Configurer getEffectiveConfigurer() {
        if (this.configurer != null) {
            return this.configurer;
        }
        if (this.context != null) {
            return this.context.getRootConfig().getConfigurer();
        }
        return null;
    }

    /**
     * Gets the source file name for error reporting.
     * For root configs, extracts filename from bindFile.
     * For subconfigs, delegates to root config via context.
     *
     * @return the source file name, or null if no bind file is set
     */
    public String getBindFileName() {
        Path file = this.bindFile;
        if ((file == null) && (this.context != null)) {
            file = this.context.getRootConfig().getBindFile();
        }
        if (file != null) {
            Path fileName = file.getFileName();
            return (fileName != null) ? fileName.toString() : null;
        }
        return null;
    }

    /**
     * Gets the configuration declaration, initializing it lazily on first access.
     *
     * @return the config declaration
     */
    public ConfigDeclaration getDeclaration() {
        if (this.declaration == null) {
            this.declaration = ConfigDeclaration.of(this);
        }
        return this.declaration;
    }

    /**
     * Updates the configuration declaration with the new one.
     * Useful in situations where constructor may not be invoked.
     *
     * @return this instance
     * @deprecated Declaration is now initialized lazily via {@link #getDeclaration()}.
     * This method is no longer needed and will be removed in a future version.
     */
    @Deprecated
    public OkaeriConfig updateDeclaration() {
        this.setDeclaration(ConfigDeclaration.of(this));
        return this;
    }

    /**
     * Replaces the current configurer with the provided.
     * Sets parent field of the configurer to this instance.
     * Also initializes the context if this is a root config.
     *
     * @param configurer new configurer
     */
    public void setConfigurer(@NonNull Configurer configurer) {
        this.configurer = configurer;
        // Initialize context for root configs (non-subconfigs)
        if ((this.context == null) && !this.isSubconfig()) {
            this.context = new ConfigContext(this);
        }
    }

    /**
     * Configures this instance using a lambda-based fluent API.
     * <p>
     * This is the recommended way to configure {@link OkaeriConfig} instances.
     * It provides a clean, scoped and reusable configuration block.
     * <p>
     * Example usage:
     * <pre>{@code
     * TestConfig config = ConfigManager.create(TestConfig.class, it -> {
     *     it.configure(opt -> {
     *         opt.configurer(new YamlBukkitConfigurer(), new SerdesBukkit());
     *         opt.bindFile(new File(this.getDataFolder(), "config.yml"));
     *         opt.removeOrphans(true);
     *     });
     *     it.saveDefaults();
     *     it.load(true);
     * });
     * }</pre>
     *
     * @param configurator the configuration lambda
     * @return this instance
     * @see OkaeriConfigOptions
     */
    public OkaeriConfig configure(@NonNull Consumer<OkaeriConfigOptions> configurator) {
        OkaeriConfigOptions configurer = new OkaeriConfigOptions(this);
        configurator.accept(configurer);
        return this;
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
     * @deprecated Use {@link #configure(Consumer)} instead. This method will be removed in a future version.
     */
    @Deprecated
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
     * @deprecated Use {@link #configure(Consumer)} instead. This method will be removed in a future version.
     */
    @Deprecated
    public OkaeriConfig withConfigurer(@NonNull Configurer configurer, @NonNull OkaeriSerdes... serdesPack) {
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
     * @deprecated Use {@link #configure(Consumer)} instead. This method will be removed in a future version.
     */
    @Deprecated
    public OkaeriConfig withSerdesPack(@NonNull OkaeriSerdes serdesPack) {

        if (this.getConfigurer() == null) {
            throw new IllegalStateException("configurer cannot be null");
        }

        this.getConfigurer().register(serdesPack);
        return this;
    }

    /**
     * Sets related configuration {@link Path} from {@link File}.
     *
     * @param bindFile the bind file
     * @return this instance
     * @deprecated Use {@link #configure(Consumer)} instead. This method will be removed in a future version.
     */
    @Deprecated
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
     * @deprecated Use {@link #configure(Consumer)} instead. This method will be removed in a future version.
     */
    @Deprecated
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
     * @deprecated Use {@link #configure(Consumer)} instead. This method will be removed in a future version.
     */
    @Deprecated
    public OkaeriConfig withBindFile(@NonNull String pathname) {
        this.setBindFile(Paths.get(pathname));
        return this;
    }

    /**
     * Replaces the current local logger with the provided.
     *
     * @param logger the logger
     * @return this instance
     * @deprecated Use {@link #configure(Consumer)} instead. This method will be removed in a future version.
     */
    @Deprecated
    public OkaeriConfig withLogger(@NonNull Logger logger) {
        if (this.context != null) {
            this.context.setLogger(logger);
        }
        return this;
    }

    /**
     * Sets the state of automatic orphan removal
     * - True: orphaned keys would be explicitly removed
     * - False: it is up to configurer to manage orphaned keys
     *
     * @param removeOrphans new state
     * @return this instance
     * @deprecated Use {@link #configure(Consumer)} instead. This method will be removed in a future version.
     */
    @Deprecated
    public OkaeriConfig withRemoveOrphans(boolean removeOrphans) {
        if (this.context != null) {
            this.context.setRemoveOrphans(removeOrphans);
        }
        return this;
    }

    /**
     * Updates configuration value by its raw key. Attempts serialization/deserialization and other
     * transformation techniques if necessary to match field type (e.g. String to int).
     *
     * @param key   target key
     * @param value target value
     * @throws OkaeriException if no configurer is available or value processing fails
     */
    public void set(@NonNull String key, Object value) throws OkaeriException {

        Configurer effectiveConfigurer = this.getEffectiveConfigurer();
        if (effectiveConfigurer == null) {
            throw new IllegalStateException("configurer cannot be null");
        }

        FieldDeclaration field = this.getDeclaration().getField(key).orElse(null);
        if (field != null) {
            value = effectiveConfigurer.resolveType(value, GenericsDeclaration.of(value), field.getType().getType(), field.getType(), SerdesContext.of(effectiveConfigurer, this.context, field));
            field.updateValue(value);
        }

        // Store in internalState for non-declared keys (orphans)
        GenericsDeclaration fieldGenerics = (field == null) ? null : field.getType();
        Object simplified = effectiveConfigurer.simplifyField(value, fieldGenerics, field, this.context);
        this.internalState.put(key, simplified);
    }

    /**
     * Gets configuration value by its raw key.
     *
     * @param key target key
     * @return the resolved value
     * @throws OkaeriException if getting the value fails
     */
    public Object get(@NonNull String key) throws OkaeriException {

        FieldDeclaration field = this.getDeclaration().getField(key).orElse(null);
        if (field != null) {
            return field.getValue();
        }

        // Return from internalState for non-declared keys
        return this.internalState.get(key);
    }

    /**
     * Gets configuration value by its raw key. Attempts serialization/deserialization and other
     * transformation techniques if necessary to match provided class type (e.g. int = String).
     *
     * @param key   target key
     * @param clazz target class for value
     * @param <T>   target value type
     * @return the resolved value
     * @throws OkaeriException if no configurer is available or the value processing fails
     */
    public <T> T get(@NonNull String key, @NonNull Class<T> clazz) throws OkaeriException {
        return this.get(key, GenericsDeclaration.of(clazz));
    }

    /**
     * Gets configuration value by its raw key. Attempts serialization/deserialization and other
     * transformation techniques if necessary to match provided class type (e.g. int = String).
     *
     * @param key      target key
     * @param generics target type for value
     * @param <T>      target value type
     * @return the resolved value
     * @throws OkaeriException if no configurer is available or the value processing fails
     */
    @SuppressWarnings("unchecked")
    public <T> T get(@NonNull String key, @NonNull GenericsDeclaration generics) throws OkaeriException {

        Configurer effectiveConfigurer = this.getEffectiveConfigurer();
        if (effectiveConfigurer == null) {
            throw new IllegalStateException("configurer cannot be null");
        }

        FieldDeclaration field = this.getDeclaration().getField(key).orElse(null);
        if (field != null) {
            return (T) effectiveConfigurer.resolveType(field.getValue(), field.getType(), generics.getType(), generics, SerdesContext.of(effectiveConfigurer, this.context, field));
        }

        Object rawValue = this.internalState.get(key);
        return (T) effectiveConfigurer.resolveValue(rawValue, generics.getType(), generics, SerdesContext.of(effectiveConfigurer, this.context, null));
    }

    /**
     * Saves current configuration state to the bindFile if file does not exist.
     *
     * @return this instance
     * @throws OkaeriException if bindFile is null or saving fails
     */
    public OkaeriConfig saveDefaults() throws OkaeriException {

        if (this.getBindFile() == null) {
            throw new IllegalStateException("bindFile cannot be null");
        }

        if (Files.exists(this.getBindFile())) {
            return this;
        }

        return this.save();
    }

    /**
     * Saves current configuration state to the bindFile.
     *
     * @return this instance
     * @throws OkaeriException if {@link #configurer} or {@link #bindFile} is null or saving fails
     */
    public OkaeriConfig save() throws OkaeriException {

        if (this.getBindFile() == null) {
            throw new IllegalStateException("bindFile cannot be null");
        }

        return this.save(this.getBindFile());
    }

    /**
     * Saves current configuration state to the specific file.
     * <p>
     * This method serializes to memory first before writing to disk to prevent
     * data loss if serialization fails. The original file content is preserved
     * if any error occurs during the save operation.
     *
     * @param file target file
     * @return this instance
     * @throws OkaeriException if {@link #configurer} is null or saving fails
     */
    public OkaeriConfig save(@NonNull File file) throws OkaeriException {
        // serialize to memory first to prevent data loss on errors
        ByteArrayOutputStream memoryBuffer = new ByteArrayOutputStream();
        this.save(memoryBuffer);
        // only write to disk if serialization succeeded
        try {
            File parentFile = file.getParentFile();
            if (parentFile != null) parentFile.mkdirs();
            try (FileOutputStream fileOut = new FileOutputStream(file, false)) {
                memoryBuffer.writeTo(fileOut);
            }
            return this;
        } catch (IOException exception) {
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
        return new String(this.saveToBytes(), StandardCharsets.UTF_8);
    }

    /**
     * Saves current configuration state as byte array.
     * Useful for binary formats or when raw bytes are needed.
     *
     * @return saved configuration bytes
     * @throws OkaeriException if {@link #configurer} is null or saving fails
     */
    public byte[] saveToBytes() throws OkaeriException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        this.save(outputStream);
        return outputStream.toByteArray();
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
            throw new IllegalStateException("configurer cannot be null");
        }

        // Validate entity before serialization (enables cross-field validation)
        if (this.context.hasValidator()) {
            this.context.validate(this, false);
        }

        // Build the data map from declared fields
        Map<String, Object> data = new LinkedHashMap<>();

        for (FieldDeclaration field : this.getDeclaration().getFields()) {
            Object valueToSave = field.getAnnotation(ReadOnly.class).isPresent()
                ? field.getStartingValue()
                : field.getValue();

            try {
                Object simplified = this.getConfigurer().simplifyField(valueToSave, field.getType(), field, this.context);
                data.put(field.getName(), simplified);
            } catch (Exception exception) {
                throw new OkaeriException("failed to simplify " + field.getName(), exception);
            }
        }

        // Handle orphans from internalState
        Set<String> allOrphans = new LinkedHashSet<>();
        for (String key : this.internalState.keySet()) {
            if (!data.containsKey(key)) {
                if (this.context.isRemoveOrphans()) {
                    allOrphans.add(key);
                } else {
                    data.put(key, this.internalState.get(key));
                }
            }
        }

        // Remove orphans recursively from nested configs
        if (this.context.isRemoveOrphans()) {
            this.removeOrphansRecursively(this.getDeclaration(), data, "", allOrphans);
            if (!allOrphans.isEmpty()) {
                this.context.getLogger().warning("Removed orphaned (undeclared) keys: " + allOrphans);
            }
        }

        // Update internalState with the serialized data
        this.internalState = data;

        try {
            this.getConfigurer().write(outputStream, data, this.getDeclaration());
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
            Object simplified = configurer.simplify(field.getValue(), field.getType(), SerdesContext.of(configurer, this.context, field), conservative);
            map.put(field.getName(), simplified);
        }

        // include undeclared keys from internalState (orphans)
        for (Map.Entry<String, Object> entry : this.internalState.entrySet()) {
            if (!map.containsKey(entry.getKey())) {
                Object simplified = configurer.simplify(entry.getValue(), GenericsDeclaration.of(entry.getValue()), SerdesContext.of(configurer, this.context, null), conservative);
                map.put(entry.getKey(), simplified);
            }
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
        return this.load(data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Loads new state to the configuration from the provided byte array.
     * Useful for binary formats or when raw bytes are available.
     *
     * @param data byte representation of the configuration
     * @return this instance
     * @throws OkaeriException if {@link #configurer} is null or loading fails
     */
    public OkaeriConfig load(byte @NonNull [] data) throws OkaeriException {

        if (this.getConfigurer() == null) {
            throw new IllegalStateException("configurer cannot be null");
        }

        ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
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
            throw new IllegalStateException("configurer cannot be null");
        }

        try {
            // Buffer the content for error reporting
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(chunk)) != -1) {
                buffer.write(chunk, 0, bytesRead);
            }
            byte[] contentBytes = buffer.toByteArray();
            String rawContent = new String(contentBytes, StandardCharsets.UTF_8);

            // Store raw content on context for error reporting
            this.context.setRawContent(rawContent);

            // Parse from buffered content and store in internalState
            Map<String, Object> loaded = this.getConfigurer().load(new ByteArrayInputStream(contentBytes), this.getDeclaration());
            this.internalState = (loaded != null) ? loaded : new LinkedHashMap<>();
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
            throw new IllegalStateException("configurer cannot be null");
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
            throw new IllegalStateException("configurer cannot be null");
        }

        return this.load(otherConfig.asMap(this.getConfigurer(), true));
    }

    /**
     * Validates the entire configuration entity using the registered validator.
     * This allows runtime validation without saving the config.
     * <p>
     * Supports both field-level and entity-level validation, including cross-field
     * constraints (e.g., JSR-380 class-level constraints).
     * <p>
     * If no validator is registered, this is a no-op.
     *
     * @return this instance
     * @throws OkaeriException if validation fails
     */
    public OkaeriConfig validate() throws OkaeriException {

        if (!this.context.hasValidator()) {
            return this;
        }

        this.context.validate(this);
        return this;
    }

    /**
     * Performs migrations on the current in-memory config state, validates the result,
     * and saves to file.
     * <p>
     * <b>IMPORTANT: Call order matters!</b> This method is <b>imperative</b> and runs
     * immediately on the current state. You MUST call {@link #load()} BEFORE migrate(),
     * otherwise migrations will run on default field values instead of your saved data.
     * <p>
     * Correct usage:
     * <pre>{@code
     * config.load();              // 1. Load existing data first
     * config.migrate(migration);  // 2. Migrate loaded data
     * // save() is called automatically by migrate()
     * }</pre>
     * <p>
     * Incorrect usage (common mistake):
     * <pre>{@code
     * config.migrate(migration);  // WRONG: Runs on defaults, not saved data!
     * config.load();              // Data loaded but migration already ran
     * }</pre>
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
     * Performs migrations on the current in-memory config state and invokes callback
     * with the count of performed migrations.
     * <p>
     * <b>IMPORTANT: Call order matters!</b> This method is <b>imperative</b> and runs
     * immediately on the current state. You MUST call {@link #load()} BEFORE migrate(),
     * otherwise migrations will run on default field values instead of your saved data.
     * <p>
     * The callback is invoked only if at least one migration was performed (count > 0).
     * Use this variant when you want custom save behavior instead of the default.
     *
     * @param callback   consumer invoked with performed migrations count (if > 0)
     * @param migrations migrations to be performed
     * @return this instance
     * @throws OkaeriException if {@link #configurer} is null or migration fails
     * @see #migrate(ConfigMigration...) for typical usage with automatic save
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
                    this.context.getLogger().info(name + ": " + description);
                }
            })
            .count();
        if (performed > 0) {
            callback.accept(performed);
        }
        return this;
    }

    /**
     * Updates state of the configuration with values from internalState,
     * also applying {@link Variable} annotation to the fields if present.
     *
     * @return this instance
     * @throws OkaeriException if {@link #declaration} is null or update fails
     */
    public OkaeriConfig update() throws OkaeriException {

        if (this.getDeclaration() == null) {
            throw new IllegalStateException("declaration cannot be null: config not initialized");
        }

        this.loadValuesFromInternalState();
        this.processVariablesRecursively(this.getDeclaration(), this, new HashSet<>());

        // Validate entity after @Variable processing (values may have changed)
        if (this.context.hasValidator()) {
            this.context.validate(this, true);
        }

        return this;
    }

    /**
     * Loads values from internalState into the declared fields.
     * For each field that exists in internalState, retrieves the value,
     * validates it, and updates both the field value and starting value.
     *
     * @throws OkaeriException if value retrieval or validation fails
     */
    private void loadValuesFromInternalState() throws OkaeriException {
        Configurer effectiveConfigurer = this.getEffectiveConfigurer();
        if (effectiveConfigurer == null) {
            throw new IllegalStateException("no effective configurer available");
        }

        for (FieldDeclaration field : this.getDeclaration().getFields()) {
            String fieldName = field.getName();
            GenericsDeclaration genericType = field.getType();
            Class<?> type = field.getType().getType();

            if (!this.internalState.containsKey(fieldName)) {
                continue;
            }

            // Build path including any base path
            ConfigPath fieldPath = ((this.internalPath == null) || this.internalPath.isEmpty())
                ? ConfigPath.of(fieldName)
                : this.internalPath.property(fieldName);
            SerdesContext serdesContext = SerdesContext.of(effectiveConfigurer, this.context, field)
                .withPath(fieldPath);

            Object rawValue = this.internalState.get(fieldName);
            Object value;
            try {
                value = effectiveConfigurer.resolveValue(rawValue, type, genericType, serdesContext);
            } catch (OkaeriConfigException exception) {
                throw exception;
            } catch (Exception exception) {
                throw OkaeriConfigException.builder()
                    .message("Cannot deserialize")
                    .path(fieldPath)
                    .expectedType(genericType)
                    .configurer(effectiveConfigurer)
                    .configContext(this.context)
                    .cause(exception)
                    .build();
            }

            field.updateValue(value);
            field.setStartingValue(value);
        }

        // Validate entity after all fields are loaded (enables cross-field validation)
        if (this.context.hasValidator()) {
            this.context.validate(this, true);
        }
    }

    /**
     * Recursively processes @Variable annotations in all fields of the given declaration,
     * including nested objects (both OkaeriConfig and Serializable).
     *
     * @param declaration    the declaration to process
     * @param configInstance the config instance containing the fields
     * @param visited        set of already visited objects to prevent infinite recursion
     */
    private void processVariablesRecursively(@NonNull ConfigDeclaration declaration, @NonNull Object configInstance, @NonNull Set<Object> visited) {
        Configurer effectiveConfigurer = this.getEffectiveConfigurer();

        // Prevent infinite recursion on circular references
        if (!visited.add(configInstance)) {
            return;
        }
        for (FieldDeclaration field : declaration.getFields()) {
            // Update the field's object reference
            field.setObject(configInstance);

            // Process @Variable for this field
            Variable variable = field.getVariable();
            if ((variable != null) && (effectiveConfigurer != null)) {
                String rawProperty = System.getProperty(variable.value());
                String property = (rawProperty == null) ? System.getenv(variable.value()) : rawProperty;

                if (property != null) {
                    GenericsDeclaration fieldType = field.getType();
                    // Build path including any base path
                    ConfigPath fieldPath = ((this.internalPath == null) || this.internalPath.isEmpty())
                        ? ConfigPath.of(field.getName())
                        : this.internalPath.property(field.getName());
                    SerdesContext serdesContext = SerdesContext.of(effectiveConfigurer, this.context, field).withPath(fieldPath);

                    Object value;
                    try {
                        value = effectiveConfigurer.resolveType(property, GenericsDeclaration.of(property), fieldType.getType(), fieldType, serdesContext);
                    } catch (OkaeriConfigException exception) {
                        throw exception;
                    } catch (Exception exception) {
                        throw OkaeriConfigException.builder()
                            .message("Failed to resolve @Variable(" + variable.value() + ")")
                            .path(fieldPath)
                            .expectedType(fieldType)
                            .actualValue(property)
                            .configurer(effectiveConfigurer)
                            .configContext(this.context)
                            .cause(exception)
                            .build();
                    }

                    field.updateValue(value);
                    field.setVariableHide(true);
                }
            }

            // Recursively process nested objects
            try {
                Object nestedObject = field.getValue();
                if (nestedObject == null) {
                    continue;
                }

                GenericsDeclaration fieldType = field.getType();
                Class<?> nestedClass = fieldType.getType();

                // Only recurse into OkaeriConfig subclasses
                // Serializable scanning is too broad and dangerous (circular refs, object graphs)
                if (fieldType.isConfig()) {
                    ConfigDeclaration nestedDeclaration = ConfigDeclaration.of(nestedClass);
                    this.processVariablesRecursively(nestedDeclaration, nestedObject, visited);
                }
            } catch (Exception exception) {
                throw new OkaeriException("failed to process variables recursively", exception);
            }
        }
    }

    /**
     * Recursively removes orphaned keys from nested configs in the data map.
     * Walks the configuration tree and removes undeclared keys at all levels.
     * <p>
     * Note: If a nested field type has a custom ObjectSerializer registered,
     * orphan removal is skipped for that field, since all keys in its serialized
     * form were intentionally added by the serializer.
     *
     * @param declaration the declaration to process
     * @param data        the data map to modify
     * @param keyPrefix   the key prefix for nested paths (empty for root)
     * @param allOrphans  set to collect all removed orphan key paths
     */
    private void removeOrphansRecursively(@NonNull ConfigDeclaration declaration, @NonNull Map<String, Object> data, @NonNull String keyPrefix, @NonNull Set<String> allOrphans) {
        Configurer effectiveConfigurer = this.getEffectiveConfigurer();

        for (FieldDeclaration field : declaration.getFields()) {
            String fieldName = field.getName();
            String fullPath = keyPrefix.isEmpty() ? fieldName : (keyPrefix + "." + fieldName);

            GenericsDeclaration fieldType = field.getType();
            if (!fieldType.isConfig()) {
                continue;
            }

            Object nestedValue = data.get(fieldName);
            if (!(nestedValue instanceof Map)) {
                continue;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> nestedMap = (Map<String, Object>) nestedValue;

            // Check if this field type has a custom serializer
            // If it does, don't remove any keys - they were added by the serializer
            if ((effectiveConfigurer != null) && (effectiveConfigurer.getRegistry().getSerializer(fieldType.getType()) != null)) {
                continue;
            }

            ConfigDeclaration nestedDeclaration = ConfigDeclaration.of(fieldType.getType());

            Set<String> declaredKeys = nestedDeclaration.getFieldMap().keySet();
            Set<String> nestedOrphanedKeys = new LinkedHashSet<>(nestedMap.keySet());
            nestedOrphanedKeys.removeAll(declaredKeys);

            for (String orphanKey : nestedOrphanedKeys) {
                nestedMap.remove(orphanKey);
                allOrphans.add(fullPath + "." + orphanKey);
            }

            this.removeOrphansRecursively(nestedDeclaration, nestedMap, fullPath, allOrphans);
        }
    }
}
