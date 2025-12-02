package eu.okaeri.configs.serdes.commons.duration;

import eu.okaeri.configs.serdes.SerdesContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DurationTransformerTest {

    private DurationTransformer transformer;
    private SerdesContext context;

    @BeforeEach
    void setUp() {
        this.transformer = new DurationTransformer();
        this.context = mock(SerdesContext.class);
        when(this.context.getAttachment(DurationSpecData.class)).thenReturn(Optional.empty());
    }

    @Test
    void testGetPair() {
        var pair = this.transformer.getPair();
        assertThat(pair.getFrom().getType()).isEqualTo(String.class);
        assertThat(pair.getTo().getType()).isEqualTo(Duration.class);
    }

    // ==================== leftToRight (String → Duration) ====================

    @Nested
    class LeftToRight {

        @ParameterizedTest(name = "JBOD simple: {0}")
        @CsvSource({
            "7d, P7D",
            "1h, PT1H",
            "30m, PT30M",
            "5s, PT5S",
            "100ms, PT0.1S",
            "1000ns, PT0.000001S"
        })
        void testJbodSimpleFormats(String input, String expectedIso) {
            Duration result = DurationTransformerTest.this.transformer.leftToRight(input, DurationTransformerTest.this.context);
            assertThat(result).isEqualTo(Duration.parse(expectedIso));
        }

        @ParameterizedTest(name = "JBOD uppercase: {0}")
        @CsvSource({
            "7D, P7D",
            "1H, PT1H",
            "30M, PT30M",
            "5S, PT5S",
            "100MS, PT0.1S"
        })
        void testJbodUppercaseFormats(String input, String expectedIso) {
            Duration result = DurationTransformerTest.this.transformer.leftToRight(input, DurationTransformerTest.this.context);
            assertThat(result).isEqualTo(Duration.parse(expectedIso));
        }

        @ParameterizedTest(name = "JBOD compound: {0}")
        @CsvSource({
            "14d12h, P14DT12H",
            "1h30m, PT1H30M",
            "1h30m30s, PT1H30M30S",
            "14d 12h, P14DT12H",
            "1h 30m 30s, PT1H30M30S"
        })
        void testJbodCompoundFormats(String input, String expectedIso) {
            Duration result = DurationTransformerTest.this.transformer.leftToRight(input, DurationTransformerTest.this.context);
            assertThat(result).isEqualTo(Duration.parse(expectedIso));
        }

        @Test
        void testJbodWithDuplicateUnits() {
            // 14d1d = 15 days
            Duration result = DurationTransformerTest.this.transformer.leftToRight("14d1d", DurationTransformerTest.this.context);
            assertThat(result).isEqualTo(Duration.ofDays(15));
        }

        @Test
        void testJbodNegativeValues() {
            Duration result = DurationTransformerTest.this.transformer.leftToRight("-5s", DurationTransformerTest.this.context);
            assertThat(result).isEqualTo(Duration.ofSeconds(-5));
        }

        @Test
        void testPlainNumber_DefaultUnitSeconds() {
            Duration result = DurationTransformerTest.this.transformer.leftToRight("60", DurationTransformerTest.this.context);
            assertThat(result).isEqualTo(Duration.ofSeconds(60));
        }

        @Test
        void testPlainNumber_CustomFallbackUnit() {
            when(DurationTransformerTest.this.context.getAttachment(DurationSpecData.class))
                .thenReturn(Optional.of(DurationSpecData.of(ChronoUnit.MINUTES, DurationFormat.SIMPLIFIED)));

            Duration result = DurationTransformerTest.this.transformer.leftToRight("30", DurationTransformerTest.this.context);
            assertThat(result).isEqualTo(Duration.ofMinutes(30));
        }

        @Test
        void testPlainNumber_NegativeValue() {
            Duration result = DurationTransformerTest.this.transformer.leftToRight("-30", DurationTransformerTest.this.context);
            assertThat(result).isEqualTo(Duration.ofSeconds(-30));
        }

        @ParameterizedTest(name = "ISO format: {0}")
        @ValueSource(strings = {"PT1H", "PT30M", "PT5S", "P1D", "PT1H30M", "P1DT12H"})
        void testIsoFormats(String input) {
            Duration result = DurationTransformerTest.this.transformer.leftToRight(input, DurationTransformerTest.this.context);
            assertThat(result).isEqualTo(Duration.parse(input));
        }

        @Test
        void testIsoDuration_SubSecond() {
            Duration result = DurationTransformerTest.this.transformer.leftToRight("PT0.5S", DurationTransformerTest.this.context);
            assertThat(result).isEqualTo(Duration.ofMillis(500));
        }

        @ParameterizedTest(name = "Invalid JBOD format: {0}")
        @ValueSource(strings = {"1y", "14d huh 6h ???", "abc", ""})
        void testInvalidJbodFormats(String input) {
            assertThatThrownBy(() -> DurationTransformerTest.this.transformer.leftToRight(input, DurationTransformerTest.this.context))
                .isInstanceOf(Exception.class);
        }
    }

    // ==================== rightToLeft (Duration → String) ====================

    @Nested
    class RightToLeft {

        @Test
        void testIsoFormat() {
            when(DurationTransformerTest.this.context.getAttachment(DurationSpecData.class))
                .thenReturn(Optional.of(DurationSpecData.of(ChronoUnit.SECONDS, DurationFormat.ISO)));

            String result = DurationTransformerTest.this.transformer.rightToLeft(Duration.ofHours(1), DurationTransformerTest.this.context);
            assertThat(result).isEqualTo("PT1H");
        }

        @Test
        void testSimplifiedFormat_Zero() {
            String result = DurationTransformerTest.this.transformer.rightToLeft(Duration.ZERO, DurationTransformerTest.this.context);
            assertThat(result).isEqualTo("0");
        }

        @ParameterizedTest(name = "Simplified format: {0} → {1}")
        @CsvSource({
            "PT1H, 1h",
            "PT30M, 30m",
            "PT5S, 5s"
        })
        void testSimplifiedFormat_SimpleValues(String isoInput, String expectedOutput) {
            Duration duration = Duration.parse(isoInput);
            String result = DurationTransformerTest.this.transformer.rightToLeft(duration, DurationTransformerTest.this.context);
            assertThat(result).isEqualTo(expectedOutput);
        }

        @Test
        void testSimplifiedFormat_Days() {
            // 48 hours = 2 days
            Duration duration = Duration.ofHours(48);
            String result = DurationTransformerTest.this.transformer.rightToLeft(duration, DurationTransformerTest.this.context);
            assertThat(result).isEqualTo("2d");
        }

        @Test
        void testSimplifiedFormat_Days_FallbackHours() {
            // When fallbackUnit is HOURS, don't convert to days
            when(DurationTransformerTest.this.context.getAttachment(DurationSpecData.class))
                .thenReturn(Optional.of(DurationSpecData.of(ChronoUnit.HOURS, DurationFormat.SIMPLIFIED)));

            Duration duration = Duration.ofHours(48);
            String result = DurationTransformerTest.this.transformer.rightToLeft(duration, DurationTransformerTest.this.context);
            assertThat(result).isEqualTo("48h");
        }

        @Test
        void testSimplifiedFormat_Negative() {
            Duration duration = Duration.ofSeconds(-30);
            String result = DurationTransformerTest.this.transformer.rightToLeft(duration, DurationTransformerTest.this.context);
            assertThat(result).isEqualTo("-30s");
        }

        @Test
        void testSimplifiedFormat_NegativeDays() {
            Duration duration = Duration.ofHours(-48);
            String result = DurationTransformerTest.this.transformer.rightToLeft(duration, DurationTransformerTest.this.context);
            assertThat(result).isEqualTo("-2d");
        }

        @Test
        void testSimplifiedFormat_Milliseconds() {
            Duration duration = Duration.ofMillis(500);
            String result = DurationTransformerTest.this.transformer.rightToLeft(duration, DurationTransformerTest.this.context);
            assertThat(result).isEqualTo("500ms");
        }

        @Test
        void testSimplifiedFormat_Nanoseconds() {
            Duration duration = Duration.ofNanos(100);
            String result = DurationTransformerTest.this.transformer.rightToLeft(duration, DurationTransformerTest.this.context);
            assertThat(result).isEqualTo("100ns");
        }

        @Test
        void testSimplifiedFormat_NegativeMilliseconds() {
            Duration duration = Duration.ofMillis(-500);
            String result = DurationTransformerTest.this.transformer.rightToLeft(duration, DurationTransformerTest.this.context);
            assertThat(result).isEqualTo("-500ms");
        }

        @Test
        void testSimplifiedFormat_ComplexDuration() {
            // PT1H30M - not simple, falls back
            Duration duration = Duration.parse("PT1H30M");
            String result = DurationTransformerTest.this.transformer.rightToLeft(duration, DurationTransformerTest.this.context);
            // Should produce simplified lowercase output
            assertThat(result).isEqualTo("1h30m");
        }
    }

    // ==================== Round-trip Tests ====================

    @Nested
    class RoundTrip {

        @ParameterizedTest(name = "Round-trip: {0}")
        @ValueSource(strings = {"7d", "1h", "30m", "5s", "100ms"})
        void testRoundTrip_SimpleFormats(String input) {
            Duration duration = DurationTransformerTest.this.transformer.leftToRight(input, DurationTransformerTest.this.context);
            String output = DurationTransformerTest.this.transformer.rightToLeft(duration, DurationTransformerTest.this.context);
            Duration reparsed = DurationTransformerTest.this.transformer.leftToRight(output, DurationTransformerTest.this.context);
            assertThat(reparsed).isEqualTo(duration);
        }

        @Test
        void testRoundTrip_Zero() {
            Duration duration = DurationTransformerTest.this.transformer.leftToRight("0", DurationTransformerTest.this.context);
            assertThat(duration).isEqualTo(Duration.ZERO);

            String output = DurationTransformerTest.this.transformer.rightToLeft(Duration.ZERO, DurationTransformerTest.this.context);
            assertThat(output).isEqualTo("0");
        }
    }
}
