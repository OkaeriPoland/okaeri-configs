package eu.okaeri.configs.test.configs;

import eu.okaeri.configs.OkaeriConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Set;

/**
 * Test config for collection types (List, Set).
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class CollectionsTestConfig extends OkaeriConfig {

    private List<String> stringList = List.of("alpha", "beta", "gamma");
    private List<Integer> intList = List.of(1, 2, 3, 5, 8);
    private Set<String> stringSet = Set.of("one", "two", "three");
    private Set<Integer> intSet = Set.of(10, 20, 30);

    // Empty collections
    private List<String> emptyList = List.of();
    private Set<String> emptySet = Set.of();

    // Nested collections
    private List<List<String>> nestedList = List.of(
        List.of("a", "b"),
        List.of("c", "d")
    );
}
