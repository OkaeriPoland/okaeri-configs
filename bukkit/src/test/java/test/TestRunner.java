package test;

import eu.okaeri.configs.OkaeriConfig;
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

        config();
        empty();
        inline();
    }

    @SneakyThrows
    private static void inline() {

        File bindFile = new File("inline.yml");
        BukkitConfigurer configurer = new BukkitConfigurer(new YamlConfiguration());

        OkaeriConfig inline = new OkaeriConfig() {
            String test = "siema";
            String hmm = "działa";
        };

        inline.setBindFile(bindFile);
        inline.setConfigurer(configurer);

        inline.save();
        System.out.println(inline);
    }

    @SneakyThrows
    private static void empty() {

        File bindFile = new File("empty.yml");
        BukkitConfigurer configurer = new BukkitConfigurer(new YamlConfiguration());

        OkaeriConfig empty = new OkaeriConfig() {};
        empty.setBindFile(bindFile);
        empty.setConfigurer(configurer);

        empty.set("elo", 1);
        empty.set("elon-musk", "witam dziwne");
        empty.set("elon-musk-2", "witam dziwniejsze\nhmmm hehe");

        empty.save();
        empty.load();

        System.out.println(empty.get("elon-musk-2"));
    }

    @SneakyThrows
    private static void config() {

        File bindFile = new File("config.yml");
        BukkitConfigurer configurer = new BukkitConfigurer(new YamlConfiguration());

        TestConfig config = new TestConfig();
        config.setBindFile(bindFile);
        config.setConfigurer(configurer);

        config.save();
        config.load();
        System.out.println(config);

//        config.getWhitelist().add("127.0.0.2");
//        System.out.println(config);

        config.save();
    }
}
