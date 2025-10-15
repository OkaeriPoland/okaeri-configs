package eu.okaeri.configs.test.configs;

import eu.okaeri.configs.OkaeriConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.*;

/**
 * Test config for Serializable custom objects.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class SerializableTestConfig extends OkaeriConfig {

    private CustomSerializable singleObject = new CustomSerializable("test", 999);
    private List<CustomSerializable> objectList = List.of(
            new CustomSerializable("item1", 1),
            new CustomSerializable("item2", 2)
    );
    private Map<String, CustomSerializable> objectMap = Map.of(
            "obj1", new CustomSerializable("first", 10),
            "obj2", new CustomSerializable("second", 20)
    );

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomSerializable implements Serializable {
        private static final long serialVersionUID = 1L;
        private String name;
        private int id;
    }
}
