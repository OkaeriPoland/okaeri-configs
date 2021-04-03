package eu.okaeri.configs.yaml.bukkit;

import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.postprocessor.ConfigLineInfo;
import eu.okaeri.configs.postprocessor.ConfigPostprocessor;
import eu.okaeri.configs.postprocessor.SectionSeparator;
import eu.okaeri.configs.postprocessor.format.YamlSectionWalker;
import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.schema.FieldDeclaration;
import eu.okaeri.configs.schema.GenericsDeclaration;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
        Object simplified = this.simplify(value, type, true);
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
                .prependContextComment(this.commentPrefix, this.sectionSeparator, declaration.getHeader())
                // save
                .write();
    }
}
