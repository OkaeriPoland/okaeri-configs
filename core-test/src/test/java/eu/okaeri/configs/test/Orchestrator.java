package eu.okaeri.configs.test;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.hjson.HjsonConfigurer;
import eu.okaeri.configs.hocon.lightbend.HoconLightbendConfigurer;
import eu.okaeri.configs.json.gson.JsonGsonConfigurer;
import eu.okaeri.configs.json.simple.JsonSimpleConfigurer;
import eu.okaeri.configs.yaml.bukkit.YamlBukkitConfigurer;
import eu.okaeri.configs.yaml.bungee.YamlBungeeConfigurer;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;
import lombok.SneakyThrows;

import java.nio.file.Paths;
import java.util.function.Function;

public final class Orchestrator {

    public static final String BASE_PATH = "./core-test/src/test/resources/data/";

    public static <T> String getTarget(String target, Configurer configurer, T value, Function<T, String> nameFunction) {
        Object object = (nameFunction == null) ? value : nameFunction.apply(value);
        return String.format(BASE_PATH + target + "/" + configurer.getClass().getSimpleName() + ".txt", object);
    }

    @SneakyThrows
    public static <T> Function<T, String> numName(Class<T> type) {

        Object MIN_VALUE = type.getField("MIN_VALUE").get(null);
        Object MAX_VALUE = type.getField("MAX_VALUE").get(null);

        return (value) -> {
            if (value == MIN_VALUE) {
                return "MIN_VALUE";
            } else if (value == MAX_VALUE) {
                return "MAX_VALUE";
            } else {
                return String.format("%s", value);
            }
        };
    }

    public static Object[][] withConfigurer() {
        return new Object[][]{
            // general use
            new Object[]{new YamlSnakeYamlConfigurer()},
            new Object[]{new HjsonConfigurer()},
            new Object[]{new JsonGsonConfigurer()},
            new Object[]{new JsonSimpleConfigurer()},
            new Object[]{new HoconLightbendConfigurer()},
            // environment dependant
            new Object[]{new YamlBukkitConfigurer()},
            new Object[]{new YamlBungeeConfigurer()}
        };
    }

    public static void main(String[] args) {
        writeSimple(new OkaeriConfig() {
            boolean value;
        }, "structural/primitive/boolean/%s", boolean.class, true, false);
        writeSimple(new OkaeriConfig() {
            byte value;
        }, "structural/primitive/byte/%s", numName(Byte.class), byte.class, Byte.MIN_VALUE, Byte.MAX_VALUE, (byte) 0, (byte) 1);
        writeSimple(new OkaeriConfig() {
            char value;
        }, "structural/primitive/char/%s", char.class, 'a', 'b', 'y', 'z', '0', '1');
        writeSimple(new OkaeriConfig() {
            double value;
        }, "structural/primitive/double/%s", numName(Double.class), double.class, Double.MIN_VALUE, Double.MAX_VALUE, 0d, 1d, 0.1d, 0.01d, 0.001d, 0.0001d, 0.00001d);
        writeSimple(new OkaeriConfig() {
            float value;
        }, "structural/primitive/float/%s", numName(Float.class), float.class, Float.MIN_VALUE, Float.MAX_VALUE, 0f, 1f, 0.1f, 0.01f, 0.001f, 0.0001f, 0.00001f);
        writeSimple(new OkaeriConfig() {
            int value;
        }, "structural/primitive/int/%s", numName(Integer.class), int.class, Integer.MIN_VALUE, Integer.MAX_VALUE, 0, 1);
        writeSimple(new OkaeriConfig() {
            long value;
        }, "structural/primitive/long/%s", numName(Long.class), long.class, Long.MIN_VALUE, Long.MAX_VALUE, 0L, 1L);
        writeSimple(new OkaeriConfig() {
            short value;
        }, "structural/primitive/short/%s", numName(Short.class), short.class, Short.MIN_VALUE, Short.MAX_VALUE, (short) 0, (short) 1);
    }

    @SafeVarargs
    public static <T> void writeSimple(OkaeriConfig config, String target, Class<T> type, T... values) {
        writeSimple(config, target, null, type, values);
    }

    @SafeVarargs
    public static <T> void writeSimple(OkaeriConfig config, String target, Function<T, String> nameFunction, Class<T> type, T... values) {
        for (Object[] configurer0 : withConfigurer()) {
            Configurer configurer = (Configurer) configurer0[0];
            writeSimple(config, configurer, target, nameFunction, type, values);
        }
    }

    @SafeVarargs
    public static <T> void writeSimple(OkaeriConfig config, Configurer configurer, String target, Function<T, String> nameFunction, Class<T> type, T... values) {
        for (T value : values) {
            config.setConfigurer(configurer);
            config.set("value", value);
            config.save(Paths.get(getTarget(target, configurer, value, nameFunction)));
        }
    }
}
