package eu.okaeri.configs.bukkit;

import eu.okaeri.configs.ConfigUtil;
import eu.okaeri.configs.Configurer;
import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.schema.GenericsDeclaration;
import lombok.AllArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@AllArgsConstructor
public class BukkitConfigurer extends Configurer {

    private YamlConfiguration config;

    @Override
    public String getCommentPrefix() {
        return "#";
    }

    @Override
    public String getSectionSeparator() {
        return "\n";
    }

    @Override
    public void setValue(String key, Object value) {
        this.config.set(key, value);
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
    public <T> T resolveType(Object object, Class<T> clazz, GenericsDeclaration type) {

        if ((object instanceof MemorySection) && (clazz == Map.class)) {

            Map<String, Object> values = ((MemorySection) object).getValues(false);
            GenericsDeclaration keyDeclaration = type.getSubtype().get(0);
            GenericsDeclaration valueDeclaration = type.getSubtype().get(1);
            Map<Object, Object> map = new LinkedHashMap<>();

            for (Map.Entry<String, Object> entry : values.entrySet()) {
                Object key = this.resolveType(entry.getKey(), keyDeclaration.getType(), keyDeclaration);
                Object value = this.resolveType(entry.getValue(), valueDeclaration.getType(), valueDeclaration);
                map.put(key, value);
            }

            return super.resolveType(map, clazz, type);
        }

        return super.resolveType(object, clazz, null);
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

        this.config.save(file);

        String data = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        data = ConfigUtil.removeStartingWith(this.getCommentPrefix(), data);
        data = ConfigUtil.addCommentsToFields(this.getCommentPrefix(), this.getSectionSeparator(), data, declaration);

        String header = ConfigUtil.convertToComment(this.getCommentPrefix(), declaration.getHeader(), true);
        String output = header + this.getSectionSeparator() + data;

        FileUtils.write(file, output, StandardCharsets.UTF_8);
    }
}
