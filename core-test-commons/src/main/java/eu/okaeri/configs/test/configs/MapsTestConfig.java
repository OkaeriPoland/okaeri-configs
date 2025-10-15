package eu.okaeri.configs.test.configs;

import eu.okaeri.configs.OkaeriConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.*;

/**
 * Test config for map types with various key/value combinations.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class MapsTestConfig extends OkaeriConfig {

    private Map<String, String> simpleMap = Map.of(
            "key1", "value1",
            "key2", "value2"
    );

    private Map<Integer, String> intKeyMap = Map.of(
            1, "one",
            2, "two"
    );

    private Map<String, Integer> intValueMap = Map.of(
            "a", 100,
            "b", 200
    );

    private Map<String, List<String>> complexValueMap = Map.of(
            "group1", List.of("item1", "item2"),
            "group2", List.of("item3", "item4")
    );

    private Map<String, Map<String, Integer>> nestedMap = Map.of(
            "outer", Map.of(
                    "inner1", 1,
                    "inner2", 2
            )
    );

    private Map<String, String> emptyMap = Map.of();
}
