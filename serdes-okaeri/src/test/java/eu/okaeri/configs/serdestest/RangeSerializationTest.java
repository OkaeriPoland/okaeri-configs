package eu.okaeri.configs.serdestest;

import eu.okaeri.commons.range.*;
import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Range serialization with SerdesContextAttachment pattern.
 * Verifies both INLINE and SECTION formats work correctly.
 */
class RangeSerializationTest {

    @TempDir
    Path tempDir;

    // === INLINE FORMAT TESTS ===

    @Test
    void testIntRange_InlineFormat_SerializesCorrectly() throws IOException {
        File configFile = this.tempDir.resolve("inline-int.yml").toFile();

        InlineRangeConfig config = ConfigManager.create(InlineRangeConfig.class, it -> {
            it.withConfigurer(new YamlSnakeYamlConfigurer(), new SerdesOkaeri());
            it.withBindFile(configFile);
            it.saveDefaults();
            it.load(true);
        });

        config.setIntRange(IntRange.of(10, 20));
        config.save();

        String content = Files.readString(configFile.toPath());
        assertThat(content).contains("intRange: 10-20");
    }

    @Test
    void testLongRange_InlineFormat_SerializesCorrectly() throws IOException {
        File configFile = this.tempDir.resolve("inline-long.yml").toFile();

        InlineRangeConfig config = ConfigManager.create(InlineRangeConfig.class, it -> {
            it.withConfigurer(new YamlSnakeYamlConfigurer(), new SerdesOkaeri());
            it.withBindFile(configFile);
            it.saveDefaults();
            it.load(true);
        });

        config.setLongRange(LongRange.of(100L, 500L));
        config.save();

        String content = Files.readString(configFile.toPath());
        assertThat(content).contains("longRange: 100-500");
    }

    @Test
    void testFloatRange_InlineFormat_SerializesCorrectly() throws IOException {
        File configFile = this.tempDir.resolve("inline-float.yml").toFile();

        InlineRangeConfig config = ConfigManager.create(InlineRangeConfig.class, it -> {
            it.withConfigurer(new YamlSnakeYamlConfigurer(), new SerdesOkaeri());
            it.withBindFile(configFile);
            it.saveDefaults();
            it.load(true);
        });

        config.setFloatRange(FloatRange.of(1.5f, 3.5f));
        config.save();

        String content = Files.readString(configFile.toPath());
        assertThat(content).contains("floatRange: 1.5-3.5");
    }

    @Test
    void testDoubleRange_InlineFormat_SerializesCorrectly() throws IOException {
        File configFile = this.tempDir.resolve("inline-double.yml").toFile();

        InlineRangeConfig config = ConfigManager.create(InlineRangeConfig.class, it -> {
            it.withConfigurer(new YamlSnakeYamlConfigurer(), new SerdesOkaeri());
            it.withBindFile(configFile);
            it.saveDefaults();
            it.load(true);
        });

        config.setDoubleRange(DoubleRange.of(10.25, 99.75));
        config.save();

        String content = Files.readString(configFile.toPath());
        assertThat(content).contains("doubleRange: 10.25-99.75");
    }

    @Test
    void testInlineFormat_DeserializesCorrectly() throws IOException {
        File configFile = this.tempDir.resolve("inline-deserialize.yml").toFile();

        // Write YAML manually
        Files.writeString(configFile.toPath(),
            "intRange: \"10-20\"\n" +
                "longRange: \"100-500\"\n" +
                "shortRange: \"5-15\"\n" +
                "byteRange: \"1-10\"\n" +
                "floatRange: \"1.5-3.5\"\n" +
                "doubleRange: \"10.25-99.75\"\n"
        );

        InlineRangeConfig config = ConfigManager.create(InlineRangeConfig.class, it -> {
            it.withConfigurer(new YamlSnakeYamlConfigurer(), new SerdesOkaeri());
            it.withBindFile(configFile);
            it.load(true);
        });

        assertThat(config.getIntRange()).isEqualTo(IntRange.of(10, 20));
        assertThat(config.getLongRange()).isEqualTo(LongRange.of(100L, 500L));
        assertThat(config.getShortRange()).isEqualTo(ShortRange.of((short) 5, (short) 15));
        assertThat(config.getByteRange()).isEqualTo(ByteRange.of((byte) 1, (byte) 10));
        assertThat(config.getFloatRange()).isEqualTo(FloatRange.of(1.5f, 3.5f));
        assertThat(config.getDoubleRange()).isEqualTo(DoubleRange.of(10.25, 99.75));
    }

    // === SECTION FORMAT TESTS ===

