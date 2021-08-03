package eu.okaeri.configs.yaml.snakeyaml;

import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.postprocessor.ConfigLineInfo;
import eu.okaeri.configs.postprocessor.ConfigPostprocessor;
import eu.okaeri.configs.postprocessor.SectionSeparator;
import eu.okaeri.configs.postprocessor.format.YamlSectionWalker;
import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.schema.FieldDeclaration;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.SerdesContext;
import lombok.NonNull;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.resolver.Resolver;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.function.Consumer;

public class YamlSnakeYamlConfigurer extends Configurer {

    private Yaml config;
    private Map<String, Object> map = new LinkedHashMap<>();

    private String commentPrefix = "# ";
    private String sectionSeparator = SectionSeparator.NONE;

    public YamlSnakeYamlConfigurer(@NonNull Yaml config, @NonNull Map<String, Object> map, @NonNull String commentPrefix, @NonNull String sectionSeparator) {
        this(config, commentPrefix, sectionSeparator);
        this.map = map;
    }

    public YamlSnakeYamlConfigurer(@NonNull Yaml config, @NonNull String commentPrefix, @NonNull String sectionSeparator) {
        this(commentPrefix, sectionSeparator);
        this.config = config;
    }

    public YamlSnakeYamlConfigurer(@NonNull String commentPrefix, @NonNull String sectionSeparator) {
        this();
        this.commentPrefix = commentPrefix;
        this.sectionSeparator = sectionSeparator;
    }

    public YamlSnakeYamlConfigurer(@NonNull String sectionSeparator) {
        this();
        this.sectionSeparator = sectionSeparator;
    }

    public YamlSnakeYamlConfigurer(@NonNull Yaml config, @NonNull Map<String, Object> map) {
        this.config = config;
        this.map = map;
    }

    public YamlSnakeYamlConfigurer(@NonNull Yaml config) {
        this.config = config;
    }

    public YamlSnakeYamlConfigurer() {
        this(new Yaml(
                new Constructor(),
                apply(new Representer(), (representer) -> representer.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK)),
                apply(new DumperOptions(), (dumperOptions) -> dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK)),
                new LoaderOptions(),
                new Resolver()));
    }

    @Override
    public void setValue(@NonNull String key, Object value, GenericsDeclaration type, FieldDeclaration field) {
        Object simplified = this.simplify(value, type, SerdesContext.of(this, field), true);
        this.map.put(key, simplified);
    }

    @Override
    public Object getValue(@NonNull String key) {
        return this.map.get(key);
    }

    @Override
    public boolean keyExists(@NonNull String key) {
        return this.map.containsKey(key);
    }

    @Override
    public List<String> getAllKeys() {
        return Collections.unmodifiableList(new ArrayList<>(this.map.keySet()));
    }

    @Override
    public void load(@NonNull InputStream inputStream, @NonNull ConfigDeclaration declaration) throws Exception {
        // try loading from input stream
        this.map = this.config.load(inputStream);
        // when no map was loaded reset with empty
        if (this.map == null) this.map = new LinkedHashMap<>();
    }

    @Override
    public void write(@NonNull OutputStream outputStream, @NonNull ConfigDeclaration declaration) throws Exception {

        // render to string
        String contents = this.config.dump(this.map);

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

                        String comment = ConfigPostprocessor.createComment(YamlSnakeYamlConfigurer.this.commentPrefix, fieldComment);
                        return ConfigPostprocessor.addIndent(comment, lineInfo.getIndent()) + line;
                    }
                })
                // add header if available
                .prependContextComment(this.commentPrefix, this.sectionSeparator, declaration.getHeader())
                // save
                .write(outputStream);
    }

    private static <T> T apply(T object, Consumer<T> consumer) {
        consumer.accept(object);
        return object;
    }
}
