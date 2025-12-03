package eu.okaeri.configs.exception;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.configurer.InMemoryConfigurer;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.ConfigPath;
import eu.okaeri.configs.serdes.DeserializationData;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.serdes.SerializationData;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
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

        assertThat(exception.getMessage()).isEqualTo(
            "Failed to convert value 'maxPlayers' to Integer from String: \"not_a_number\"");
    }

    @Test
    void testExceptionMessage_NestedPath() {
        OkaeriConfigException exception = OkaeriConfigException.builder()
            .path(ConfigPath.of("database").property("connection").property("port"))
            .expectedType(Integer.class)
            .actualValue("invalid")
            .build();

        assertThat(exception.getMessage()).isEqualTo(
            "Failed to load 'database.connection.port' to Integer from String: \"invalid\"");
    }

    @Test
    void testExceptionMessage_ListIndex() {
        OkaeriConfigException exception = OkaeriConfigException.builder()
            .path(ConfigPath.of("servers").index(2).property("host"))
            .expectedType(String.class)
            .actualValue(123)
            .build();

        assertThat(exception.getMessage()).isEqualTo(
            "Failed to load 'servers[2].host' to String from Integer: 123");
    }

    @Test
    void testExceptionMessage_MapKey() {
        OkaeriConfigException exception = OkaeriConfigException.builder()
            .path(ConfigPath.of("settings").key("api-key"))
            .expectedType(String.class)
            .actualValue(null)
            .build();

        assertThat(exception.getMessage()).isEqualTo(
            "Failed to load 'settings[\"api-key\"]' to String");
    }

    @Test
    void testExceptionMessage_WithGenerics() {
        OkaeriConfigException exception = OkaeriConfigException.builder()
            .path(ConfigPath.of("users"))
            .expectedType(GenericsDeclaration.of(List.class, List.of(String.class)))
            .actualValue("not_a_list")
            .build();

        assertThat(exception.getMessage()).isEqualTo(
            "Failed to load 'users' to List<String> from String: \"not_a_list\"");
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
            config.setConfigurer(new InMemoryConfigurer());
            config.setInternalState(data);
            config.update();
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
            config.setConfigurer(new InMemoryConfigurer());
            config.setInternalState(data);
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
            config.setConfigurer(new InMemoryConfigurer());
            config.setInternalState(data);
            config.update();
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
            config.setConfigurer(new InMemoryConfigurer());
            config.setInternalState(data);
            config.update();
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
            config.setConfigurer(new InMemoryConfigurer());
            config.setInternalState(data);
            config.update();
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
            config.setConfigurer(new InMemoryConfigurer());
            config.setInternalState(data);
            config.update();
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
            config.setConfigurer(new InMemoryConfigurer());
            config.setInternalState(data);
            config.update();
        })
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException configEx = (OkaeriConfigException) ex;
                assertThat(configEx.getPath()).isNotNull();
                // Full path through all nested levels
                assertThat(configEx.getPath().toString()).isEqualTo("level1.level2.level3.value");
            });
    }

    // ==================== ObjectSerializer Path Propagation Tests ====================

    @Test
    void testLoadConfig_CustomSerializer_PathPropagatesThroughNestedGet() {
        // Test that path propagates when ObjectSerializer calls data.get() for nested properties
        Map<String, Object> personData = new LinkedHashMap<>();
        personData.put("name", "John");
        personData.put("age", "not_a_number"); // Invalid - should fail with path person.age

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("person", personData);

        assertThatThrownBy(() -> {
            CustomSerializerConfig config = ConfigManager.create(CustomSerializerConfig.class);
            InMemoryConfigurer configurer = new InMemoryConfigurer();
            configurer.getRegistry().register(new PersonSerializer());
            config.setConfigurer(configurer);
            config.setInternalState(data);
            config.update();
        })
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException configEx = (OkaeriConfigException) ex;
                assertThat(configEx.getPath()).isNotNull();
                // Path should include both the field name and the property from inside the serializer
                assertThat(configEx.getPath().toString()).isEqualTo("person.age");
            });
    }

    @Test
    void testLoadConfig_MissingSerializer_SuggestsRegistration() {
        // Test that missing serializer gives a helpful error message
        Map<String, Object> personData = new LinkedHashMap<>();
        personData.put("name", "John");
        personData.put("age", 25);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("person", personData);

        assertThatThrownBy(() -> {
            CustomSerializerConfig config = ConfigManager.create(CustomSerializerConfig.class);
            InMemoryConfigurer configurer = new InMemoryConfigurer();
            // Intentionally NOT registering PersonSerializer
            config.setConfigurer(configurer);
            config.setInternalState(data);
            config.update();
        })
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException configEx = (OkaeriConfigException) ex;
                assertThat(configEx.getPath()).isNotNull();
                assertThat(configEx.getPath().toString()).isEqualTo("person");
                // Should suggest registering a serializer
                assertThat(configEx.getMessage()).contains("No serializer found");
                assertThat(configEx.getMessage()).contains("Person");
                assertThat(configEx.getMessage()).contains("ObjectSerializer");
            });
    }

    @Test
    void testLoadConfig_CustomSerializer_PathPropagatesThroughNestedList() {
        // Test that path propagates when ObjectSerializer calls data.getAsList()
        Map<String, Object> teamData = new LinkedHashMap<>();
        teamData.put("name", "Engineering");
        teamData.put("memberIds", Arrays.asList(1, 2, "not_a_number", 4)); // Invalid at index 2

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("team", teamData);

        assertThatThrownBy(() -> {
            TeamConfig config = ConfigManager.create(TeamConfig.class);
            InMemoryConfigurer configurer = new InMemoryConfigurer();
            configurer.getRegistry().register(new TeamSerializer());
            config.setConfigurer(configurer);
            config.setInternalState(data);
            config.update();
        })
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException configEx = (OkaeriConfigException) ex;
                assertThat(configEx.getPath()).isNotNull();
                // Path should include field, property from serializer, and list index
                assertThat(configEx.getPath().toString()).isEqualTo("team.memberIds[2]");
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

    // ==================== Custom Serializer Test Classes ====================

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class CustomSerializerConfig extends OkaeriConfig {
        private Person person = new Person("Default", 0);
    }

    @Data
    public static class Person {
        private final String name;
        private final int age;
    }

    public static class PersonSerializer implements ObjectSerializer<Person> {
        @Override
        public boolean supports(@NonNull Class<?> type) {
            return Person.class.isAssignableFrom(type);
        }

        @Override
        public void serialize(@NonNull Person object, @NonNull SerializationData data, @NonNull GenericsDeclaration generics) {
            data.set("name", object.getName());
            data.set("age", object.getAge());
        }

        @Override
        public Person deserialize(@NonNull DeserializationData data, @NonNull GenericsDeclaration generics) {
            // These calls should propagate the path context
            String name = data.get("name", String.class);
            int age = data.get("age", Integer.class); // Will fail if "age" is not a valid integer
            return new Person(name, age);
        }
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class TeamConfig extends OkaeriConfig {
        private Team team = new Team("Default", Arrays.asList(1, 2, 3));
    }

    @Data
    public static class Team {
        private final String name;
        private final List<Integer> memberIds;
    }

    public static class TeamSerializer implements ObjectSerializer<Team> {
        @Override
        public boolean supports(@NonNull Class<?> type) {
            return Team.class.isAssignableFrom(type);
        }

        @Override
        public void serialize(@NonNull Team object, @NonNull SerializationData data, @NonNull GenericsDeclaration generics) {
            data.set("name", object.getName());
            data.setCollection("memberIds", object.getMemberIds(), Integer.class);
        }

        @Override
        public Team deserialize(@NonNull DeserializationData data, @NonNull GenericsDeclaration generics) {
            String name = data.get("name", String.class);
            // This call should propagate path context through the list elements
            List<Integer> memberIds = data.getAsList("memberIds", Integer.class);
            return new Team(name, memberIds);
        }
    }
}