    @Test
    void testIntRange_SectionFormat_SerializesCorrectly() throws IOException {
        File configFile = this.tempDir.resolve("section-int.yml").toFile();

        SectionRangeConfig config = ConfigManager.create(SectionRangeConfig.class, it -> {
            it.withConfigurer(new YamlSnakeYamlConfigurer(), new SerdesOkaeri());
            it.withBindFile(configFile);
            it.saveDefaults();
            it.load(true);
        });

        config.setIntRange(IntRange.of(10, 20));
        config.save();

        String content = Files.readString(configFile.toPath());
        assertThat(content).contains("intRange:");
        assertThat(content).contains("min: 10");
        assertThat(content).contains("max: 20");
    }

    @Test
    void testLongRange_SectionFormat_SerializesCorrectly() throws IOException {
        File configFile = this.tempDir.resolve("section-long.yml").toFile();

        SectionRangeConfig config = ConfigManager.create(SectionRangeConfig.class, it -> {
            it.withConfigurer(new YamlSnakeYamlConfigurer(), new SerdesOkaeri());
            it.withBindFile(configFile);
            it.saveDefaults();
            it.load(true);
        });

        config.setLongRange(LongRange.of(100L, 500L));
        config.save();

        String content = Files.readString(configFile.toPath());
        assertThat(content).contains("longRange:");
        assertThat(content).contains("min: 100");
        assertThat(content).contains("max: 500");
    }

    @Test
    void testSectionFormat_DeserializesCorrectly() throws IOException {
        File configFile = this.tempDir.resolve("section-deserialize.yml").toFile();

        // Write YAML manually with section format
        Files.writeString(configFile.toPath(),
            "intRange:\n" +
                "  min: 10\n" +
                "  max: 20\n" +
                "longRange:\n" +
                "  min: 100\n" +
                "  max: 500\n" +
                "shortRange:\n" +
                "  min: 5\n" +
                "  max: 15\n" +
                "byteRange:\n" +
                "  min: 1\n" +
                "  max: 10\n" +
                "floatRange:\n" +
                "  min: 1.5\n" +
                "  max: 3.5\n" +
                "doubleRange:\n" +
                "  min: 10.25\n" +
                "  max: 99.75\n"
        );

        SectionRangeConfig config = ConfigManager.create(SectionRangeConfig.class, it -> {
            it.withConfigurer(new YamlSnakeYamlConfigurer(), new SerdesOkaeri());
            it.withBindFile(configFile);
            it.load(true);
        });

        assertThat(config.getIntRange()).isEqualTo(IntRange.of(10, 20));
        assertThat(config.getLongRange()).isEqualTo(LongRange.of(100L, 500L));
        assertThat(config.getShortRange()).isEqualTo(ShortRange.of((short) 5, (short) 15));
        assertThat(config.getByteRange()).isEqualTo(ByteRange.of((byte) 1, (byte) 10));
        assertThat(config.getFloatRange()).isEqualTo(FloatRange.of(1.5f, 3.5f));
        assertThat(config.getDoubleRange()).isEqualTo(DoubleRange.of(10.25, 99.75));
    }

    // === MIXED FORMAT TESTS ===

    @Test
    void testMixedFormats_InSameConfig() throws IOException {
        File configFile = this.tempDir.resolve("mixed.yml").toFile();

        MixedRangeConfig config = ConfigManager.create(MixedRangeConfig.class, it -> {
            it.withConfigurer(new YamlSnakeYamlConfigurer(), new SerdesOkaeri());
            it.withBindFile(configFile);
            it.saveDefaults();
            it.load(true);
        });

        config.setInlineRange(IntRange.of(10, 20));
        config.setSectionRange(IntRange.of(50, 100));
        config.save();

        String content = Files.readString(configFile.toPath());

        // Inline format
        assertThat(content).contains("inlineRange: 10-20");

        // Section format
        assertThat(content).contains("sectionRange:");
        assertThat(content).contains("min: 50");
        assertThat(content).contains("max: 100");
    }

    // === DEFAULT FORMAT TEST ===

    @Test
    void testDefaultFormat_IsSectionFormat() throws IOException {
        File configFile = this.tempDir.resolve("default.yml").toFile();

        DefaultRangeConfig config = ConfigManager.create(DefaultRangeConfig.class, it -> {
            it.withConfigurer(new YamlSnakeYamlConfigurer(), new SerdesOkaeri());
            it.withBindFile(configFile);
            it.saveDefaults();
            it.load(true);
        });

        config.setIntRange(IntRange.of(10, 20));
        config.save();

        String content = Files.readString(configFile.toPath());

        // Should use SECTION format by default
        assertThat(content).contains("intRange:");
        assertThat(content).contains("min: 10");
        assertThat(content).contains("max: 20");
    }

    // === ROUND-TRIP TESTS ===

