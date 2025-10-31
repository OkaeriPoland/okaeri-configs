package eu.okaeri.configs.serdestest;

import eu.okaeri.commons.range.IntRange;
import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.serdes.okaeri.SerdesOkaeri;
import eu.okaeri.configs.serdes.okaeri.range.RangeFormat;
import eu.okaeri.configs.serdes.okaeri.range.RangeSpec;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for class-level annotation support with SerdesContextAttachment pattern.
 * Verifies field-level precedence and multiple levels of nesting.
 */
public class ClassLevelAnnotationTest {

    @TempDir
    Path tempDir;

    /**
     * Test class-level annotation applies to all fields without field-level annotation
     */
    @Test
    public void test_classLevel_appliesTo_allFields() throws IOException {
        File configFile = this.tempDir.resolve("class-level-test.yml").toFile();

        AllInlineConfig config = ConfigManager.create(AllInlineConfig.class, it -> {
            it.withConfigurer(new YamlSnakeYamlConfigurer(), new SerdesOkaeri());
            it.withBindFile(configFile);
            it.saveDefaults();
            it.load(true);
        });

        // Verify all ranges use INLINE format from class level
        String yaml = Files.readString(configFile.toPath());
        assertTrue(yaml.contains("range1: 10-20"), "range1 should use INLINE format");
        assertTrue(yaml.contains("range2: 30-40"), "range2 should use INLINE format");
        assertTrue(yaml.contains("range3: 50-60"), "range3 should use INLINE format");
        assertFalse(yaml.contains("min:"), "Should not contain SECTION format");
        assertFalse(yaml.contains("max:"), "Should not contain SECTION format");
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @RangeSpec(format = RangeFormat.INLINE)
    public static class AllInlineConfig extends OkaeriConfig {
        private IntRange range1 = IntRange.of(10, 20);
        private IntRange range2 = IntRange.of(30, 40);
        private IntRange range3 = IntRange.of(50, 60);
    }

    /**
     * Test field-level annotation takes precedence over class-level
     */
    @Test
    public void test_fieldLevel_overrides_classLevel() throws IOException {
        File configFile = this.tempDir.resolve("field-override-test.yml").toFile();

        FieldOverrideConfig config = ConfigManager.create(FieldOverrideConfig.class, it -> {
            it.withConfigurer(new YamlSnakeYamlConfigurer(), new SerdesOkaeri());
            it.withBindFile(configFile);
            it.saveDefaults();
            it.load(true);
        });

        // Verify field-level override works
        String yaml = Files.readString(configFile.toPath());
        assertTrue(yaml.contains("range1: 10-20"), "range1 should use INLINE from class");
        assertTrue(yaml.contains("range3: 50-60"), "range3 should use INLINE from class");

        // range2 should use SECTION format (field-level override)
        assertTrue(yaml.contains("range2:"), "range2 should be in SECTION format");
        assertTrue(yaml.contains("  min: 30"), "range2 should have min");
        assertTrue(yaml.contains("  max: 40"), "range2 should have max");
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @RangeSpec(format = RangeFormat.INLINE)
    public static class FieldOverrideConfig extends OkaeriConfig {
        private IntRange range1 = IntRange.of(10, 20);

        @RangeSpec(format = RangeFormat.SECTION)
        private IntRange range2 = IntRange.of(30, 40);

        private IntRange range3 = IntRange.of(50, 60);
    }

    /**
     * Test single level of nesting - nested config inherits nothing from parent
     */
    @Test
    public void test_singleLevel_nesting() throws IOException {
        File configFile = this.tempDir.resolve("single-nesting-test.yml").toFile();

        ParentConfig config = ConfigManager.create(ParentConfig.class, it -> {
            it.withConfigurer(new YamlSnakeYamlConfigurer(), new SerdesOkaeri());
            it.withBindFile(configFile);
            it.saveDefaults();
            it.load(true);
        });

        // Verify each config uses its own class-level annotation
        String yaml = Files.readString(configFile.toPath());
        assertTrue(yaml.contains("parentRange: 1-10"), "parent should use INLINE format");

        // Nested should use SECTION format
        assertTrue(yaml.contains("nested:"), "nested config section should exist");
        assertTrue(yaml.contains("  nestedRange:"), "nestedRange should be in SECTION format");
        assertTrue(yaml.contains("    min: 20"), "nestedRange should have min");
        assertTrue(yaml.contains("    max: 30"), "nestedRange should have max");
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @RangeSpec(format = RangeFormat.INLINE)
    public static class ParentConfig extends OkaeriConfig {
        private IntRange parentRange = IntRange.of(1, 10);
        private NestedConfig nested = new NestedConfig();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @RangeSpec(format = RangeFormat.SECTION)
    public static class NestedConfig extends OkaeriConfig {
        private IntRange nestedRange = IntRange.of(20, 30);
    }

    /**
     * Test multiple levels of nesting (3 levels deep)
     */
    @Test
    public void test_multipleLevel_nesting() throws IOException {
        File configFile = this.tempDir.resolve("multi-level-nesting-test.yml").toFile();

        Level1Config config = ConfigManager.create(Level1Config.class, it -> {
            it.withConfigurer(new YamlSnakeYamlConfigurer(), new SerdesOkaeri());
            it.withBindFile(configFile);
            it.saveDefaults();
            it.load(true);
        });

        // Verify all levels use their respective formats
        String yaml = Files.readString(configFile.toPath());

        // Level 1: INLINE
        assertTrue(yaml.contains("level1Range: 1-10"), "level1 should use INLINE format");

        // Level 2: SECTION
        assertTrue(yaml.contains("level2:"), "level2 config section should exist");
        assertTrue(yaml.contains("  level2Range:"), "level2Range should be in SECTION format");
        assertTrue(yaml.contains("    min: 20"), "level2Range should have min");
        assertTrue(yaml.contains("    max: 30"), "level2Range should have max");

        // Level 3: INLINE for level3Range1, SECTION for level3Range2 (field override)
        assertTrue(yaml.contains("  level3:"), "level3 config section should exist");
        assertTrue(yaml.contains("    level3Range1: 40-50"), "level3Range1 should use INLINE from class");

        assertTrue(yaml.contains("    level3Range2:"), "level3Range2 should be in SECTION format");
        assertTrue(yaml.contains("      min: 60"), "level3Range2 should have min");
        assertTrue(yaml.contains("      max: 70"), "level3Range2 should have max");
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @RangeSpec(format = RangeFormat.INLINE)
    public static class Level1Config extends OkaeriConfig {
        private IntRange level1Range = IntRange.of(1, 10);
        private Level2Config level2 = new Level2Config();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @RangeSpec(format = RangeFormat.SECTION)
    public static class Level2Config extends OkaeriConfig {
        private IntRange level2Range = IntRange.of(20, 30);
        private Level3Config level3 = new Level3Config();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @RangeSpec(format = RangeFormat.INLINE)
    public static class Level3Config extends OkaeriConfig {
        private IntRange level3Range1 = IntRange.of(40, 50);

        @RangeSpec(format = RangeFormat.SECTION)
        private IntRange level3Range2 = IntRange.of(60, 70);
    }

    /**
     * Test roundtrip serialization with class-level annotations
     */
    @Test
    public void test_roundtrip_with_classLevel() throws IOException {
        File configFile = this.tempDir.resolve("roundtrip-test.yml").toFile();

        // Create and save
        RoundtripConfig config = ConfigManager.create(RoundtripConfig.class, it -> {
            it.withConfigurer(new YamlSnakeYamlConfigurer(), new SerdesOkaeri());
            it.withBindFile(configFile);
            it.saveDefaults();
            it.load(true);
        });

        // Verify values are preserved
        assertEquals(IntRange.of(100, 200), config.getRange1(), "range1 should be loaded correctly");
        assertEquals(IntRange.of(300, 400), config.getRange2(), "range2 should be loaded correctly");

        // Verify format is preserved
        String yaml = Files.readString(configFile.toPath());
        assertTrue(yaml.contains("range1: 100-200"), "range1 should use INLINE format");
        assertTrue(yaml.contains("range2: 300-400"), "range2 should use INLINE format");
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @RangeSpec(format = RangeFormat.INLINE)
    public static class RoundtripConfig extends OkaeriConfig {
        private IntRange range1 = IntRange.of(100, 200);
        private IntRange range2 = IntRange.of(300, 400);
    }

    /**
     * Test no class-level annotation falls back to default (SECTION)
     */
    @Test
    public void test_noClassLevel_usesDefault() throws IOException {
        File configFile = this.tempDir.resolve("no-class-level-test.yml").toFile();

        NoClassLevelConfig config = ConfigManager.create(NoClassLevelConfig.class, it -> {
            it.withConfigurer(new YamlSnakeYamlConfigurer(), new SerdesOkaeri());
            it.withBindFile(configFile);
            it.saveDefaults();
            it.load(true);
        });

        // Verify default SECTION format is used
        String yaml = Files.readString(configFile.toPath());
        assertTrue(yaml.contains("range1:"), "range1 should be in SECTION format");
        assertTrue(yaml.contains("  min: 10"), "range1 should have min");
        assertTrue(yaml.contains("  max: 20"), "range1 should have max");
        assertTrue(yaml.contains("range2:"), "range2 should be in SECTION format");
        assertTrue(yaml.contains("  min: 30"), "range2 should have min");
        assertTrue(yaml.contains("  max: 40"), "range2 should have max");
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class NoClassLevelConfig extends OkaeriConfig {
        private IntRange range1 = IntRange.of(10, 20);
        private IntRange range2 = IntRange.of(30, 40);
    }

    /**
     * Test deep nesting (4 levels) with alternating formats
     */
    @Test
    public void test_deepNesting_alternatingFormats() throws IOException {
        File configFile = this.tempDir.resolve("deep-nesting-test.yml").toFile();

        L1 config = ConfigManager.create(L1.class, it -> {
            it.withConfigurer(new YamlSnakeYamlConfigurer(), new SerdesOkaeri());
            it.withBindFile(configFile);
            it.saveDefaults();
            it.load(true);
        });

        // Verify alternating formats
        String yaml = Files.readString(configFile.toPath());

        // L1: INLINE
        assertTrue(yaml.contains("r1: 1-2"), "L1 should use INLINE");

        // L2: SECTION
        assertTrue(yaml.contains("  r2:"), "L2 should use SECTION");
        assertTrue(yaml.contains("    min: 3"), "L2 should have min");

        // L3: INLINE
        assertTrue(yaml.contains("    r3: 5-6"), "L3 should use INLINE");

        // L4: SECTION
        assertTrue(yaml.contains("      r4:"), "L4 should use SECTION");
        assertTrue(yaml.contains("        min: 7"), "L4 should have min");
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @RangeSpec(format = RangeFormat.INLINE)
    public static class L1 extends OkaeriConfig {
        private IntRange r1 = IntRange.of(1, 2);
        private L2 l2 = new L2();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @RangeSpec(format = RangeFormat.SECTION)
    public static class L2 extends OkaeriConfig {
        private IntRange r2 = IntRange.of(3, 4);
        private L3 l3 = new L3();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @RangeSpec(format = RangeFormat.INLINE)
    public static class L3 extends OkaeriConfig {
        private IntRange r3 = IntRange.of(5, 6);
        private L4 l4 = new L4();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @RangeSpec(format = RangeFormat.SECTION)
    public static class L4 extends OkaeriConfig {
        private IntRange r4 = IntRange.of(7, 8);
    }
}
