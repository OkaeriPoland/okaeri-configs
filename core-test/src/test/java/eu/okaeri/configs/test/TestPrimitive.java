package eu.okaeri.configs.test;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.configurer.Configurer;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import lombok.SneakyThrows;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

@RunWith(JUnitParamsRunner.class)
public class TestPrimitive {

    Object[][] withConfigurer() {
        return Orchestrator.withConfigurer();
    }

    @Test
    @SneakyThrows
    @Parameters(method = "withConfigurer")
    public void test_simple_string_1(Configurer configurer) {

        OkaeriConfig config = new OkaeriConfig() {
            byte value = Byte.MAX_VALUE;
        };

        Path path = Paths.get("C:\\Users\\user\\Workspace\\IdeaProjects\\okaeri-configs\\core-test\\src\\test\\resources\\result\\primitive\\byte\\MAX_VALUE");
        Path file = path.resolve(configurer.getClass().getSimpleName() + ".txt");

        Files.createFile(file);
        Files.write(file, Arrays.asList(config.withConfigurer(configurer).saveToString().split("\n")), StandardCharsets.UTF_8);
    }
}