    @Test
    void testRoundTrip_InlineFormat() throws IOException {
        File configFile = this.tempDir.resolve("roundtrip-inline.yml").toFile();

        InlineRangeConfig config1 = ConfigManager.create(InlineRangeConfig.class, it -> {
            it.withConfigurer(new YamlSnakeYamlConfigurer(), new SerdesOkaeri());
            it.withBindFile(configFile);
            it.saveDefaults();
            it.load(true);
        });

        config1.setIntRange(IntRange.of(10, 20));
        config1.setLongRange(LongRange.of(100L, 500L));
        config1.save();

        InlineRangeConfig config2 = ConfigManager.create(InlineRangeConfig.class, it -> {
            it.withConfigurer(new YamlSnakeYamlConfigurer(), new SerdesOkaeri());
            it.withBindFile(configFile);
            it.load(true);
        });

        assertThat(config2.getIntRange()).isEqualTo(config1.getIntRange());
        assertThat(config2.getLongRange()).isEqualTo(config1.getLongRange());
    }

    @Test
    void testRoundTrip_SectionFormat() throws IOException {
        File configFile = this.tempDir.resolve("roundtrip-section.yml").toFile();

        SectionRangeConfig config1 = ConfigManager.create(SectionRangeConfig.class, it -> {
            it.withConfigurer(new YamlSnakeYamlConfigurer(), new SerdesOkaeri());
            it.withBindFile(configFile);
            it.saveDefaults();
            it.load(true);
        });

        config1.setIntRange(IntRange.of(10, 20));
        config1.setLongRange(LongRange.of(100L, 500L));
        config1.save();

        SectionRangeConfig config2 = ConfigManager.create(SectionRangeConfig.class, it -> {
            it.withConfigurer(new YamlSnakeYamlConfigurer(), new SerdesOkaeri());
            it.withBindFile(configFile);
            it.load(true);
        });

        assertThat(config2.getIntRange()).isEqualTo(config1.getIntRange());
        assertThat(config2.getLongRange()).isEqualTo(config1.getLongRange());
    }

    // === TEST CONFIGS ===

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class InlineRangeConfig extends OkaeriConfig {

        @Comment("Integer range (inline format)")
        @RangeSpec(format = RangeFormat.INLINE)
        private IntRange intRange = IntRange.of(1, 10);

        @Comment("Long range (inline format)")
        @RangeSpec(format = RangeFormat.INLINE)
        private LongRange longRange = LongRange.of(1L, 100L);

        @Comment("Short range (inline format)")
        @RangeSpec(format = RangeFormat.INLINE)
        private ShortRange shortRange = ShortRange.of((short) 1, (short) 10);

        @Comment("Byte range (inline format)")
        @RangeSpec(format = RangeFormat.INLINE)
        private ByteRange byteRange = ByteRange.of((byte) 1, (byte) 10);

        @Comment("Float range (inline format)")
        @RangeSpec(format = RangeFormat.INLINE)
        private FloatRange floatRange = FloatRange.of(0.0f, 1.0f);

        @Comment("Double range (inline format)")
        @RangeSpec(format = RangeFormat.INLINE)
        private DoubleRange doubleRange = DoubleRange.of(0.0, 1.0);
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class SectionRangeConfig extends OkaeriConfig {

        @Comment("Integer range (section format)")
        @RangeSpec(format = RangeFormat.SECTION)
        private IntRange intRange = IntRange.of(1, 10);

        @Comment("Long range (section format)")
        @RangeSpec(format = RangeFormat.SECTION)
        private LongRange longRange = LongRange.of(1L, 100L);

        @Comment("Short range (section format)")
        @RangeSpec(format = RangeFormat.SECTION)
        private ShortRange shortRange = ShortRange.of((short) 1, (short) 10);

        @Comment("Byte range (section format)")
        @RangeSpec(format = RangeFormat.SECTION)
        private ByteRange byteRange = ByteRange.of((byte) 1, (byte) 10);

        @Comment("Float range (section format)")
        @RangeSpec(format = RangeFormat.SECTION)
        private FloatRange floatRange = FloatRange.of(0.0f, 1.0f);

        @Comment("Double range (section format)")
        @RangeSpec(format = RangeFormat.SECTION)
        private DoubleRange doubleRange = DoubleRange.of(0.0, 1.0);
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class MixedRangeConfig extends OkaeriConfig {

        @Comment("Inline format range")
        @RangeSpec(format = RangeFormat.INLINE)
        private IntRange inlineRange = IntRange.of(1, 10);

        @Comment("Section format range")
        @RangeSpec(format = RangeFormat.SECTION)
        private IntRange sectionRange = IntRange.of(1, 100);
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class DefaultRangeConfig extends OkaeriConfig {

        @Comment("Range with default format (should be SECTION)")
        private IntRange intRange = IntRange.of(1, 10);
    }
}
