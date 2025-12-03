package eu.okaeri.configs.yaml.bukkit;

import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.format.yaml.YamlSourceWalker;
import eu.okaeri.configs.postprocessor.ConfigPostprocessor;
import eu.okaeri.configs.schema.ConfigDeclaration;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Accessors(chain = true)
public class YamlBukkitConfigurer extends Configurer {

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

        YamlConfiguration config = new YamlConfiguration();
        config.options().pathSeparator((char) 29);
        config.loadFromString(ConfigPostprocessor.of(inputStream).getContext());

        return this.memorySectionToMap(config);
    }

    @Override
    public void write(@NonNull OutputStream outputStream, @NonNull Map<String, Object> data, @NonNull ConfigDeclaration declaration) throws Exception {

        YamlConfiguration config = new YamlConfiguration();
        config.options().pathSeparator((char) 29); // 'group separator': disables dot parsing in set/get

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            config.set(entry.getKey(), entry.getValue());
        }

        ConfigPostprocessor.of(config.saveToString())
            .removeLines(line -> line.startsWith(this.commentPrefix.trim()))
            .removeLinesUntil(line -> line.chars().anyMatch(x -> !Character.isWhitespace(x)))
            .updateContext(ctx -> YamlSourceWalker.of(ctx).insertComments(declaration, this.commentPrefix))
            .write(outputStream);
    }

    private Map<String, Object> memorySectionToMap(MemorySection section) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            Object value = section.get(key);
            if (value instanceof MemorySection) {
                value = this.memorySectionToMap((MemorySection) value);
            }
            map.put(key, value);
        }
        return map;
    }
}
