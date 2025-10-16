package eu.okaeri.configs.serdes;

import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.configurer.InMemoryConfigurer;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.standard.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive test suite for StandardSerdes.
 * Tests all built-in transformers registered by StandardSerdes.
 */
class StandardSerdesTest {

    private SerdesRegistry registry;
    private Configurer configurer;
    private SerdesContext context;

    @BeforeEach
    void setUp() {
        this.configurer = new InMemoryConfigurer();
        this.registry = this.configurer.getRegistry();
        this.context = SerdesContext.of(this.configurer);
    }

    // ============================================
    // HELPER METHODS
    // ============================================

    @SuppressWarnings("unchecked")
    private <F, T> ObjectTransformer<F, T> getTransformer(Class<F> from, Class<T> to) {
        GenericsDeclaration fromDecl = GenericsDeclaration.of(from);
        GenericsDeclaration toDecl = GenericsDeclaration.of(to);
        return (ObjectTransformer<F, T>) this.registry.getTransformer(fromDecl, toDecl);
    }

    private boolean canTransform(Class<?> from, Class<?> to) {
        GenericsDeclaration fromDecl = GenericsDeclaration.of(from);
        GenericsDeclaration toDecl = GenericsDeclaration.of(to);
        return this.registry.canTransform(fromDecl, toDecl);
    }

    // ============================================
    // 1. REGISTRATION TESTS
    // ============================================

    @Test
    void testRegistration_ObjectToStringTransformer_IsRegistered() {
        ObjectTransformer<?, ?> transformer = this.getTransformer(Object.class, String.class);
        
        assertThat(transformer).isNotNull();
        assertThat(transformer).isInstanceOf(ObjectToStringTransformer.class);
    }

    @Test
    void testRegistration_StringToStringTransformer_IsRegistered() {
        ObjectTransformer<?, ?> transformer = this.getTransformer(String.class, String.class);
        
        assertThat(transformer).isNotNull();
        assertThat(transformer).isInstanceOf(StringToStringTransformer.class);
    }

    @Test
    void testRegistration_AllStringToTypeTransformers_AreRegistered() {
        assertThat(this.getTransformer(String.class, BigDecimal.class)).isInstanceOf(StringToBigDecimalTransformer.class);
        assertThat(this.getTransformer(String.class, BigInteger.class)).isInstanceOf(StringToBigIntegerTransformer.class);
        assertThat(this.getTransformer(String.class, Boolean.class)).isInstanceOf(StringToBooleanTransformer.class);
        assertThat(this.getTransformer(String.class, Byte.class)).isInstanceOf(StringToByteTransformer.class);
        assertThat(this.getTransformer(String.class, Character.class)).isInstanceOf(StringToCharacterTransformer.class);
        assertThat(this.getTransformer(String.class, Double.class)).isInstanceOf(StringToDoubleTransformer.class);
        assertThat(this.getTransformer(String.class, Float.class)).isInstanceOf(StringToFloatTransformer.class);
        assertThat(this.getTransformer(String.class, Integer.class)).isInstanceOf(StringToIntegerTransformer.class);
        assertThat(this.getTransformer(String.class, Long.class)).isInstanceOf(StringToLongTransformer.class);
        assertThat(this.getTransformer(String.class, Short.class)).isInstanceOf(StringToShortTransformer.class);
        assertThat(this.getTransformer(String.class, UUID.class)).isInstanceOf(StringToUuidTransformer.class);
    }

    @Test
    void testRegistration_AllTypeToStringTransformers_AreRegistered() {
        // These are created via registerWithReversedToString()
        assertThat(this.getTransformer(BigDecimal.class, String.class)).isNotNull();
        assertThat(this.getTransformer(BigInteger.class, String.class)).isNotNull();
        assertThat(this.getTransformer(Boolean.class, String.class)).isNotNull();
        assertThat(this.getTransformer(Byte.class, String.class)).isNotNull();
        assertThat(this.getTransformer(Character.class, String.class)).isNotNull();
        assertThat(this.getTransformer(Double.class, String.class)).isNotNull();
        assertThat(this.getTransformer(Float.class, String.class)).isNotNull();
        assertThat(this.getTransformer(Integer.class, String.class)).isNotNull();
        assertThat(this.getTransformer(Long.class, String.class)).isNotNull();
        assertThat(this.getTransformer(Short.class, String.class)).isNotNull();
        assertThat(this.getTransformer(UUID.class, String.class)).isNotNull();
    }

