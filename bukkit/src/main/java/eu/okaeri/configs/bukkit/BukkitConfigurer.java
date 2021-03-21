package eu.okaeri.configs.bukkit;

import eu.okaeri.configs.OkaeriConfigurer;
import lombok.AllArgsConstructor;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

@AllArgsConstructor
public class BukkitConfigurer implements OkaeriConfigurer {

    private YamlConfiguration config;

    @Override
    public void setValue(String key, Object value) {
        this.config.set(key, value);
    }

    @Override
    public Object getValue(String key) {
        return this.config.get(key);
    }

    @Override
    public void loadFromFile(File file) throws IOException {
        try {
            this.config.load(file);
        } catch (InvalidConfigurationException exception) {
            throw new IOException(exception);
        }
    }

    @Override
    public void writeToFile(File file) throws IOException {
        this.config.save(file);
    }
}
