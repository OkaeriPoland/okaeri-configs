package eu.okaeri.configs.serdes.commons.transformer;

import eu.okaeri.configs.serdes.SerdesContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class InstantTransformerTest {

    private InstantTransformer transformer;
    private SerdesContext context;

    @BeforeEach
    void setUp() {
        this.transformer = new InstantTransformer();
        this.context = mock(SerdesContext.class);
    }

    @Test
    void testGetPair() {
        var pair = this.transformer.getPair();
        assertThat(pair.getFrom().getType()).isEqualTo(String.class);
        assertThat(pair.getTo().getType()).isEqualTo(Instant.class);
    }

    // ==================== leftToRight (String → Instant) ====================

    @ParameterizedTest(name = "Parse ISO-8601: {0}")
    @ValueSource(strings = {
        "2023-01-15T10:30:00Z",
        "2023-06-15T14:45:30.123Z",
        "1970-01-01T00:00:00Z",
        "2099-12-31T23:59:59.999Z"
    })
    void testParseIso8601(String input) {
        Instant result = this.transformer.leftToRight(input, this.context);
        assertThat(result).isEqualTo(Instant.parse(input));
    }

    @Test
    void testParseEpochInstant() {
        Instant result = this.transformer.leftToRight("1970-01-01T00:00:00Z", this.context);
        assertThat(result).isEqualTo(Instant.EPOCH);
    }

    @Test
    void testParseWithNanoseconds() {
        Instant result = this.transformer.leftToRight("2023-01-15T10:30:00.123456789Z", this.context);
        assertThat(result.getNano()).isEqualTo(123456789);
    }

    @ParameterizedTest(name = "Invalid format: {0}")
    @ValueSource(strings = {"invalid", "2023/01/15", "2023-01-15", ""})
    void testParseInvalidFormat(String input) {
        assertThatThrownBy(() -> this.transformer.leftToRight(input, this.context))
            .isInstanceOf(IllegalArgumentException.class);
    }

    // ==================== rightToLeft (Instant → String) ====================

    @Test
    void testSerializeInstant() {
        Instant instant = Instant.parse("2023-01-15T10:30:00Z");
        String result = this.transformer.rightToLeft(instant, this.context);
        assertThat(result).isEqualTo("2023-01-15T10:30:00Z");
    }

    @Test
    void testSerializeEpoch() {
        String result = this.transformer.rightToLeft(Instant.EPOCH, this.context);
        assertThat(result).isEqualTo("1970-01-01T00:00:00Z");
    }

    @Test
    void testSerializeWithNanoseconds() {
        Instant instant = Instant.parse("2023-01-15T10:30:00.123456789Z");
        String result = this.transformer.rightToLeft(instant, this.context);
        assertThat(result).isEqualTo("2023-01-15T10:30:00.123456789Z");
    }

    // ==================== Round-trip Tests ====================

    @ParameterizedTest(name = "Round-trip: {0}")
    @ValueSource(strings = {
        "2023-01-15T10:30:00Z",
        "2023-06-15T14:45:30.123Z",
        "1970-01-01T00:00:00Z"
    })
    void testRoundTrip(String input) {
        Instant instant = this.transformer.leftToRight(input, this.context);
        String output = this.transformer.rightToLeft(instant, this.context);
        Instant reparsed = this.transformer.leftToRight(output, this.context);
        assertThat(reparsed).isEqualTo(instant);
    }

    @Test
    void testRoundTrip_Now() {
        Instant now = Instant.now();
        String serialized = this.transformer.rightToLeft(now, this.context);
        Instant deserialized = this.transformer.leftToRight(serialized, this.context);
        assertThat(deserialized).isEqualTo(now);
    }
}
