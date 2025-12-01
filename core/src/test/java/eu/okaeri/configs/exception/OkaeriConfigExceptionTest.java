package eu.okaeri.configs.exception;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.configurer.InMemoryConfigurer;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.ConfigPath;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OkaeriConfigExceptionTest {

    // ==================== Exception Building Tests ====================

    @Test
    void testExceptionMessage_SimpleField() {
        OkaeriConfigException exception = OkaeriConfigException.builder()
            .message("Failed to convert value")
            .path(ConfigPath.of("maxPlayers"))
            .expectedType(Integer.class)
            .actualValue("not_a_number")
            .build();

        assertThat(exception.getMessage()).contains("maxPlayers");
        assertThat(exception.getMessage()).contains("Integer");
        assertThat(exception.getMessage()).contains("not_a_number");
        assertThat(exception.getMessage()).contains("String");
    }

    @Test
    void testExceptionMessage_NestedPath() {
        OkaeriConfigException exception = OkaeriConfigException.builder()
            .path(ConfigPath.of("database").property("connection").property("port"))
            .expectedType(Integer.class)
            .actualValue("invalid")
            .build();

        assertThat(exception.getMessage()).contains("database.connection.port");
    }

    @Test
    void testExceptionMessage_ListIndex() {
        OkaeriConfigException exception = OkaeriConfigException.builder()
            .path(ConfigPath.of("servers").index(2).property("host"))
            .expectedType(String.class)
            .actualValue(123)
            .build();

        assertThat(exception.getMessage()).contains("servers[2].host");
    }

    @Test
    void testExceptionMessage_MapKey() {
        OkaeriConfigException exception = OkaeriConfigException.builder()
            .path(ConfigPath.of("settings").key("api-key"))
            .expectedType(String.class)
            .actualValue(null)
            .build();

        assertThat(exception.getMessage()).contains("settings[\"api-key\"]");
    }

    @Test
    void testExceptionMessage_WithGenerics() {
        OkaeriConfigException exception = OkaeriConfigException.builder()
            .path(ConfigPath.of("users"))
            .expectedType(GenericsDeclaration.of(List.class, List.of(String.class)))
            .actualValue("not_a_list")
            .build();

        assertThat(exception.getMessage()).contains("List<String>");
    }

    @Test
    void testShortDescription() {
        OkaeriConfigException exception = OkaeriConfigException.builder()
            .path(ConfigPath.of("database").property("port"))
            .expectedType(Integer.class)
            .actualValue("invalid")
            .build();

        String shortDesc = exception.getShortDescription();
        assertThat(shortDesc).contains("database.port");
        assertThat(shortDesc).contains("expected Integer");
    }

    // ==================== Integration Tests ====================

    @Test
    void testLoadConfig_InvalidIntegerField_ExceptionContainsPath() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("number", "not_a_number");

        assertThatThrownBy(() -> {
            SimpleConfig config = ConfigManager.create(SimpleConfig.class);
            config.setConfigurer(new InMemoryConfigurer(data));
            config.update(); // load from InMemoryConfigurer
        })
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException configEx = (OkaeriConfigException) ex;
                assertThat(configEx.getPath()).isNotNull();
                assertThat(configEx.getPath().toString()).isEqualTo("number");
            });
    }

    @Test
    void testLoadConfig_DecimalInIntegerField_ExceptionContainsPath() {
        // This is the exact case from the original issue:
        // field: int number; value: 0.1
        // Previously threw: ArithmeticException: Rounding necessary
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("number", 0.1);

        assertThatThrownBy(() -> {
            SimpleConfig config = ConfigManager.create(SimpleConfig.class);
            config.setConfigurer(new InMemoryConfigurer(data));
            config.update();
        })
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException configEx = (OkaeriConfigException) ex;
                assertThat(configEx.getPath()).isNotNull();
                assertThat(configEx.getPath().toString()).isEqualTo("number");
                assertThat(configEx.getExpectedType()).isNotNull();
                assertThat(configEx.getExpectedType().getType()).isEqualTo(Integer.class);
                // Should preserve original Double value, not intermediate String
                assertThat(configEx.getActualValue()).isEqualTo(0.1);
                assertThat(configEx.getActualType()).isEqualTo(Double.class);
                // Should contain helpful info in message
                assertThat(configEx.getMessage()).contains("number");
                assertThat(configEx.getMessage()).contains("0.1");
                assertThat(configEx.getMessage()).contains("Double");
            });
    }

    @Test
    void testLoadConfig_InvalidNestedField_ExceptionContainsNestedPath() {
        Map<String, Object> nestedData = new LinkedHashMap<>();
        nestedData.put("port", "invalid_port");

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("database", nestedData);

        assertThatThrownBy(() -> {
            NestedConfig config = ConfigManager.create(NestedConfig.class);
            config.setConfigurer(new InMemoryConfigurer(data));
            config.update(); // load from InMemoryConfigurer
        })
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException configEx = (OkaeriConfigException) ex;
                assertThat(configEx.getPath()).isNotNull();
                // Full path from root including parent field
                assertThat(configEx.getPath().toString()).isEqualTo("database.port");
            });
    }

    @Test
    void testLoadConfig_InvalidListElement_ExceptionContainsIndex() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("numbers", Arrays.asList(1, 2, "not_a_number", 4));

        assertThatThrownBy(() -> {
            ListConfig config = ConfigManager.create(ListConfig.class);
            config.setConfigurer(new InMemoryConfigurer(data));
            config.update(); // load from InMemoryConfigurer
        })
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException configEx = (OkaeriConfigException) ex;
                assertThat(configEx.getPath()).isNotNull();
                // Full path with field name and index
                assertThat(configEx.getPath().toString()).isEqualTo("numbers[2]");
            });
    }

    @Test
    void testLoadConfig_InvalidMapValue_ExceptionContainsKey() {
        Map<String, Object> limits = new LinkedHashMap<>();
        limits.put("daily", 100);
        limits.put("weekly", "not_a_number");

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("limits", limits);

        assertThatThrownBy(() -> {
            MapConfig config = ConfigManager.create(MapConfig.class);
            config.setConfigurer(new InMemoryConfigurer(data));
            config.update(); // load from InMemoryConfigurer
        })
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException configEx = (OkaeriConfigException) ex;
                assertThat(configEx.getPath()).isNotNull();
                // Full path with field name and map key
                assertThat(configEx.getPath().toString()).isEqualTo("limits[\"weekly\"]");
            });
    }

    @Test
    void testLoadConfig_InvalidEnumValue_ExceptionContainsEnumInfo() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("level", "INVALID_LEVEL");

        assertThatThrownBy(() -> {
            EnumConfig config = ConfigManager.create(EnumConfig.class);
            config.setConfigurer(new InMemoryConfigurer(data));
            config.update(); // load from InMemoryConfigurer
        })
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException configEx = (OkaeriConfigException) ex;
                assertThat(configEx.getMessage()).contains("level");
                assertThat(configEx.getMessage()).contains("INVALID_LEVEL");
                assertThat(configEx.getMessage()).contains("LOW");
                assertThat(configEx.getMessage()).contains("MEDIUM");
                assertThat(configEx.getMessage()).contains("HIGH");
            });
    }

    @Test
    void testLoadConfig_DeeplyNestedError_ExceptionContainsFullPath() {
        Map<String, Object> innermost = new LinkedHashMap<>();
        innermost.put("value", "not_an_integer");

        Map<String, Object> inner = new LinkedHashMap<>();
        inner.put("level3", innermost);

        Map<String, Object> outer = new LinkedHashMap<>();
        outer.put("level2", inner);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("level1", outer);

        assertThatThrownBy(() -> {
            DeepNestedConfig config = ConfigManager.create(DeepNestedConfig.class);
            config.setConfigurer(new InMemoryConfigurer(data));
            config.update(); // load from InMemoryConfigurer
        })
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException configEx = (OkaeriConfigException) ex;
                assertThat(configEx.getPath()).isNotNull();
                // Full path through all nested levels
                assertThat(configEx.getPath().toString()).isEqualTo("level1.level2.level3.value");
            });
    }

    // ==================== Test Config Classes ====================

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class SimpleConfig extends OkaeriConfig {
        private int number = 42;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class NestedConfig extends OkaeriConfig {
        private DatabaseConfig database = new DatabaseConfig();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class DatabaseConfig extends OkaeriConfig {
        private String host = "localhost";
        private int port = 5432;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class ListConfig extends OkaeriConfig {
        private List<Integer> numbers = Arrays.asList(1, 2, 3);
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class MapConfig extends OkaeriConfig {
        private Map<String, Integer> limits = new LinkedHashMap<>();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class EnumConfig extends OkaeriConfig {
        private Level level = Level.MEDIUM;
    }

    public enum Level {
        LOW, MEDIUM, HIGH
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class DeepNestedConfig extends OkaeriConfig {
        private Level1 level1 = new Level1();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class Level1 extends OkaeriConfig {
        private Level2 level2 = new Level2();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class Level2 extends OkaeriConfig {
        private Level3 level3 = new Level3();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class Level3 extends OkaeriConfig {
        private int value = 100;
    }
}
