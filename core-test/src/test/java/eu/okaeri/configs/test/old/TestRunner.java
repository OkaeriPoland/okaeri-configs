package eu.okaeri.configs.test.old;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.hjson.HjsonConfigurer;
import eu.okaeri.configs.hocon.lightbend.HoconLightbendConfigurer;
import eu.okaeri.configs.json.gson.JsonGsonConfigurer;
import eu.okaeri.configs.json.simple.JsonSimpleConfigurer;
import eu.okaeri.configs.test.old.impl.TestConfig;
import eu.okaeri.configs.test.old.serdes.LocationSerializer;
import eu.okaeri.configs.test.old.serdes.StringWorldTransformer;
import eu.okaeri.configs.validator.okaeri.OkaeriValidator;
import eu.okaeri.configs.yaml.bukkit.YamlBukkitConfigurer;
import eu.okaeri.configs.yaml.bungee.YamlBungeeConfigurer;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class TestRunner {

    public static void main(String[] args) {

        // bukkit
        config("config.bukkit.yml", new YamlBukkitConfigurer());
        empty("empty.bukkit.yml", new YamlBukkitConfigurer());
        inline("inline.bukkit.yml", new YamlBukkitConfigurer());

        // bungee
        config("config.bungee.yml", new YamlBungeeConfigurer());
        empty("empty.bungee.yml", new YamlBungeeConfigurer());
        inline("inline.bungee.yml", new YamlBungeeConfigurer());

        // snakeyaml
        config("config.snakeyaml.yml", new YamlSnakeYamlConfigurer());
        empty("empty.snakeyaml.yml", new YamlSnakeYamlConfigurer());
        inline("inline.snakeyaml.yml", new YamlSnakeYamlConfigurer());

        // gson
        config("config.gson.json", new JsonGsonConfigurer());
        empty("empty.gson.json", new JsonGsonConfigurer());
        inline("inline.gson.json", new JsonGsonConfigurer());

        // json-simple
        config("config.json-simple.json", new JsonSimpleConfigurer());
        empty("empty.json-simple.json", new JsonSimpleConfigurer());
        inline("inline.json-simple.json", new JsonSimpleConfigurer());

        // hocon
        config("config.hocon.conf", new HoconLightbendConfigurer());
        empty("empty.hocon.conf", new HoconLightbendConfigurer());
        inline("inline.hocon.conf", new HoconLightbendConfigurer());

        // hjson
        config("config.hjson.hjson", new OkaeriValidator(new HjsonConfigurer()));
        empty("empty.hjson.hjson", new HjsonConfigurer());
        inline("inline.hjson.hjson", new HjsonConfigurer());

        // convert test
        convert();
    }

    private static void convert() {

        System.out.println("#convert");
        long start = System.currentTimeMillis();

        TestConfig config = ConfigManager.create(TestConfig.class, (it) -> {
            it.withConfigurer(new HoconLightbendConfigurer());
            it.withSerdesPack(registry -> {
                registry.register(new LocationSerializer());
                registry.register(new StringWorldTransformer());
            });
            it.withBindFile("config.hocon.conf");
            it.load();
            it.withConfigurer(new HjsonConfigurer());
            it.withBindFile("config.hjson-converted.hjson");
            it.save();
            it.load();
        });

        long took = System.currentTimeMillis() - start;
        System.out.println(took + " ms");

        System.out.println(config);
    }

    private static void inline(String pathname, Configurer configurer) {

        System.out.println("#inline " + pathname);
        Path bindFile = Paths.get(pathname);

        OkaeriConfig inline = new OkaeriConfig() {
            String test = "siema";
            String hmm = "działa";
        };

        inline.updateDeclaration();
        inline.withBindFile(bindFile);
        inline.setConfigurer(configurer);

        inline.save();
//        System.out.println(inline);
    }

    private static void empty(String pathname, Configurer configurer) {

        System.out.println("#empty " + pathname);
        Path bindFile = Paths.get(pathname);

        OkaeriConfig empty = new OkaeriConfig() {
        };
        empty.updateDeclaration();
        empty.withBindFile(bindFile);
        empty.setConfigurer(configurer);

        empty.set("elo", 1);
        empty.set("elon-musk", "witam dziwne");
        empty.set("elon-musk-2", "witam dziwniejsze\nhmmm hehe");

        empty.save();
        empty.load();

//        System.out.println(empty.get("elon-musk-2"));
    }

    private static void config(String pathname, Configurer configurer) {

        System.out.println("#config " + pathname);
        long start = System.currentTimeMillis();

        TestConfig config = ConfigManager.create(TestConfig.class, (it) -> {
            it.withConfigurer(configurer);
            it.withSerdesPack(registry -> {
                registry.register(new LocationSerializer());
                registry.register(new StringWorldTransformer());
            });
            it.withBindFile(pathname);
            it.saveDefaults();
            it.load(true);

            System.out.println(it.getDeclaration().getFieldMap());
        });

        long took = System.currentTimeMillis() - start;
        System.out.println(took + " ms");

        System.out.println(config);
//        config.save();
    }
}
