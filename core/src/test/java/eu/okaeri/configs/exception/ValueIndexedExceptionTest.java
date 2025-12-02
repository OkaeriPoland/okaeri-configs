package eu.okaeri.configs.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ValueIndexedExceptionTest {

    // ==================== Constructor Tests ====================

    @Test
    void testSingleIndexConstructor() {
        ValueIndexedException exception = new ValueIndexedException("error message", 5);

        assertThat(exception.getMessage()).isEqualTo("error message");
        assertThat(exception.getStartIndex()).isEqualTo(5);
        assertThat(exception.getLength()).isEqualTo(1);
        assertThat(exception.getContextLinesBefore()).isEqualTo(ValueIndexedException.DEFAULT_CONTEXT_BEFORE);
        assertThat(exception.getContextLinesAfter()).isEqualTo(ValueIndexedException.DEFAULT_CONTEXT_AFTER);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void testSingleIndexWithCauseConstructor() {
        Exception cause = new RuntimeException("root cause");
        ValueIndexedException exception = new ValueIndexedException("error message", 5, cause);

        assertThat(exception.getMessage()).isEqualTo("error message");
        assertThat(exception.getStartIndex()).isEqualTo(5);
        assertThat(exception.getLength()).isEqualTo(1);
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void testRangeConstructor() {
        ValueIndexedException exception = new ValueIndexedException("error message", 5, 10);

        assertThat(exception.getMessage()).isEqualTo("error message");
        assertThat(exception.getStartIndex()).isEqualTo(5);
        assertThat(exception.getLength()).isEqualTo(10);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void testRangeWithCauseConstructor() {
        Exception cause = new RuntimeException("root cause");
        ValueIndexedException exception = new ValueIndexedException("error message", 5, 10, cause);

        assertThat(exception.getMessage()).isEqualTo("error message");
        assertThat(exception.getStartIndex()).isEqualTo(5);
        assertThat(exception.getLength()).isEqualTo(10);
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void testLengthNormalization() {
        // Length of 0 should be normalized to 1
        ValueIndexedException exception = new ValueIndexedException("error message", 5, 0);
        assertThat(exception.getLength()).isEqualTo(1);

        // Negative length should be normalized to 1
        ValueIndexedException exception2 = new ValueIndexedException("error message", 5, -5);
        assertThat(exception2.getLength()).isEqualTo(1);
    }

    // ==================== Builder Tests ====================

    @Test
    void testBuilderWithAllFields() {
        Exception cause = new RuntimeException("root cause");

        ValueIndexedException exception = ValueIndexedException.builder()
            .message("error message")
            .startIndex(10)
            .length(5)
            .contextLinesBefore(2)
            .contextLinesAfter(3)
            .cause(cause)
            .build();

        assertThat(exception.getMessage()).isEqualTo("error message");
        assertThat(exception.getStartIndex()).isEqualTo(10);
        assertThat(exception.getLength()).isEqualTo(5);
        assertThat(exception.getContextLinesBefore()).isEqualTo(2);
        assertThat(exception.getContextLinesAfter()).isEqualTo(3);
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void testBuilderWithContextLinesOnly() {
        ValueIndexedException exception = ValueIndexedException.builder()
            .message("error at position")
            .startIndex(0)
            .contextLinesBefore(5)
            .contextLinesAfter(5)
            .build();

        assertThat(exception.getContextLinesBefore()).isEqualTo(5);
        assertThat(exception.getContextLinesAfter()).isEqualTo(5);
        assertThat(exception.getLength()).isEqualTo(1); // Default when not specified
    }

    @Test
    void testBuilderNormalizesNegativeContextLines() {
        ValueIndexedException exception = ValueIndexedException.builder()
            .message("error")
            .startIndex(0)
            .contextLinesBefore(-1)
            .contextLinesAfter(-5)
            .build();

        // Negative values should be normalized to defaults
        assertThat(exception.getContextLinesBefore()).isEqualTo(ValueIndexedException.DEFAULT_CONTEXT_BEFORE);
        assertThat(exception.getContextLinesAfter()).isEqualTo(ValueIndexedException.DEFAULT_CONTEXT_AFTER);
    }

    @Test
    void testBuilderNormalizesZeroLength() {
        ValueIndexedException exception = ValueIndexedException.builder()
            .message("error")
            .startIndex(0)
            .length(0)
            .build();

        assertThat(exception.getLength()).isEqualTo(1);
    }

    @Test
    void testBuilderMinimal() {
        ValueIndexedException exception = ValueIndexedException.builder()
            .message("minimal error")
            .build();

        assertThat(exception.getMessage()).isEqualTo("minimal error");
        assertThat(exception.getStartIndex()).isEqualTo(0);
        assertThat(exception.getLength()).isEqualTo(1);
        assertThat(exception.getContextLinesBefore()).isEqualTo(ValueIndexedException.DEFAULT_CONTEXT_BEFORE);
        assertThat(exception.getContextLinesAfter()).isEqualTo(ValueIndexedException.DEFAULT_CONTEXT_AFTER);
    }

    // ==================== Use Case Tests ====================

    @Test
    void testTypicalPatternErrorUseCase() {
        // Simulating a regex pattern syntax error
        ValueIndexedException exception = ValueIndexedException.builder()
            .message("Unclosed group")
            .startIndex(5)
            .length(1)
            .build();

        assertThat(exception.getStartIndex()).isEqualTo(5);
        assertThat(exception.getLength()).isEqualTo(1);
    }

    @Test
    void testTypicalMiniMessageErrorUseCase() {
        // Simulating a MiniMessage parse error with context lines
        ValueIndexedException exception = ValueIndexedException.builder()
            .message("Invalid tag")
            .startIndex(10)
            .length(15)
            .contextLinesBefore(2)
            .contextLinesAfter(1)
            .build();

        assertThat(exception.getStartIndex()).isEqualTo(10);
        assertThat(exception.getLength()).isEqualTo(15);
        assertThat(exception.getContextLinesBefore()).isEqualTo(2);
        assertThat(exception.getContextLinesAfter()).isEqualTo(1);
    }

    @Test
    void testTypicalJsonParseErrorUseCase() {
        // Simulating a JSON parse error with many context lines
        ValueIndexedException exception = ValueIndexedException.builder()
            .message("Unexpected token")
            .startIndex(42)
            .length(1)
            .contextLinesBefore(5)
            .contextLinesAfter(5)
            .build();

        assertThat(exception.getContextLinesBefore()).isEqualTo(5);
        assertThat(exception.getContextLinesAfter()).isEqualTo(5);
    }
}