    // ============================================
    // 2. STRING → TYPE TRANSFORMATIONS
    // ============================================

    @Test
    void testTransform_StringToBigDecimal_ValidInput() {
        ObjectTransformer<String, BigDecimal> transformer = this.getTransformer(String.class, BigDecimal.class);
        
        BigDecimal result = transformer.transform("123.456789012345678901234567890", this.context);
        assertThat(result).isEqualByComparingTo(new BigDecimal("123.456789012345678901234567890"));
    }

    @Test
    void testTransform_StringToBigInteger_ValidInput() {
        ObjectTransformer<String, BigInteger> transformer = this.getTransformer(String.class, BigInteger.class);
        
        BigInteger result = transformer.transform("999999999999999999999999999999", this.context);
        assertThat(result).isEqualByComparingTo(new BigInteger("999999999999999999999999999999"));
    }

    @Test
    void testTransform_StringToBoolean_TrueVariants() {
        ObjectTransformer<String, Boolean> transformer = this.getTransformer(String.class, Boolean.class);
        
        assertThat(transformer.transform("true", this.context)).isTrue();
        assertThat(transformer.transform("TRUE", this.context)).isTrue();
        assertThat(transformer.transform("True", this.context)).isTrue();
    }

    @Test
    void testTransform_StringToBoolean_FalseVariants() {
        ObjectTransformer<String, Boolean> transformer = this.getTransformer(String.class, Boolean.class);
        
        assertThat(transformer.transform("false", this.context)).isFalse();
        assertThat(transformer.transform("FALSE", this.context)).isFalse();
        assertThat(transformer.transform("False", this.context)).isFalse();
    }

    @Test
    void testTransform_StringToByte_ValidInput() {
        ObjectTransformer<String, Byte> transformer = this.getTransformer(String.class, Byte.class);
        
        assertThat(transformer.transform("127", this.context)).isEqualTo((byte) 127);
        assertThat(transformer.transform("-128", this.context)).isEqualTo((byte) -128);
        assertThat(transformer.transform("0", this.context)).isEqualTo((byte) 0);
    }

    @Test
    void testTransform_StringToCharacter_ValidInput() {
        ObjectTransformer<String, Character> transformer = this.getTransformer(String.class, Character.class);
        
        assertThat(transformer.transform("A", this.context)).isEqualTo('A');
        assertThat(transformer.transform("€", this.context)).isEqualTo('€');
        assertThat(transformer.transform("Ω", this.context)).isEqualTo('Ω');
    }

    @Test
    void testTransform_StringToCharacter_MultipleChars_ThrowsException() {
        ObjectTransformer<String, Character> transformer = this.getTransformer(String.class, Character.class);
        
        // StringToCharacterTransformer enforces single character strings
        assertThatThrownBy(() -> transformer.transform("ABC", this.context))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("too long");
    }

    @Test
    void testTransform_StringToFloat_ValidInput() {
        ObjectTransformer<String, Float> transformer = this.getTransformer(String.class, Float.class);
        
        assertThat(transformer.transform("3.14", this.context)).isEqualTo(3.14f);
        assertThat(transformer.transform("-1.5", this.context)).isEqualTo(-1.5f);
        assertThat(transformer.transform("0.0", this.context)).isEqualTo(0.0f);
    }

    @Test
    void testTransform_StringToInteger_ValidInput() {
        ObjectTransformer<String, Integer> transformer = this.getTransformer(String.class, Integer.class);
        
        assertThat(transformer.transform("123", this.context)).isEqualTo(123);
        assertThat(transformer.transform("-456", this.context)).isEqualTo(-456);
        assertThat(transformer.transform("0", this.context)).isEqualTo(0);
        assertThat(transformer.transform("2147483647", this.context)).isEqualTo(Integer.MAX_VALUE);
        assertThat(transformer.transform("-2147483648", this.context)).isEqualTo(Integer.MIN_VALUE);
    }

