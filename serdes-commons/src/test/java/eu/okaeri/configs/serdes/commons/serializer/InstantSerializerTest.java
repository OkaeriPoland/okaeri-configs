package eu.okaeri.configs.serdes.commons.serializer;

import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.DeserializationData;
import eu.okaeri.configs.serdes.SerializationData;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class InstantSerializerTest {

    private final GenericsDeclaration generics = mock(GenericsDeclaration.class);

    // ==================== supports() ====================

    @Test
    void testSupports_Instant() {
        InstantSerializer serializer = new InstantSerializer(false);
        assertThat(serializer.supports(Instant.class)).isTrue();
    }

    @Test
    void testSupports_OtherClass() {
        InstantSerializer serializer = new InstantSerializer(false);
        assertThat(serializer.supports(String.class)).isFalse();
        assertThat(serializer.supports(Long.class)).isFalse();
    }

    // ==================== String Mode (numeric=false) ====================

    @Nested
    class StringMode {

        private final InstantSerializer serializer = new InstantSerializer(false);

        @Test
        void testSerialize_Instant() {
            Instant instant = Instant.parse("2023-01-15T10:30:00Z");
            SerializationData data = mock(SerializationData.class);

            this.serializer.serialize(instant, data, InstantSerializerTest.this.generics);

            verify(data).setValue("2023-01-15T10:30:00Z");
        }

        @Test
        void testSerialize_Epoch() {
            Instant instant = Instant.EPOCH;
            SerializationData data = mock(SerializationData.class);

            this.serializer.serialize(instant, data, InstantSerializerTest.this.generics);

            verify(data).setValue("1970-01-01T00:00:00Z");
        }

        @Test
        void testDeserialize_IsoFormat() {
            DeserializationData data = mock(DeserializationData.class);
            when(data.getValue(String.class)).thenReturn("2023-01-15T10:30:00Z");

            Instant result = this.serializer.deserialize(data, InstantSerializerTest.this.generics);

            assertThat(result).isEqualTo(Instant.parse("2023-01-15T10:30:00Z"));
        }

        @Test
        void testDeserialize_EpochMillis() {
            DeserializationData data = mock(DeserializationData.class);
            when(data.getValue(String.class)).thenReturn("1673778600000");

            Instant result = this.serializer.deserialize(data, InstantSerializerTest.this.generics);

            assertThat(result).isEqualTo(Instant.ofEpochMilli(1673778600000L));
        }

        @Test
        void testDeserialize_EpochZero() {
            DeserializationData data = mock(DeserializationData.class);
            when(data.getValue(String.class)).thenReturn("0");

            Instant result = this.serializer.deserialize(data, InstantSerializerTest.this.generics);

            assertThat(result).isEqualTo(Instant.EPOCH);
        }
    }

    // ==================== Numeric Mode (numeric=true) ====================

    @Nested
    class NumericMode {

        private final InstantSerializer serializer = new InstantSerializer(true);

        @Test
        void testSerialize_Instant() {
            Instant instant = Instant.ofEpochMilli(1673778600000L);
            SerializationData data = mock(SerializationData.class);

            this.serializer.serialize(instant, data, InstantSerializerTest.this.generics);

            verify(data).setValue(1673778600000L, Long.class);
        }

        @Test
        void testSerialize_Epoch() {
            Instant instant = Instant.EPOCH;
            SerializationData data = mock(SerializationData.class);

            this.serializer.serialize(instant, data, InstantSerializerTest.this.generics);

            verify(data).setValue(0L, Long.class);
        }

        @Test
        void testDeserialize_EpochMillis() {
            DeserializationData data = mock(DeserializationData.class);
            when(data.getValue(String.class)).thenReturn("1673778600000");

            Instant result = this.serializer.deserialize(data, InstantSerializerTest.this.generics);

            assertThat(result).isEqualTo(Instant.ofEpochMilli(1673778600000L));
        }

        @Test
        void testDeserialize_IsoFallback() {
            // Even in numeric mode, deserialize should handle ISO format as fallback
            DeserializationData data = mock(DeserializationData.class);
            when(data.getValue(String.class)).thenReturn("2023-01-15T10:30:00Z");

            Instant result = this.serializer.deserialize(data, InstantSerializerTest.this.generics);

            assertThat(result).isEqualTo(Instant.parse("2023-01-15T10:30:00Z"));
        }
    }

    // ==================== Edge Cases ====================

    @Nested
    class EdgeCases {

        private final InstantSerializer serializer = new InstantSerializer(false);

        @Test
        void testDeserialize_NegativeEpochMillis() {
            DeserializationData data = mock(DeserializationData.class);
            when(data.getValue(String.class)).thenReturn("-1000");

            Instant result = this.serializer.deserialize(data, InstantSerializerTest.this.generics);

            assertThat(result).isEqualTo(Instant.ofEpochMilli(-1000L));
        }

        @Test
        void testDeserialize_DecimalString() {
            // BigDecimal parses, then uses longValueExact
            DeserializationData data = mock(DeserializationData.class);
            when(data.getValue(String.class)).thenReturn("1000.0");

            Instant result = this.serializer.deserialize(data, InstantSerializerTest.this.generics);

            assertThat(result).isEqualTo(Instant.ofEpochMilli(1000L));
        }

        @Test
        void testSerialize_WithNanoseconds() {
            Instant instant = Instant.parse("2023-01-15T10:30:00.123456789Z");
            SerializationData data = mock(SerializationData.class);

            this.serializer.serialize(instant, data, InstantSerializerTest.this.generics);

            // Note: toEpochMilli() truncates nanoseconds for numeric mode
            // String mode preserves full precision
            verify(data).setValue("2023-01-15T10:30:00.123456789Z");
        }
    }
}
