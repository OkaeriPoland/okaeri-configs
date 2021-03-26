package eu.okaeri.configs.bukkit;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BukkitConfigurer extends Configurer {

    private YamlConfiguration config;
    private String commentPrefix = "# ";
    private String sectionSeparator = SectionSeparator.NEW_LINE;

    public BukkitConfigurer(YamlConfiguration config, String commentPrefix, String sectionSeparator) {
        this(commentPrefix, sectionSeparator);
    }

    public BukkitConfigurer(String commentPrefix, String sectionSeparator) {
        this();
        this.commentPrefix = commentPrefix;
        this.sectionSeparator = sectionSeparator;
    }

    public BukkitConfigurer(String sectionSeparator) {
        this();
        this.sectionSeparator = sectionSeparator;
    }

    public BukkitConfigurer(YamlConfiguration config) {
        this.config = config;
    }

    public BukkitConfigurer() {
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

        // add comments
        ConfigPostprocessor.of(file).read()
                .removeLines((line) -> line.startsWith(this.commentPrefix))
                .updateLines((line) -> declaration.getFields().stream().filter(field -> line.startsWith(field.getName() + ":"))
                        .findAny()
                        .map(FieldDeclaration::getComment)
                        .map(comment -> this.sectionSeparator + this.buildComment(comment) + line)
                        .orElse(line))
                .updateContext(context -> {
                    // add header if available
                    if (declaration.getHeader() == null) {
                        return context;
                    }
                    return this.buildComment(declaration.getHeader()) + this.sectionSeparator + context;
                })
                .write();
    }

    private String buildComment(String[] strings) {
        if (strings == null) return null;
        List<String> lines = new ArrayList<>();
        for (String line : strings) {
            String[] parts = line.split("\n");
            String prefix = line.startsWith(this.commentPrefix.trim()) ? "" : this.commentPrefix;
            lines.add((line.isEmpty() ? "" : prefix) + line);
        }
        return String.join("\n", lines) + "\n";
    }
}
