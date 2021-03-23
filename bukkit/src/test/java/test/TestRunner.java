package test;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.bukkit.BukkitConfigurer;
import lombok.SneakyThrows;
import org.bukkit.configuration.file.YamlConfiguration;
import test.impl.TestConfig;

import java.io.File;

public final class TestRunner {

    @SneakyThrows
    public static void main(String[] args) {
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

        inline.updateDeclaration();
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
        empty.updateDeclaration();
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

        long start = System.currentTimeMillis();

        TestConfig config = ConfigManager.create(TestConfig.class, (it) -> {
            it.withConfigurer(new BukkitConfigurer());
            it.withSerdesPack(registry -> registry.register(new LocationSerializer()));
            it.withBindFile("config.yml");
            it.saveDefaults();
            it.load(true);
        });

        long took = System.currentTimeMillis() - start;
        System.out.println(took + " ms");

        System.out.println(config);
//        config.save();
    }
}
