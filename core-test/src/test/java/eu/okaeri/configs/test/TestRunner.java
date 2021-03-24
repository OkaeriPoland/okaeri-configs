package eu.okaeri.configs.test;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.bukkit.BukkitConfigurer;
import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.gson.GsonConfigurer;
import eu.okaeri.configs.json_simple.JsonSimpleConfigurer;
import eu.okaeri.configs.hocon.HoconConfigurer;
import eu.okaeri.configs.hjson.HjsonConfigurer;
import eu.okaeri.configs.test.impl.TestConfig;
import lombok.SneakyThrows;

import java.io.File;

public final class TestRunner {

    @SneakyThrows
    public static void main(String[] args) {

        // bukkit
        config("config.bukkit.yml", new BukkitConfigurer("# ", ""));
        empty("empty.bukkit.yml", new BukkitConfigurer());
        inline("inline.bukkit.yml", new BukkitConfigurer());

        // gson
        config("config.gson.json", new GsonConfigurer());
        empty("empty.gson.json", new GsonConfigurer());
        inline("inline.gson.json", new GsonConfigurer());

        // json-simple
        config("config.json-simple.json", new JsonSimpleConfigurer());
        empty("empty.json-simple.json", new JsonSimpleConfigurer());
        inline("inline.json-simple.json", new JsonSimpleConfigurer());

        // hocon
        config("config.hocon.conf", new HoconConfigurer());
        empty("empty.hocon.conf", new HoconConfigurer());
        inline("inline.hocon.conf", new HoconConfigurer());

        // hocon
        config("config.hjson.hjson", new HjsonConfigurer());
        empty("empty.hjson.hjson", new HjsonConfigurer());
        inline("inline.hjson.hjson", new HjsonConfigurer());
    }

    @SneakyThrows
    private static void inline(String pathname, Configurer configurer) {

        System.out.println("#inline " + pathname);
        File bindFile = new File(pathname);

        OkaeriConfig inline = new OkaeriConfig() {
            String test = "siema";
            String hmm = "działa";
        };

        inline.updateDeclaration();
        inline.setBindFile(bindFile);
        inline.setConfigurer(configurer);

        inline.save();
//        System.out.println(inline);
    }

    @SneakyThrows
    private static void empty(String pathname, Configurer configurer) {

        System.out.println("#empty " + pathname);
        File bindFile = new File(pathname);

        OkaeriConfig empty = new OkaeriConfig() {};
        empty.updateDeclaration();
        empty.setBindFile(bindFile);
        empty.setConfigurer(configurer);

        empty.set("elo", 1);
        empty.set("elon-musk", "witam dziwne");
        empty.set("elon-musk-2", "witam dziwniejsze\nhmmm hehe");

        empty.save();
        empty.load();

//        System.out.println(empty.get("elon-musk-2"));
    }

    @SneakyThrows
    private static void config(String pathname, Configurer configurer) {

        System.out.println("#config " + pathname);
        long start = System.currentTimeMillis();

        TestConfig config = ConfigManager.create(TestConfig.class, (it) -> {
            it.withConfigurer(configurer);
            it.withSerdesPack(registry -> registry.register(new LocationSerializer()));
            it.withBindFile(pathname);
            it.saveDefaults();
            it.load(true);
        });

        long took = System.currentTimeMillis() - start;
        System.out.println(took + " ms");

        System.out.println(config);
//        config.save();
    }
}
