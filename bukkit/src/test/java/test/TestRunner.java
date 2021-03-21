package test;

import eu.okaeri.configs.bukkit.BukkitConfigurer;
import eu.okaeri.configs.bukkit.serdes.LocationSerializer;
import eu.okaeri.configs.transformer.TransformerRegistry;
import lombok.SneakyThrows;
import org.bukkit.configuration.file.YamlConfiguration;
import test.impl.TestConfig;

import java.io.File;

public final class TestRunner {

    @SneakyThrows
    public static void main(String[] args) {

        TransformerRegistry.register(new LocationSerializer());

        File bindFile = new File("config.yml");
        BukkitConfigurer configurer = new BukkitConfigurer(new YamlConfiguration());

        TestConfig config = new TestConfig();
        config.setBindFile(bindFile);
        config.setConfigurer(configurer);

        config.load();
        System.out.println(config);

//        config.getWhitelist().add("127.0.0.2");
//        System.out.println(config);

        config.save();
    }
}