    @Test
    void testTransform_StringToLong_ValidInput() {
        ObjectTransformer<String, Long> transformer = this.getTransformer(String.class, Long.class);
        
        assertThat(transformer.transform("9223372036854775807", this.context)).isEqualTo(Long.MAX_VALUE);
        assertThat(transformer.transform("-9223372036854775808", this.context)).isEqualTo(Long.MIN_VALUE);
        assertThat(transformer.transform("0", this.context)).isEqualTo(0L);
    }

    @Test
    void testTransform_StringToShort_ValidInput() {
        ObjectTransformer<String, Short> transformer = this.getTransformer(String.class, Short.class);
        
        assertThat(transformer.transform("32767", this.context)).isEqualTo((short) 32767);
        assertThat(transformer.transform("-32768", this.context)).isEqualTo((short) -32768);
        assertThat(transformer.transform("0", this.context)).isEqualTo((short) 0);
    }

    @Test
    void testTransform_StringToUuid_ValidInput() {
        ObjectTransformer<String, UUID> transformer = this.getTransformer(String.class, UUID.class);
        
        UUID expected = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UUID result = transformer.transform("550e8400-e29b-41d4-a716-446655440000", this.context);
        
        assertThat(result).isEqualTo(expected);
    }

    // ============================================
    // 3. TYPE → STRING TRANSFORMATIONS
    // ============================================

    @Test
    void testTransform_BigDecimalToString_ValidInput() {
        ObjectTransformer<BigDecimal, String> transformer = this.getTransformer(BigDecimal.class, String.class);
        
        String result = transformer.transform(new BigDecimal("123.456"), this.context);
        assertThat(result).isEqualTo("123.456");
    }

    @Test
    void testTransform_BigIntegerToString_ValidInput() {
        ObjectTransformer<BigInteger, String> transformer = this.getTransformer(BigInteger.class, String.class);
        
        String result = transformer.transform(new BigInteger("999999999999999999999999999999"), this.context);
        assertThat(result).isEqualTo("999999999999999999999999999999");
    }

    @Test
    void testTransform_BooleanToString_ValidInput() {
        ObjectTransformer<Boolean, String> transformer = this.getTransformer(Boolean.class, String.class);
        
        assertThat(transformer.transform(true, this.context)).isEqualTo("true");
        assertThat(transformer.transform(false, this.context)).isEqualTo("false");
    }

    @Test
    void testTransform_ByteToString_ValidInput() {
        ObjectTransformer<Byte, String> transformer = this.getTransformer(Byte.class, String.class);
        
        assertThat(transformer.transform((byte) 127, this.context)).isEqualTo("127");
        assertThat(transformer.transform((byte) -128, this.context)).isEqualTo("-128");
        assertThat(transformer.transform((byte) 0, this.context)).isEqualTo("0");
    }

    @Test
    void testTransform_CharacterToString_ValidInput() {
        ObjectTransformer<Character, String> transformer = this.getTransformer(Character.class, String.class);
        
        assertThat(transformer.transform('A', this.context)).isEqualTo("A");
        assertThat(transformer.transform('€', this.context)).isEqualTo("€");
        assertThat(transformer.transform('Ω', this.context)).isEqualTo("Ω");
    }

    @Test
    void testTransform_DoubleToString_ValidInput() {
        ObjectTransformer<Double, String> transformer = this.getTransformer(Double.class, String.class);
        
        assertThat(transformer.transform(3.14159, this.context)).isEqualTo("3.14159");
        assertThat(transformer.transform(-2.71828, this.context)).isEqualTo("-2.71828");
        assertThat(transformer.transform(0.0, this.context)).isEqualTo("0.0");
    }

    @Test
    void testTransform_FloatToString_ValidInput() {
        ObjectTransformer<Float, String> transformer = this.getTransformer(Float.class, String.class);
        
        assertThat(transformer.transform(3.14f, this.context)).isEqualTo("3.14");
        assertThat(transformer.transform(-1.5f, this.context)).isEqualTo("-1.5");
        assertThat(transformer.transform(0.0f, this.context)).isEqualTo("0.0");
    }

