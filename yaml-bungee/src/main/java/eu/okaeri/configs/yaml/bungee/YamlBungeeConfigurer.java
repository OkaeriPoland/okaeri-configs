package eu.okaeri.configs.yaml.bungee;

import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.exception.OkaeriException;
import eu.okaeri.configs.format.SourceWalker;
import eu.okaeri.configs.format.yaml.YamlSourceWalker;
import eu.okaeri.configs.postprocessor.ConfigPostprocessor;
import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.SerdesContext;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Accessors(chain = true)
public class YamlBungeeConfigurer extends Configurer {

    @Setter private String commentPrefix = "# ";

    @Override
    public List<String> getExtensions() {
        return Arrays.asList("yml", "yaml");
    }

    @Override
    public SourceWalker createSourceWalker() {
        String raw = this.getRawContent();
        return (raw == null) ? null : YamlSourceWalker.of(raw);
    }

    @Override
    public boolean isCommentLine(String line) {
        return line.trim().startsWith("#");
    }

    @Override
    public Object simplify(Object value, GenericsDeclaration genericType, @NonNull SerdesContext serdesContext, boolean conservative) throws OkaeriException {

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
    public <T> T resolveType(Object object, GenericsDeclaration genericSource, @NonNull Class<T> targetClazz, GenericsDeclaration genericTarget, @NonNull SerdesContext serdesContext) {

        if (object instanceof Configuration) {
            Configuration configuration = (Configuration) object;
            Map<String, Object> values = new LinkedHashMap<>();
            configuration.getKeys().forEach(key -> values.put(key, configuration.get(key)));
            return super.resolveType(values, GenericsDeclaration.of(Map.class, Arrays.asList(String.class, Object.class)), targetClazz, genericTarget, serdesContext);
        }

        return super.resolveType(object, genericSource, targetClazz, genericTarget, serdesContext);
    }

    @Override
    public Map<String, Object> load(@NonNull InputStream inputStream, @NonNull ConfigDeclaration declaration) throws Exception {
        String data = ConfigPostprocessor.of(inputStream).getContext();
        Configuration config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(data);

        Map<String, Object> map = new LinkedHashMap<>();
        for (String key : config.getKeys()) {
            Object value = config.get(key);
            // Convert Configuration to Map for nested objects
            if (value instanceof Configuration) {
                value = configurationToMap((Configuration) value);
            }
            map.put(key, value);
        }
        return map;
    }

    @Override
    public void write(@NonNull OutputStream outputStream, @NonNull Map<String, Object> data, @NonNull ConfigDeclaration declaration) throws Exception {
        Configuration config = new Configuration();

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            config.set(entry.getKey(), entry.getValue());
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ConfigurationProvider.getProvider(YamlConfiguration.class).save(config, new OutputStreamWriter(baos, StandardCharsets.UTF_8));

        ConfigPostprocessor.of(baos.toString(StandardCharsets.UTF_8.name()))
            .removeLines(line -> line.startsWith(this.commentPrefix.trim()))
            .updateContext(ctx -> YamlSourceWalker.of(ctx).insertComments(declaration, this.commentPrefix))
            .write(outputStream);
    }

    private Map<String, Object> configurationToMap(Configuration config) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (String key : config.getKeys()) {
            Object value = config.get(key);
            if (value instanceof Configuration) {
                value = configurationToMap((Configuration) value);
            }
            map.put(key, value);
        }
        return map;
    }
}
