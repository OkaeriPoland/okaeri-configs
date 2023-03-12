package eu.okaeri.configs.yaml.snakeyaml;

import eu.okaeri.configs.configurer.Configurer;
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

@Accessors(chain = true)
public class YamlSnakeYamlConfigurer extends Configurer {

    private Yaml yaml;
    private Map<String, Object> map = new LinkedHashMap<>();

    @Setter private String commentPrefix = "# ";

    public YamlSnakeYamlConfigurer(@NonNull Yaml yaml, @NonNull Map<String, Object> map) {
        this.yaml = yaml;
        this.map = map;
    }

    public YamlSnakeYamlConfigurer(@NonNull Yaml yaml) {
        this.yaml = yaml;
    }

    public YamlSnakeYamlConfigurer() {
        this(createYaml());
    }

    private static Yaml createYaml() {

        LoaderOptions loaderOptions = new LoaderOptions();
        Constructor constructor = new Constructor(loaderOptions);

        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        Representer representer = new Representer(dumperOptions);
        representer.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        Resolver resolver = new Resolver();

        return new Yaml(constructor, representer, dumperOptions, loaderOptions, resolver);
    }

    private static <T> T apply(T object, Consumer<T> consumer) {
        consumer.accept(object);
        return object;
    }

    @Override
    public List<String> getExtensions() {
        return Arrays.asList("yml", "yaml");
    }

    @Override
    public void setValue(@NonNull String key, Object value, GenericsDeclaration type, FieldDeclaration field) {
        Object simplified = this.simplify(value, type, SerdesContext.of(this, field), true);
        this.map.put(key, simplified);
    }

    @Override
    public void setValueUnsafe(@NonNull String key, Object value) {
        this.map.put(key, value);
    }

    @Override
    public Object getValue(@NonNull String key) {
        return this.map.get(key);
    }

    @Override
    public Object remove(@NonNull String key) {
        return this.map.remove(key);
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
        this.map = this.yaml.load(inputStream);
        // when no map was loaded reset with empty
        if (this.map == null) this.map = new LinkedHashMap<>();
    }

    @Override
    public void write(@NonNull OutputStream outputStream, @NonNull ConfigDeclaration declaration) throws Exception {

        // render to string
        String contents = this.yaml.dump(this.map);

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
            .prependContextComment(this.commentPrefix, declaration.getHeader())
            // save
            .write(outputStream);
    }
}