    @Test
    void testTransform_IntegerToString_ValidInput() {
        ObjectTransformer<Integer, String> transformer = this.getTransformer(Integer.class, String.class);
        
        assertThat(transformer.transform(123, this.context)).isEqualTo("123");
        assertThat(transformer.transform(-456, this.context)).isEqualTo("-456");
        assertThat(transformer.transform(0, this.context)).isEqualTo("0");
    }

    @Test
    void testTransform_LongToString_ValidInput() {
        ObjectTransformer<Long, String> transformer = this.getTransformer(Long.class, String.class);
        
        assertThat(transformer.transform(9223372036854775807L, this.context)).isEqualTo("9223372036854775807");
        assertThat(transformer.transform(-9223372036854775808L, this.context)).isEqualTo("-9223372036854775808");
        assertThat(transformer.transform(0L, this.context)).isEqualTo("0");
    }

    @Test
    void testTransform_ShortToString_ValidInput() {
        ObjectTransformer<Short, String> transformer = this.getTransformer(Short.class, String.class);
        
        assertThat(transformer.transform((short) 32767, this.context)).isEqualTo("32767");
        assertThat(transformer.transform((short) -32768, this.context)).isEqualTo("-32768");
        assertThat(transformer.transform((short) 0, this.context)).isEqualTo("0");
    }

    @Test
    void testTransform_UuidToString_ValidInput() {
        ObjectTransformer<UUID, String> transformer = this.getTransformer(UUID.class, String.class);
        
        UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        String result = transformer.transform(uuid, this.context);
        
        assertThat(result).isEqualTo("550e8400-e29b-41d4-a716-446655440000");
    }

    // ============================================
    // 4. OBJECT → STRING MAGIC TRANSFORMER
    // ============================================

    @Test
    void testTransform_ObjectToString_WithToStringMethod() {
        ObjectTransformer<Object, String> transformer = this.getTransformer(Object.class, String.class);
        
        Object obj = new Object() {
            @Override
            public String toString() {
                return "custom-string-representation";
            }
        };
        
        assertThat(transformer.transform(obj, this.context)).isEqualTo("custom-string-representation");
    }

    @Test
    void testTransform_ObjectToString_WithInteger() {
        ObjectTransformer<Object, String> transformer = this.getTransformer(Object.class, String.class);
        
        assertThat(transformer.transform(42, this.context)).isEqualTo("42");
    }

    @Test
    void testTransform_ObjectToString_WithBoolean() {
        ObjectTransformer<Object, String> transformer = this.getTransformer(Object.class, String.class);
        
        assertThat(transformer.transform(true, this.context)).isEqualTo("true");
    }

    // ============================================
    // 5. EDGE CASES
    // ============================================

    @Test
    void testTransform_StringToInteger_InvalidFormat_ThrowsException() {
        ObjectTransformer<String, Integer> transformer = this.getTransformer(String.class, Integer.class);
        
        assertThatThrownBy(() -> transformer.transform("abc", this.context))
            .isInstanceOf(NumberFormatException.class);
    }

