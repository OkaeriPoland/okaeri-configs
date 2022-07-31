package eu.okaeri.configs.yaml.bukkit;

import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.exception.OkaeriException;
import eu.okaeri.configs.postprocessor.ConfigLineInfo;
import eu.okaeri.configs.postprocessor.ConfigPostprocessor;
import eu.okaeri.configs.postprocessor.format.YamlSectionWalker;
import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.schema.FieldDeclaration;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.SerdesContext;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

@Accessors(chain = true)
public class YamlBukkitConfigurer extends Configurer {

    private YamlConfiguration config;
    @Setter private String commentPrefix = "# ";

    public YamlBukkitConfigurer(@NonNull YamlConfiguration config) {
        this.config = config;
    }

    public YamlBukkitConfigurer() {
        this(new YamlConfiguration());
        this.config.options().pathSeparator((char) 29);
    }

    @Override
    public List<String> getExtensions() {
        return Arrays.asList("yml", "yaml");
    }

    @Override
    public Object simplify(Object value, GenericsDeclaration genericType, @NonNull SerdesContext serdesContext, boolean conservative) throws OkaeriException {

        if (value instanceof MemorySection) {
            return ((MemorySection) value).getValues(false);
        }

        return super.simplify(value, genericType, serdesContext, conservative);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T resolveType(Object object, GenericsDeclaration genericSource, @NonNull Class<T> targetClazz, GenericsDeclaration genericTarget, @NonNull SerdesContext serdesContext) {

        if (object instanceof MemorySection) {
            Map<String, Object> values = ((MemorySection) object).getValues(false);
            return super.resolveType(values, GenericsDeclaration.of(values), targetClazz, genericTarget, serdesContext);
        }

        return super.resolveType(object, genericSource, targetClazz, genericTarget, serdesContext);
    }

    @Override
    public void setValue(@NonNull String key, Object value, GenericsDeclaration type, FieldDeclaration field) {
        Object simplified = this.simplify(value, type, SerdesContext.of(this, field), true);
        this.config.set(key, simplified);
    }

    @Override
    public void setValueUnsafe(@NonNull String key, Object value) {
        this.config.set(key, value);
    }

    @Override
    public Object getValue(@NonNull String key) {
        return this.config.get(key);
    }

    @Override
    public Object remove(@NonNull String key) {

        if (!this.keyExists(key)) {
            return null;
        }

        Object old = this.config.get(key);
        this.config.set(key, null);
        return old;
    }

    @Override
    public boolean keyExists(@NonNull String key) {
        return this.config.getKeys(false).contains(key);
    }

    @Override
    public List<String> getAllKeys() {
        return Collections.unmodifiableList(new ArrayList<>(this.config.getKeys(false)));
    }

    @Override
    public void load(@NonNull InputStream inputStream, @NonNull ConfigDeclaration declaration) throws Exception {
        this.config.loadFromString(ConfigPostprocessor.of(inputStream).getContext());
    }

    @Override
    public void write(@NonNull OutputStream outputStream, @NonNull ConfigDeclaration declaration) throws Exception {

        // bukkit's save
        String contents = this.config.saveToString();

        // postprocess
        ConfigPostprocessor.of(contents)
            // remove all current top-level commments (bukkit may preserve header)
            .removeLines((line) -> line.startsWith(this.commentPrefix.trim()))
            // add new comments
            .updateLinesKeys(new YamlSectionWalker() {
                @Override
                public String update(String line, ConfigLineInfo lineInfo, List<ConfigLineInfo> path) {

                    ConfigDeclaration currentDeclaration = declaration;
                    for (int i = 0; i < (path.size() - 1); i++) {
                        ConfigLineInfo pathElement = path.get(i);
                        Optional<FieldDeclaration> field = currentDeclaration.getField(pathElement.getName());
                        if (!field.isPresent()) {
                            return line;
                        }
                        GenericsDeclaration fieldType = field.get().getType();
                        if (!fieldType.isConfig()) {
                            return line;
                        }
                        currentDeclaration = ConfigDeclaration.of(fieldType.getType());
                    }

                    Optional<FieldDeclaration> lineDeclaration = currentDeclaration.getField(lineInfo.getName());
                    if (!lineDeclaration.isPresent()) {
                        return line;
                    }

                    String[] fieldComment = lineDeclaration.get().getComment();
                    if (fieldComment == null) {
                        return line;
                    }

                    String comment = ConfigPostprocessor.createComment(YamlBukkitConfigurer.this.commentPrefix, fieldComment);
                    return ConfigPostprocessor.addIndent(comment, lineInfo.getIndent()) + line;
                }
            })
            // add header if available
            .prependContextComment(this.commentPrefix, declaration.getHeader())
            // save
            .write(outputStream);
    }
}
