package eu.okaeri.configs.yaml.bukkit;

import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.postprocessor.ConfigPostprocessor;
import eu.okaeri.configs.postprocessor.SectionSeparator;
import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.schema.FieldDeclaration;
import eu.okaeri.configs.schema.GenericsDeclaration;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.function.Predicate;

public class YamlBukkitConfigurer extends Configurer {

    private YamlConfiguration config;
    private String commentPrefix = "# ";
    private String sectionSeparator = SectionSeparator.NEW_LINE;

    public YamlBukkitConfigurer(YamlConfiguration config, String commentPrefix, String sectionSeparator) {
        this(commentPrefix, sectionSeparator);
    }

    public YamlBukkitConfigurer(String commentPrefix, String sectionSeparator) {
        this();
        this.commentPrefix = commentPrefix;
        this.sectionSeparator = sectionSeparator;
    }

    public YamlBukkitConfigurer(String sectionSeparator) {
        this();
        this.sectionSeparator = sectionSeparator;
    }

    public YamlBukkitConfigurer(YamlConfiguration config) {
        this.config = config;
    }

    public YamlBukkitConfigurer() {
        this(new YamlConfiguration());
    }

    @Override
    public void setValue(String key, Object value, GenericsDeclaration type, FieldDeclaration field) {
        Object simplified = this.simplify(value, type);
        this.config.set(key, simplified);
    }

    @Override
    public Object getValue(String key) {
        return this.config.get(key);
    }

    @Override
    public boolean keyExists(String key) {
        return this.config.getKeys(false).contains(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T resolveType(Object object, GenericsDeclaration genericSource, Class<T> targetClazz, GenericsDeclaration genericTarget) {

        if (object instanceof MemorySection) {
            Map<String, Object> values = ((MemorySection) object).getValues(false);
            return super.resolveType(values, GenericsDeclaration.of(values), targetClazz, genericTarget);
        }

        return super.resolveType(object, GenericsDeclaration.of(object), targetClazz, genericTarget);
    }

    @Override
    public void loadFromFile(File file, ConfigDeclaration declaration) throws IOException {
        try {
            this.config.load(file);
        } catch (InvalidConfigurationException exception) {
            throw new IOException(exception);
        }
    }

    @Override
    public void writeToFile(File file, ConfigDeclaration declaration) throws IOException {

        // bukkit's save
        this.config.save(file);

        // postprocess
        ConfigPostprocessor.of(file).read()
                // remove all current commments
                .removeLines((line) -> line.startsWith(this.commentPrefix.trim()))
                // add new comments
                .updateLines((line) -> declaration.getFields().stream()
                        .filter(this.isFieldDeclaredForLine(line))
                        .findAny()
                        .map(FieldDeclaration::getComment)
                        .map(comment -> this.sectionSeparator + ConfigPostprocessor.createComment(this.commentPrefix, comment) + line)
                        .orElse(line))
                // add header if available
                .prependContextComment(this.commentPrefix, this.sectionSeparator, declaration.getHeader())
                // save
                .write();
    }

    private Predicate<FieldDeclaration> isFieldDeclaredForLine(String line) {
        return field -> line.startsWith(field.getName() + ":"); // key:
    }
}
