package test;

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

        long start = System.currentTimeMillis();

        TestConfig config = (TestConfig) new TestConfig()
                .withConfigurer(new BukkitConfigurer())
                .withSerdesPack(registry -> registry.register(new LocationSerializer()))
                .withBindFile("config.yml")
                .saveDefaults()
                .load(true);

        long took = System.currentTimeMillis() - start;
        System.out.println(took + " ms");

        System.out.println(config);
//        config.save();
    }
}
