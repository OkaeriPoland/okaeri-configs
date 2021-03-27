package eu.okaeri.example;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.yaml.bukkit.YamlBukkitConfigurer;
import eu.okaeri.configs.yaml.bukkit.serdes.SerdesBukkit;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Level;

public class OkaeriConfigPlugin extends JavaPlugin {

    private ExampleConfig config;

    public ExampleConfig getConfiguration() {
        return this.config;
    }

    @Override
    public void onEnable() {

        try {
            this.config = ConfigManager.create(ExampleConfig.class, (it) -> {
                it.withConfigurer(new YamlBukkitConfigurer(), new SerdesBukkit());
                it.withBindFile(new File(this.getDataFolder(), "config.yml"));
                it.saveDefaults();
                it.load(true);
            });
        } catch (Exception exception) {
            this.getLogger().log(Level.SEVERE, "Error loading config.yml", exception);
            this.getPluginLoader().disablePlugin(this);
            return;
        }

        Location spawn = this.config.getSpawn();
        this.getLogger().info(spawn.toString());
    }
}
