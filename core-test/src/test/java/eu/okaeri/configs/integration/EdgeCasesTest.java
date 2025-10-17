package eu.okaeri.configs.integration;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Exclude;
import eu.okaeri.configs.test.TestUtils;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for edge cases and boundary conditions.
 * Tests unusual inputs, extreme values, and error handling.
 */
class EdgeCasesTest {

    @TempDir
    Path tempDir;

    // Test config classes
    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class EmptyConfig extends OkaeriConfig {
        // No fields
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class ExcludedOnlyConfig extends OkaeriConfig {
        @Exclude
        private String excluded1 = "value1";

        @Exclude
        private int excluded2 = 42;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class L5 extends OkaeriConfig {
        private String level5 = "L5";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class L4 extends OkaeriConfig {
        private String level4 = "L4";
        private L5 l5 = new L5();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class L3 extends OkaeriConfig {
        private String level3 = "L3";
        private L4 l4 = new L4();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class L2 extends OkaeriConfig {
        private String level2 = "L2";
        private L3 l3 = new L3();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class L1 extends OkaeriConfig {
        private String level1 = "L1";
        private L2 l2 = new L2();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class LargeConfig extends OkaeriConfig {
        private List<Integer> largeList = new ArrayList<>();
        private Map<String, String> largeMap = new LinkedHashMap<>();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class UnicodeConfig extends OkaeriConfig {
        private String japanese = "こんにちは世界 🌍";
        private String russian = "Привет мир! 🎉";
        private String emoji = "🚀 🎨 🔥 ⭐ 💎";
        private Map<String, String> unicodeMap = new LinkedHashMap<>(Map.of(
            "日本語", "にほんご",
            "Русский", "русский",
            "emoji_key", "🎯"
        ));
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class SpecialCharsConfig extends OkaeriConfig {
        private String quotes = "This has \"double quotes\" and 'single quotes'";
        private String backslashes = "Path: C:\\Users\\Test\\file.txt";
        private String mixed = "Special: !@#$%^&*()_+-=[]{}|;':<>?,./";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class NullConfig extends OkaeriConfig {
        private String nullString = null;
        private Integer nullInteger = null;
        private List<String> nullList = null;
        private Map<String, String> nullMap = null;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class SimpleConfig extends OkaeriConfig {
        private String field = "value";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class SimpleConfig2 extends OkaeriConfig {
        private String field1 = "default";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class ExtremeConfig extends OkaeriConfig {
        private byte maxByte = Byte.MAX_VALUE;
        private byte minByte = Byte.MIN_VALUE;
        private short maxShort = Short.MAX_VALUE;
        private short minShort = Short.MIN_VALUE;
        private int maxInt = Integer.MAX_VALUE;
        private int minInt = Integer.MIN_VALUE;
        private long maxLong = Long.MAX_VALUE;
        private long minLong = Long.MIN_VALUE;
        private float maxFloat = Float.MAX_VALUE;
        private float minFloat = Float.MIN_VALUE;
        private double maxDouble = Double.MAX_VALUE;
        private double minDouble = Double.MIN_VALUE;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class EmptyNullConfig extends OkaeriConfig {
        private String emptyString = "";
        private String nullString = null;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class ZeroConfig extends OkaeriConfig {
        private int zeroInt = 0;
        private long zeroLong = 0L;
        private double zeroDouble = 0.0;
        private float zeroFloat = 0.0f;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class BooleanConfig extends OkaeriConfig {
        private boolean primitiveTrue = true;
        private boolean primitiveFalse = false;
        private Boolean wrapperTrue = Boolean.TRUE;
        private Boolean wrapperFalse = Boolean.FALSE;
        private Boolean wrapperNull = null;
    }

    /**
     * Empty config (no fields)
     */
    @Test
    void testEdgeCase_EmptyConfig_HandlesCorrectly() throws Exception {
        File configFile = this.tempDir.resolve("empty.yml").toFile();

        EmptyConfig config = ConfigManager.create(EmptyConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.withBindFile(configFile);
        config.save();

        // File should be created (even if empty)
        assertThat(configFile).exists();

        // Load should work
        EmptyConfig loaded = ConfigManager.create(EmptyConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer());
        loaded.withBindFile(configFile);
        loaded.load();

        assertThat(loaded).isNotNull();
    }

    /**
     * Config with only excluded fields
     */
    @Test
    void testEdgeCase_OnlyExcludedFields_HandlesCorrectly() throws Exception {
        File configFile = this.tempDir.resolve("excluded.yml").toFile();

        ExcludedOnlyConfig config = ConfigManager.create(ExcludedOnlyConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.withBindFile(configFile);
        config.save();

        // File content should be essentially empty (just comments/structure)
        String content = TestUtils.readFile(configFile);
        assertThat(content).doesNotContain("excluded1");
        assertThat(content).doesNotContain("excluded2");
        assertThat(content).doesNotContain("value1");
        assertThat(content).doesNotContain("42");
    }

    /**
     * Very deep nesting (5+ levels)
     */
    @Test
    void testEdgeCase_VeryDeepNesting_HandlesCorrectly() throws Exception {
        File configFile = this.tempDir.resolve("deep.yml").toFile();

        L1 config = ConfigManager.create(L1.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.withBindFile(configFile);
        config.save();

        // Load and verify all levels
        L1 loaded = ConfigManager.create(L1.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer());
        loaded.withBindFile(configFile);
        loaded.load();

        assertThat(loaded.getLevel1()).isEqualTo("L1");
        assertThat(loaded.getL2().getLevel2()).isEqualTo("L2");
        assertThat(loaded.getL2().getL3().getLevel3()).isEqualTo("L3");
        assertThat(loaded.getL2().getL3().getL4().getLevel4()).isEqualTo("L4");
        assertThat(loaded.getL2().getL3().getL4().getL5().getLevel5()).isEqualTo("L5");
    }

    /**
     * Very large collections (100+ items)
     */
    @Test
    void testEdgeCase_VeryLargeCollections_HandlesCorrectly() throws Exception {
        File configFile = this.tempDir.resolve("large.yml").toFile();

        LargeConfig config = ConfigManager.create(LargeConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.withBindFile(configFile);

        // Add 150 items
        for (int i = 0; i < 150; i++) {
            config.getLargeList().add(i);
            config.getLargeMap().put("key" + i, "value" + i);
        }

        config.save();

        // Load and verify
        LargeConfig loaded = ConfigManager.create(LargeConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer());
        loaded.withBindFile(configFile);
        loaded.load();

        assertThat(loaded.getLargeList()).hasSize(150);
        assertThat(loaded.getLargeMap()).hasSize(150);
        assertThat(loaded.getLargeList().get(0)).isEqualTo(0);
        assertThat(loaded.getLargeList().get(149)).isEqualTo(149);
        assertThat(loaded.getLargeMap().get("key0")).isEqualTo("value0");
        assertThat(loaded.getLargeMap().get("key149")).isEqualTo("value149");
    }

    /**
     * Unicode in keys and values (including emoji)
     */
    @Test
    void testEdgeCase_UnicodeInKeysAndValues_HandlesCorrectly() throws Exception {
        File configFile = this.tempDir.resolve("unicode.yml").toFile();

        UnicodeConfig config = ConfigManager.create(UnicodeConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.withBindFile(configFile);
        config.save();

        // Load and verify
        UnicodeConfig loaded = ConfigManager.create(UnicodeConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer());
        loaded.withBindFile(configFile);
        loaded.load();

        assertThat(loaded.getJapanese()).isEqualTo("こんにちは世界 🌍");
        assertThat(loaded.getRussian()).isEqualTo("Привет мир! 🎉");
        assertThat(loaded.getEmoji()).isEqualTo("🚀 🎨 🔥 ⭐ 💎");
        assertThat(loaded.getUnicodeMap().get("日本語")).isEqualTo("にほんご");
        assertThat(loaded.getUnicodeMap().get("Русский")).isEqualTo("русский");
        assertThat(loaded.getUnicodeMap().get("emoji_key")).isEqualTo("🎯");
    }

    /**
     * Special characters in strings (quotes, backslashes, newlines)
     */
    @Test
    void testEdgeCase_SpecialCharactersInStrings_HandlesCorrectly() throws Exception {
        File configFile = this.tempDir.resolve("special.yml").toFile();

        SpecialCharsConfig config = ConfigManager.create(SpecialCharsConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.withBindFile(configFile);
        config.save();

        // Load and verify
        SpecialCharsConfig loaded = ConfigManager.create(SpecialCharsConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer());
        loaded.withBindFile(configFile);
        loaded.load();

        assertThat(loaded.getQuotes()).isEqualTo("This has \"double quotes\" and 'single quotes'");
        assertThat(loaded.getBackslashes()).isEqualTo("Path: C:\\Users\\Test\\file.txt");
        assertThat(loaded.getMixed()).isEqualTo("Special: !@#$%^&*()_+-=[]{}|;':<>?,./");
    }

    /**
     * Null handling everywhere
     */
    @Test
    void testEdgeCase_NullHandling_WorksCorrectly() throws Exception {
        File configFile = this.tempDir.resolve("nulls.yml").toFile();

        NullConfig config = ConfigManager.create(NullConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.withBindFile(configFile);
        config.save();

        // Load and verify
        NullConfig loaded = ConfigManager.create(NullConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer());
        loaded.withBindFile(configFile);
        loaded.load();

        assertThat(loaded.getNullString()).isNull();
        assertThat(loaded.getNullInteger()).isNull();
        assertThat(loaded.getNullList()).isNull();
        assertThat(loaded.getNullMap()).isNull();
    }

    /**
     * Load non-existent file (should throw)
     */
    @Test
    void testEdgeCase_LoadNonExistentFile_ThrowsException() throws Exception {
        File nonExistent = this.tempDir.resolve("does-not-exist.yml").toFile();

        SimpleConfig config = ConfigManager.create(SimpleConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.withBindFile(nonExistent);

        assertThatThrownBy(() -> config.load())
            .isInstanceOf(RuntimeException.class);
    }

    /**
     * Load malformed YAML (should throw)
     */
    @Test
    void testEdgeCase_LoadMalformedYaml_ThrowsException() throws Exception {
        File malformedFile = this.tempDir.resolve("malformed.yml").toFile();

        // Write malformed YAML
        String malformed = """
            field1: value1
            field2: [unclosed array
            field3: value3
            """;
        TestUtils.writeFile(malformedFile, malformed);

        SimpleConfig2 config = ConfigManager.create(SimpleConfig2.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.withBindFile(malformedFile);

        assertThatThrownBy(() -> config.load())
            .isInstanceOf(RuntimeException.class);
    }

    /**
     * Extreme numeric values (max/min)
     */
    @Test
    void testEdgeCase_ExtremeNumericValues_HandlesCorrectly() throws Exception {
        File configFile = this.tempDir.resolve("extreme.yml").toFile();

        ExtremeConfig config = ConfigManager.create(ExtremeConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.withBindFile(configFile);
        config.save();

        // Load and verify
        ExtremeConfig loaded = ConfigManager.create(ExtremeConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer());
        loaded.withBindFile(configFile);
        loaded.load();

        assertThat(loaded.getMaxByte()).isEqualTo(Byte.MAX_VALUE);
        assertThat(loaded.getMinByte()).isEqualTo(Byte.MIN_VALUE);
        assertThat(loaded.getMaxShort()).isEqualTo(Short.MAX_VALUE);
        assertThat(loaded.getMinShort()).isEqualTo(Short.MIN_VALUE);
        assertThat(loaded.getMaxInt()).isEqualTo(Integer.MAX_VALUE);
        assertThat(loaded.getMinInt()).isEqualTo(Integer.MIN_VALUE);
        assertThat(loaded.getMaxLong()).isEqualTo(Long.MAX_VALUE);
        assertThat(loaded.getMinLong()).isEqualTo(Long.MIN_VALUE);
    }

    /**
     * Empty strings vs null
     */
    @Test
    void testEdgeCase_EmptyStringVsNull_DistinguishedCorrectly() throws Exception {
        File configFile = this.tempDir.resolve("empty-vs-null.yml").toFile();

        EmptyNullConfig config = ConfigManager.create(EmptyNullConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.withBindFile(configFile);
        config.save();

        // Load and verify
        EmptyNullConfig loaded = ConfigManager.create(EmptyNullConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer());
        loaded.withBindFile(configFile);
        loaded.load();

        assertThat(loaded.getEmptyString()).isEmpty();
        assertThat(loaded.getNullString()).isNull();
        assertThat(loaded.getEmptyString()).isNotNull();
    }

    /**
     * Zero values for numeric types
     */
    @Test
    void testEdgeCase_ZeroValues_HandledCorrectly() throws Exception {
        File configFile = this.tempDir.resolve("zeros.yml").toFile();

        ZeroConfig config = ConfigManager.create(ZeroConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.withBindFile(configFile);
        config.save();

        // Load and verify
        ZeroConfig loaded = ConfigManager.create(ZeroConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer());
        loaded.withBindFile(configFile);
        loaded.load();

        assertThat(loaded.getZeroInt()).isEqualTo(0);
        assertThat(loaded.getZeroLong()).isEqualTo(0L);
        assertThat(loaded.getZeroDouble()).isEqualTo(0.0);
        assertThat(loaded.getZeroFloat()).isEqualTo(0.0f);
    }

    /**
     * Boolean edge cases
     */
    @Test
    void testEdgeCase_BooleanValues_HandledCorrectly() throws Exception {
        File configFile = this.tempDir.resolve("booleans.yml").toFile();

        BooleanConfig config = ConfigManager.create(BooleanConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.withBindFile(configFile);
        config.save();

        // Load and verify
        BooleanConfig loaded = ConfigManager.create(BooleanConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer());
        loaded.withBindFile(configFile);
        loaded.load();

        assertThat(loaded.isPrimitiveTrue()).isTrue();
        assertThat(loaded.isPrimitiveFalse()).isFalse();
        assertThat(loaded.getWrapperTrue()).isTrue();
        assertThat(loaded.getWrapperFalse()).isFalse();
        assertThat(loaded.getWrapperNull()).isNull();
    }
}
