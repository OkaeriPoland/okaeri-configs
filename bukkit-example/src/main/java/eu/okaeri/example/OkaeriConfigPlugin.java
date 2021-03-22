package eu.okaeri.example;

import eu.okaeri.configs.bukkit.BukkitConfigurer;
import eu.okaeri.configs.bukkit.serdes.BukkitSerdes;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public class OkaeriConfigPlugin extends JavaPlugin {

    private ExampleConfig config;

    public ExampleConfig getConfiguration() {
        return this.config;
    }

    @Override
    public void onEnable() {

        try {
            this.config = (ExampleConfig) new ExampleConfig()
                    .withConfigurer(new BukkitConfigurer(), new BukkitSerdes())
                    .withBindFile(new File(this.getDataFolder(), "config.yml"))
                    .saveDefaults()
                    .load(true);
        } catch (IllegalAccessException | IOException exception) {
            this.getLogger().log(Level.SEVERE, "Error loading config.yml", exception);
            this.getPluginLoader().disablePlugin(this);
        }

        Location spawn = this.config.getSpawn();
        this.getLogger().info(spawn.toString());
    }
}
