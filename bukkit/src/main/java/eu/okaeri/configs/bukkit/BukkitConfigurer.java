package eu.okaeri.configs.bukkit;

import eu.okaeri.configs.ConfigUtil;
import eu.okaeri.configs.Configurer;
import eu.okaeri.configs.schema.ConfigDeclaration;
import lombok.AllArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@AllArgsConstructor
public class BukkitConfigurer implements Configurer {

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
    public <T> T getValue(String key, Class<T> clazz) {

        Object value = this.getValue(key);
        if (value == null) {
            return null;
        }

        if ((value instanceof MemorySection) && (clazz == Map.class)) {
            return clazz.cast(((MemorySection) value).getValues(false));
        }

        return clazz.cast(value); // FIXME: primitives support "Cannot cast java.lang.Integer to int"
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
