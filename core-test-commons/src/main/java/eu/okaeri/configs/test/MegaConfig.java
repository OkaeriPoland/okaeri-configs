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

    @Comment("Long string without spaces (tests line wrapping)")
    private String longStringNoSpaces = "a".repeat(200);

    @Comment("Long string with spaces (tests folding behavior)")
    private String longStringWithSpaces = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.";

    @Comment("Multiline string (tests literal/folded style)")
    private String multilineString = "Line 1\nLine 2\nLine 3\nLine 4 with more text\nLine 5 final";

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
    private Set<String> stringSet = new LinkedHashSet<>(List.of("one", "two", "three"));

    @Comment("Set of enums")
    private Set<TestEnum> enumSet = new LinkedHashSet<>(List.of(TestEnum.FIRST, TestEnum.SECOND));

    @Comment("Nested collection - list of lists")
    private List<List<String>> nestedListOfLists = List.of(
        List.of("a", "b"),
        List.of("c", "d", "e")
    );

    // === MAPS ===
    @Comment("Simple string-to-string map")
    private Map<String, String> simpleMap = new LinkedHashMap<>() {{
        this.put("key1", "value1");
        this.put("key2", "value2");
    }};

    @Comment("Map with integer keys")
    private Map<Integer, String> intKeyMap = new LinkedHashMap<>() {{
        this.put(1, "one");
        this.put(2, "two");
    }};

    @Comment("Nested map")
    private Map<String, Map<String, Integer>> nestedMap = new LinkedHashMap<>() {{
        this.put("group1", new LinkedHashMap<>() {{
            this.put("a", 1);
            this.put("b", 2);
        }});
    }};

    @Comment("Map with enum keys")
    private Map<TestEnum, String> enumKeyMap = new LinkedHashMap<>() {{
        this.put(TestEnum.FIRST, "first value");
        this.put(TestEnum.SECOND, "second value");
    }};

    @Comment("Map with enum values")
    private Map<String, TestEnum> enumValueMap = new LinkedHashMap<>() {{
        this.put("a", TestEnum.FIRST);
        this.put("b", TestEnum.THIRD);
    }};

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

    // === MULTI-LEVEL SELF-REFERENTIAL NESTING ===
    @Comment({"Complex multi-level subconfig", "MegaConfig nested inside itself (2 levels deep)", "Note: Set to null by default to avoid infinite recursion", "Call populateNestedMegaConfig() after construction to initialize"})
    private MegaConfig nestedMegaConfig = null;

    // === SERIALIZABLE ===
    @Comment("Serializable custom object")
    private CustomSerializable customObj = new CustomSerializable("test", 999);

    @Comment("List of serializable objects")
    private List<CustomSerializable> serializableList = List.of(
        new CustomSerializable("item1", 1),
        new CustomSerializable("item2", 2),
        new CustomSerializable("item3", 3)
    );

    @Comment("Map of serializable objects")
    private Map<String, CustomSerializable> serializableMap = new LinkedHashMap<>() {{
        this.put("obj1", new CustomSerializable("first", 100));
        this.put("obj2", new CustomSerializable("second", 200));
    }};

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
    private Map<String, String> emptyMap = new LinkedHashMap<>();

    @Comment("Field with repeating comments")
    @Comment("This tests multiple @Comment annotations")
    @Comment("on the same field")
    private String repeatingCommentField = "test value";

    // === FACTORY METHOD FOR NESTED MEGACONFIG ===

    /**
     * Populates the nestedMegaConfig field with a full MegaConfig instance.
     * This creates a 2-level deep nesting structure (the nested instance has nestedMegaConfig = null).
     * Must be called after construction to avoid infinite recursion.
     */
    public void populateNestedMegaConfig() {
        if (this.nestedMegaConfig == null) {
            this.nestedMegaConfig = new MegaConfig();
            // The nested instance keeps its nestedMegaConfig as null (no further nesting)
        }
    }

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

        @Comment("Name field in serializable object")
        private String name;

        @Comment("ID field in serializable object")
        private int id;
    }
}
