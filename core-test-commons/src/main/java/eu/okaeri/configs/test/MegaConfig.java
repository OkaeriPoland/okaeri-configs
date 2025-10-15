package eu.okaeri.configs.test;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/**
 * Comprehensive config that exercises ALL okaeri-configs features.
 * Used for E2E format testing.
 * <p>
 * This config is saved to a file and used by all format implementations
 * to ensure consistent behavior across formats.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Header("===========================================")
@Header("  Okaeri Configs - Mega Test Config")
@Header("  Tests ALL features comprehensively")
@Header("===========================================")
@Names(strategy = NameStrategy.IDENTITY, modifier = NameModifier.NONE)
public class MegaConfig extends OkaeriConfig {

    // === PRIMITIVES ===
    @Comment("Boolean primitive and wrapper")
    private boolean primBool = true;
    private Boolean wrapBool = false;

    @Comment("Numeric primitives")
    private byte primByte = 127;
    private short primShort = 32000;
    private int primInt = 2147483647;
    private long primLong = 9223372036854775807L;
    private float primFloat = 3.14159f;
    private double primDouble = 2.718281828;
    private char primChar = 'Ω';

    // === WRAPPERS ===
    @Comment("Wrapper types")
    private Byte wrapByte = 100;
    private Short wrapShort = 30000;
    private Integer wrapInt = 123456;
    private Long wrapLong = 987654321L;
    private Float wrapFloat = 1.414f;
    private Double wrapDouble = 1.732;
    private Character wrapChar = '€';

    // === STRINGS ===
    @Comment({"String tests", "including unicode and special chars"})
    private String simpleString = "Hello, World!";
    private String unicodeJapanese = "こんにちは世界 🌍";
    private String unicodeRussian = "Привет мир! Тестирование кириллицы";
    private String unicodePolish = "Część świecie! Łódź, Gdańsk, Kraków, źdźbło";
    private String specialChars = "!@#$%^&*()_+-=[]{}|;':\"<>?,./";
    private String emptyString = "";

    // === BIG NUMBERS ===
    @Comment("Math types for precision")
    private BigInteger bigInt = new BigInteger("999999999999999999999999999999");
    private BigDecimal bigDec = new BigDecimal("123.456789012345678901234567890");

    // === COLLECTIONS ===
    @Comment("List of strings")
    private List<String> stringList = List.of("alpha", "beta", "gamma");

    @Comment("List of integers")
    private List<Integer> intList = List.of(1, 2, 3, 5, 8, 13);

    @Comment("Set of strings (order preserved)")
    private Set<String> stringSet = Set.of("one", "two", "three");

    @Comment("Set of enums")
    private Set<TestEnum> enumSet = Set.of(TestEnum.FIRST, TestEnum.SECOND);

    // === MAPS ===
    @Comment("Simple string-to-string map")
    private Map<String, String> simpleMap = Map.of(
            "key1", "value1",
            "key2", "value2"
    );

    @Comment("Map with integer keys")
    private Map<Integer, String> intKeyMap = Map.of(
            1, "one",
            2, "two"
    );

    @Comment("Nested map")
    private Map<String, Map<String, Integer>> nestedMap = Map.of(
            "group1", Map.of(
                    "a", 1,
                    "b", 2
            )
    );

    @Comment("Map with enum keys")
    private Map<TestEnum, String> enumKeyMap = Map.of(
            TestEnum.FIRST, "first value",
            TestEnum.SECOND, "second value"
    );

    // === ENUMS ===
    @Comment("Simple enum")
    private TestEnum singleEnum = TestEnum.THIRD;

    @Comment("List of enums")
    private List<TestEnum> enumList = List.of(TestEnum.FIRST, TestEnum.THIRD);

    // === NESTED CONFIGS ===
    @Comment("Nested subconfig")
    private SubConfig subConfig = new SubConfig();

    @Comment("List of nested configs")
    private List<SubConfig> subConfigList = List.of(
            new SubConfig("sub1", 10),
            new SubConfig("sub2", 20)
    );

    // === SERIALIZABLE ===
    @Comment("Serializable custom object")
    private CustomSerializable customObj = new CustomSerializable("test", 999);

    // === ANNOTATIONS ===
    @CustomKey("custom-key-field")
    @Comment("Field with custom key")
    private String customKeyField = "custom value";

    @Variable("TEST_VARIABLE")
    @Comment("Field backed by environment variable")
    private String variableField = "default";

    @Exclude
    private String excludedField = "should not appear";

    // === EDGE CASES ===
    @Comment("Null value test")
    private String nullValue = null;

    @Comment("Empty collection")
    private List<String> emptyList = List.of();

    @Comment("Empty map")
    private Map<String, String> emptyMap = Map.of();

    // === NESTED CLASSES ===

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = false)
    public static class SubConfig extends OkaeriConfig {
        @Comment("Subconfig field")
        private String subField = "default sub";
        private int subNumber = 42;
    }

    public enum TestEnum {
        FIRST, SECOND, THIRD
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomSerializable implements Serializable {
        private static final long serialVersionUID = 1L;
        private String name;
        private int id;
    }
}
