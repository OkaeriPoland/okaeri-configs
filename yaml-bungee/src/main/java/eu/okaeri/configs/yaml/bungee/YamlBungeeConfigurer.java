package eu.okaeri.configs.yaml.bungee;

import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.format.yaml.YamlSourceWalker;
import eu.okaeri.configs.postprocessor.ConfigPostprocessor;
import eu.okaeri.configs.schema.ConfigDeclaration;
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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Accessors(chain = true)
public class YamlBungeeConfigurer extends Configurer {

    @Setter private String commentPrefix = "# ";

    @Override
    public List<String> getExtensions() {
        return Arrays.asList("yml", "yaml");
    }

    @Override
    public boolean isCommentLine(String line) {
        return line.trim().startsWith("#");
    }

    @Override
    public Map<String, Object> load(@NonNull InputStream inputStream, @NonNull ConfigDeclaration declaration) throws Exception {

        String data = ConfigPostprocessor.of(inputStream).getContext();
        Configuration config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(data);

        return this.configurationToMap(config);
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
                value = this.configurationToMap((Configuration) value);
            }
            map.put(key, value);
        }
        return map;
    }
}
