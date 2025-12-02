package eu.okaeri.configs.serdes.commons.transformer;

import eu.okaeri.configs.serdes.SerdesContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class LocaleTransformerTest {

    private LocaleTransformer transformer;
    private SerdesContext context;

    @BeforeEach
    void setUp() {
        this.transformer = new LocaleTransformer();
        this.context = mock(SerdesContext.class);
    }

    @Test
    void testGetPair() {
        var pair = this.transformer.getPair();
        assertThat(pair.getFrom().getType()).isEqualTo(String.class);
        assertThat(pair.getTo().getType()).isEqualTo(Locale.class);
    }

    // ==================== leftToRight (String → Locale) ====================

    @ParameterizedTest(name = "Parse locale: {0}")
    @CsvSource({
        "en, en",
        "en-US, en-US",
        "pl-PL, pl-PL",
        "de-DE, de-DE",
        "zh-CN, zh-CN"
    })
    void testParseValidLocale_Hyphen(String input, String expectedTag) {
        Locale result = this.transformer.leftToRight(input, this.context);
        assertThat(result.toLanguageTag()).isEqualTo(expectedTag);
    }

    @ParameterizedTest(name = "Parse locale with underscore: {0}")
    @CsvSource({
        "en_US, en-US",
        "pl_PL, pl-PL",
        "de_DE, de-DE"
    })
    void testParseValidLocale_Underscore(String input, String expectedTag) {
        Locale result = this.transformer.leftToRight(input, this.context);
        assertThat(result.toLanguageTag()).isEqualTo(expectedTag);
    }

    @Test
    void testParseLanguageOnly() {
        Locale result = this.transformer.leftToRight("en", this.context);
        assertThat(result.getLanguage()).isEqualTo("en");
        assertThat(result.getCountry()).isEmpty();
    }

    @Test
    void testParseLanguageWithCountry() {
        Locale result = this.transformer.leftToRight("en-US", this.context);
        assertThat(result.getLanguage()).isEqualTo("en");
        assertThat(result.getCountry()).isEqualTo("US");
    }

    @Test
    void testParseInvalidLocale_SpecialChars() {
        // Special characters produce empty language from Locale.forLanguageTag
        assertThatThrownBy(() -> this.transformer.leftToRight("!!!", this.context))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Expected locale");
    }

    @Test
    void testParseInvalidLocale_Numbers() {
        // Pure numbers produce empty language from Locale.forLanguageTag
        assertThatThrownBy(() -> this.transformer.leftToRight("123", this.context))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Expected locale");
    }

    @Test
    void testEmptyString_ReturnsRootLocale() {
        // Empty string maps to empty language which is Locale.ROOT
        Locale result = this.transformer.leftToRight("", this.context);
        assertThat(result).isEqualTo(Locale.ROOT);
    }

    // ==================== rightToLeft (Locale → String) ====================

    @Test
    void testSerializeLocale_LanguageOnly() {
        Locale locale = Locale.ENGLISH;
        String result = this.transformer.rightToLeft(locale, this.context);
        assertThat(result).isEqualTo("en");
    }

    @Test
    void testSerializeLocale_WithCountry() {
        Locale locale = Locale.US;
        String result = this.transformer.rightToLeft(locale, this.context);
        assertThat(result).isEqualTo("en-US");
    }

    @Test
    void testSerializeLocale_NormalizesUnderscore() {
        // Locale.toString() returns "en_US", but we normalize to "en-US"
        Locale locale = new Locale("en", "US");
        String result = this.transformer.rightToLeft(locale, this.context);
        assertThat(result).isEqualTo("en-US");
    }

    @Test
    void testSerializeLocale_Root() {
        String result = this.transformer.rightToLeft(Locale.ROOT, this.context);
        assertThat(result).isEmpty();
    }

    // ==================== Round-trip Tests ====================

    @ParameterizedTest(name = "Round-trip: {0}")
    @ValueSource(strings = {"en", "en-US", "pl-PL", "de-DE", "zh-CN"})
    void testRoundTrip(String input) {
        Locale locale = this.transformer.leftToRight(input, this.context);
        String output = this.transformer.rightToLeft(locale, this.context);
        Locale reparsed = this.transformer.leftToRight(output, this.context);
        assertThat(reparsed).isEqualTo(locale);
    }
}
