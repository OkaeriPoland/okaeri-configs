package eu.okaeri.configs.test.configs;

import eu.okaeri.configs.OkaeriConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.*;

/**
 * Test config for enum types.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class EnumsTestConfig extends OkaeriConfig {

    public enum TestEnum {
        FIRST, SECOND, THIRD
    }

    private TestEnum singleEnum = TestEnum.SECOND;
    private List<TestEnum> enumList = List.of(TestEnum.FIRST, TestEnum.THIRD);
    private Set<TestEnum> enumSet = Set.of(TestEnum.FIRST, TestEnum.SECOND);

    private Map<TestEnum, String> enumKeyMap = Map.of(
            TestEnum.FIRST, "first value",
            TestEnum.SECOND, "second value"
    );

    private Map<String, TestEnum> enumValueMap = Map.of(
            "a", TestEnum.FIRST,
            "b", TestEnum.THIRD
    );
}
