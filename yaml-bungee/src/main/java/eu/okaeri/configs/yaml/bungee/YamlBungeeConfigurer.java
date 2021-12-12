package eu.okaeri.configs.yaml.bungee;

import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.exception.OkaeriException;
import eu.okaeri.configs.postprocessor.ConfigLineInfo;
import eu.okaeri.configs.postprocessor.ConfigPostprocessor;
import eu.okaeri.configs.postprocessor.SectionSeparator;
import eu.okaeri.configs.postprocessor.format.YamlSectionWalker;
import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.schema.FieldDeclaration;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.SerdesContext;
import lombok.NonNull;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class YamlBungeeConfigurer extends Configurer {

    private Configuration config;
    private String commentPrefix = "# ";
    private String sectionSeparator = SectionSeparator.NONE;

    public YamlBungeeConfigurer(@NonNull YamlConfiguration config, @NonNull String commentPrefix, @NonNull String sectionSeparator) {
        this(commentPrefix, sectionSeparator);
    }

    public YamlBungeeConfigurer(@NonNull String commentPrefix, @NonNull String sectionSeparator) {
        this();
        this.commentPrefix = commentPrefix;
        this.sectionSeparator = sectionSeparator;
    }

    public YamlBungeeConfigurer(@NonNull String sectionSeparator) {
        this();
        this.sectionSeparator = sectionSeparator;
    }

    public YamlBungeeConfigurer(@NonNull Configuration config) {
        this.config = config;
    }

    public YamlBungeeConfigurer() {
        this(new Configuration());
    }

    @Override
    public Object simplify(Object value, GenericsDeclaration genericType, SerdesContext serdesContext, boolean conservative) throws OkaeriException {

        if (value instanceof Configuration) {
            Configuration configuration = (Configuration) value;
            Map<String, Object> values = new LinkedHashMap<>();
            configuration.getKeys().forEach(key -> values.put(key, configuration.get(key)));
            return values;
        }

        return super.simplify(value, genericType, serdesContext, conservative);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T resolveType(Object object, GenericsDeclaration genericSource, @NonNull Class<T> targetClazz, GenericsDeclaration genericTarget, SerdesContext serdesContext) {

        if (object instanceof Configuration) {
            Configuration configuration = (Configuration) object;
            Map<String, Object> values = new LinkedHashMap<>();
            configuration.getKeys().forEach(key -> values.put(key, configuration.get(key)));
            return super.resolveType(values, GenericsDeclaration.of(Map.class, Arrays.asList(String.class, Object.class)), targetClazz, genericTarget, serdesContext);
        }

        return super.resolveType(object, genericSource, targetClazz, genericTarget, serdesContext);
    }

    @Override
    public void setValue(@NonNull String key, Object value, GenericsDeclaration type, FieldDeclaration field) {
        Object simplified = this.simplify(value, type, SerdesContext.of(this, field), true);
        this.config.set(key, simplified);
    }

    @Override
    public Object getValue(@NonNull String key) {
        return this.config.get(key);
    }

    @Override
    public boolean keyExists(@NonNull String key) {
        return this.config.getKeys().contains(key);
    }

    @Override
    public List<String> getAllKeys() {
        return Collections.unmodifiableList(new ArrayList<>(this.config.getKeys()));
    }

    @Override
    public void load(@NonNull InputStream inputStream, @NonNull ConfigDeclaration declaration) throws Exception {
        String data = ConfigPostprocessor.of(inputStream).getContext();
        this.config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(data);
    }

    @Override
    public void write(@NonNull OutputStream outputStream, @NonNull ConfigDeclaration declaration) throws Exception {

        // bungee's save
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ConfigurationProvider.getProvider(YamlConfiguration.class).save(this.config, new OutputStreamWriter(baos));
        String contents = new String(baos.toByteArray(), StandardCharsets.UTF_8);

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

                    String comment = ConfigPostprocessor.createComment(YamlBungeeConfigurer.this.commentPrefix, fieldComment);
                    return ConfigPostprocessor.addIndent(comment, lineInfo.getIndent()) + line;
                }
            })
            // add header if available
            .prependContextComment(this.commentPrefix, this.sectionSeparator, declaration.getHeader())
            // save
            .write(outputStream);
    }
}