    @Test
    void testTransform_StringToInteger_Overflow_ThrowsException() {
        ObjectTransformer<String, Integer> transformer = this.getTransformer(String.class, Integer.class);
        
        assertThatThrownBy(() -> transformer.transform("999999999999999999999", this.context))
            .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void testTransform_StringToDouble_InvalidFormat_ThrowsException() {
        ObjectTransformer<String, Double> transformer = this.getTransformer(String.class, Double.class);
        
        assertThatThrownBy(() -> transformer.transform("not-a-number", this.context))
            .isInstanceOf(NumberFormatException.class);
    }

    @Test
    void testTransform_StringToUuid_InvalidFormat_ThrowsException() {
        ObjectTransformer<String, UUID> transformer = this.getTransformer(String.class, UUID.class);
        
        assertThatThrownBy(() -> transformer.transform("not-a-uuid", this.context))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testTransform_StringToCharacter_EmptyString_ThrowsException() {
        ObjectTransformer<String, Character> transformer = this.getTransformer(String.class, Character.class);
        
        assertThatThrownBy(() -> transformer.transform("", this.context))
            .isInstanceOf(Exception.class);
    }

    // ============================================
    // 6. ROUND-TRIP TESTS
    // ============================================

    @Test
    void testRoundTrip_Integer_StringInteger() {
        ObjectTransformer<String, Integer> strToInt = this.getTransformer(String.class, Integer.class);
        ObjectTransformer<Integer, String> intToStr = this.getTransformer(Integer.class, String.class);
        
        String original = "12345";
        Integer intermediate = strToInt.transform(original, this.context);
        String result = intToStr.transform(intermediate, this.context);
        
        assertThat(result).isEqualTo(original);
    }

    @Test
    void testRoundTrip_Double_StringDouble() {
        ObjectTransformer<String, Double> strToDouble = this.getTransformer(String.class, Double.class);
        ObjectTransformer<Double, String> doubleToStr = this.getTransformer(Double.class, String.class);
        
        String original = "3.14159";
        Double intermediate = strToDouble.transform(original, this.context);
        String result = doubleToStr.transform(intermediate, this.context);
        
        assertThat(result).isEqualTo(original);
    }

    @Test
    void testRoundTrip_Boolean_StringBoolean() {
        ObjectTransformer<String, Boolean> strToBool = this.getTransformer(String.class, Boolean.class);
        ObjectTransformer<Boolean, String> boolToStr = this.getTransformer(Boolean.class, String.class);
        
        String original = "true";
        Boolean intermediate = strToBool.transform(original, this.context);
        String result = boolToStr.transform(intermediate, this.context);
        
        assertThat(result).isEqualTo(original);
    }

    @Test
    void testRoundTrip_UUID_StringUuid() {
        ObjectTransformer<String, UUID> strToUuid = this.getTransformer(String.class, UUID.class);
        ObjectTransformer<UUID, String> uuidToStr = this.getTransformer(UUID.class, String.class);
        
        String original = "550e8400-e29b-41d4-a716-446655440000";
        UUID intermediate = strToUuid.transform(original, this.context);
        String result = uuidToStr.transform(intermediate, this.context);
        
        assertThat(result).isEqualTo(original);
    }

    @Test
    void testRoundTrip_BigDecimal_StringBigDecimal() {
        ObjectTransformer<String, BigDecimal> strToBigDec = this.getTransformer(String.class, BigDecimal.class);
        ObjectTransformer<BigDecimal, String> bigDecToStr = this.getTransformer(BigDecimal.class, String.class);
        
        String original = "123.456789012345678901234567890";
        BigDecimal intermediate = strToBigDec.transform(original, this.context);
        String result = bigDecToStr.transform(intermediate, this.context);
        
        assertThat(result).isEqualTo(original);
    }

    // ============================================
    // 7. INTEGRATION WITH SERDESREGISTRY
    // ============================================

    @Test
    void testRegistry_CanTransform_StringToInteger_ReturnsTrue() {
        assertThat(this.canTransform(String.class, Integer.class)).isTrue();
    }

    @Test
    void testRegistry_CanTransform_IntegerToString_ReturnsTrue() {
        assertThat(this.canTransform(Integer.class, String.class)).isTrue();
    }

    @Test
    void testRegistry_CanTransform_StringToCustomClass_ReturnsFalse() {
        class CustomClass {}
        assertThat(this.canTransform(String.class, CustomClass.class)).isFalse();
    }

    @Test
    void testRegistry_GetTransformer_StringToInteger_ReturnsCorrectTransformer() {
        ObjectTransformer<?, ?> transformer = this.getTransformer(String.class, Integer.class);
        
        assertThat(transformer).isNotNull();
        assertThat(transformer).isInstanceOf(StringToIntegerTransformer.class);
    }

    @Test
    void testRegistry_GetTransformer_NonExistentTransformation_ReturnsNull() {
        // Use GenericsDeclaration directly since local classes can't be used with generic methods
        GenericsDeclaration from = GenericsDeclaration.of(String.class);
        GenericsDeclaration to = GenericsDeclaration.of(AtomicReference.class);
        ObjectTransformer<?, ?> transformer = this.registry.getTransformer(from, to);
        
        assertThat(transformer).isNull();
    }
}
