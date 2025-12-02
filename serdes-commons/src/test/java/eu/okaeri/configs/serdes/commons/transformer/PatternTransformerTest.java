package eu.okaeri.configs.serdes.commons.transformer;

import eu.okaeri.configs.exception.ValueIndexedException;
import eu.okaeri.configs.serdes.SerdesContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class PatternTransformerTest {

    private PatternTransformer transformer;
    private SerdesContext context;

    @BeforeEach
    void setUp() {
        this.transformer = new PatternTransformer();
        this.context = mock(SerdesContext.class);
    }

    @Test
    void testGetPair() {
        var pair = this.transformer.getPair();
        assertThat(pair.getFrom().getType()).isEqualTo(String.class);
        assertThat(pair.getTo().getType()).isEqualTo(Pattern.class);
    }

    // ==================== leftToRight (String → Pattern) ====================

    @ParameterizedTest(name = "Valid pattern: {0}")
    @ValueSource(strings = {
        ".*",
        "\\d+",
        "[a-zA-Z]+",
        "^hello$",
        "test\\s+pattern",
        "(group1)(group2)",
        "(?i)case-insensitive"
    })
    void testCompileValidPatterns(String input) {
        Pattern result = this.transformer.leftToRight(input, this.context);
        assertThat(result.pattern()).isEqualTo(input);
    }

    @Test
    void testCompileEmptyPattern() {
        Pattern result = this.transformer.leftToRight("", this.context);
        assertThat(result.pattern()).isEmpty();
    }

    @Test
    void testInvalidPattern_ThrowsValueIndexedException() {
        assertThatThrownBy(() -> this.transformer.leftToRight("[", this.context))
            .isInstanceOf(ValueIndexedException.class);
    }

    @Test
    void testInvalidPattern_ContainsIndex() {
        try {
            this.transformer.leftToRight("[unclosed", this.context);
        } catch (ValueIndexedException e) {
            // The error index should be present
            assertThat(e.getStartIndex()).isGreaterThanOrEqualTo(0);
            assertThat(e.getMessage()).isNotEmpty();
        }
    }

    @Test
    void testInvalidPattern_UnbalancedParenthesis() {
        assertThatThrownBy(() -> this.transformer.leftToRight("(unclosed", this.context))
            .isInstanceOf(ValueIndexedException.class);
    }

    @Test
    void testInvalidPattern_InvalidEscape() {
        assertThatThrownBy(() -> this.transformer.leftToRight("\\", this.context))
            .isInstanceOf(ValueIndexedException.class);
    }

    @Test
    void testInvalidPattern_InvalidQuantifier() {
        assertThatThrownBy(() -> this.transformer.leftToRight("a{invalid}", this.context))
            .isInstanceOf(ValueIndexedException.class);
    }

    // ==================== rightToLeft (Pattern → String) ====================

    @Test
    void testSerializePattern() {
        Pattern pattern = Pattern.compile("\\d+");
        String result = this.transformer.rightToLeft(pattern, this.context);
        assertThat(result).isEqualTo("\\d+");
    }

    @Test
    void testSerializeEmptyPattern() {
        Pattern pattern = Pattern.compile("");
        String result = this.transformer.rightToLeft(pattern, this.context);
        assertThat(result).isEmpty();
    }

    @Test
    void testSerializeComplexPattern() {
        Pattern pattern = Pattern.compile("(?i)^[a-z]+\\s*:\\s*(.+)$");
        String result = this.transformer.rightToLeft(pattern, this.context);
        assertThat(result).isEqualTo("(?i)^[a-z]+\\s*:\\s*(.+)$");
    }

    // ==================== Round-trip Tests ====================

    @ParameterizedTest(name = "Round-trip: {0}")
    @ValueSource(strings = {
        ".*",
        "\\d+",
        "[a-zA-Z]+",
        "^hello$",
        "(group1)(group2)"
    })
    void testRoundTrip(String input) {
        Pattern pattern = this.transformer.leftToRight(input, this.context);
        String output = this.transformer.rightToLeft(pattern, this.context);
        Pattern reparsed = this.transformer.leftToRight(output, this.context);
        assertThat(reparsed.pattern()).isEqualTo(pattern.pattern());
    }

    // ==================== Pattern Behavior Tests ====================

    @Test
    void testCompiledPatternMatchesProperly() {
        Pattern pattern = this.transformer.leftToRight("\\d{3}-\\d{4}", this.context);
        assertThat(pattern.matcher("123-4567").matches()).isTrue();
        assertThat(pattern.matcher("abc-defg").matches()).isFalse();
    }
}
