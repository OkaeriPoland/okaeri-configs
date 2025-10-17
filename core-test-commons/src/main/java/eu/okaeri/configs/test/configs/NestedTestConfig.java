package eu.okaeri.configs.test.configs;

import eu.okaeri.configs.OkaeriConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Test config for nested subconfigs.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class NestedTestConfig extends OkaeriConfig {

    private SubConfig singleNested = new SubConfig("default", 42);
    private List<SubConfig> nestedList = List.of(
        new SubConfig("first", 10),
        new SubConfig("second", 20)
    );
    private Map<String, SubConfig> nestedMap = Map.of(
        "config1", new SubConfig("map1", 100),
        "config2", new SubConfig("map2", 200)
    );

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = false)
    public static class SubConfig extends OkaeriConfig {
        private String name;
        private int value;
    }
}
