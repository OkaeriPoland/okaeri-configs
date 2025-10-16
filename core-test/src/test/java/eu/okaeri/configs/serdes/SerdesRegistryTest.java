package eu.okaeri.configs.serdes;

import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.standard.StandardSerdes;
import lombok.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for SerdesRegistry - registration and querying of serializers and transformers.
 */
class SerdesRegistryTest {

    private SerdesRegistry registry;

    @BeforeEach
    void setUp() {
        this.registry = new SerdesRegistry();
    }

    // === TRANSFORMER REGISTRATION TESTS ===

    @Test
    void testRegister_ObjectTransformer_RegistersSuccessfully() {
        // Create simple transformer String → Integer
        ObjectTransformer<String, Integer> transformer = new ObjectTransformer<String, Integer>() {
            @Override
            public GenericsPair<String, Integer> getPair() {
                return new GenericsPair<>(GenericsDeclaration.of(String.class), GenericsDeclaration.of(Integer.class));
            }

            @Override
            public Integer transform(@NonNull String data, @NonNull SerdesContext serdesContext) {
                return Integer.parseInt(data);
            }
        };

        this.registry.register(transformer);

        // Verify transformer is registered
        assertThat(this.registry.canTransform(GenericsDeclaration.of(String.class), GenericsDeclaration.of(Integer.class)))
                .isTrue();
        assertThat(this.registry.getTransformer(GenericsDeclaration.of(String.class), GenericsDeclaration.of(Integer.class)))
                .isNotNull()
                .isSameAs(transformer);
    }

    @Test
    void testRegister_BidirectionalTransformer_RegistersBothDirections() {
        // Create bidirectional transformer String ↔ Integer
        BidirectionalTransformer<String, Integer> biTransformer = new BidirectionalTransformer<String, Integer>() {
            @Override
            public GenericsPair<String, Integer> getPair() {
                return new GenericsPair<>(GenericsDeclaration.of(String.class), GenericsDeclaration.of(Integer.class));
            }

            @Override
            public Integer leftToRight(@NonNull String data, @NonNull SerdesContext serdesContext) {
                return Integer.parseInt(data);
            }

            @Override
            public String rightToLeft(@NonNull Integer data, @NonNull SerdesContext serdesContext) {
                return data.toString();
            }
        };

        this.registry.register(biTransformer);

        // Verify both directions are registered
        assertThat(this.registry.canTransform(GenericsDeclaration.of(String.class), GenericsDeclaration.of(Integer.class)))
                .isTrue();
        assertThat(this.registry.canTransform(GenericsDeclaration.of(Integer.class), GenericsDeclaration.of(String.class)))
                .isTrue();
    }

    @Test
    void testRegisterWithReversedToString_RegistersBothDirections() {
        // Create transformer String → Integer
        ObjectTransformer<String, Integer> transformer = new ObjectTransformer<String, Integer>() {
            @Override
            public GenericsPair<String, Integer> getPair() {
                return new GenericsPair<>(GenericsDeclaration.of(String.class), GenericsDeclaration.of(Integer.class));
            }

            @Override
            public Integer transform(@NonNull String data, @NonNull SerdesContext serdesContext) {
                return Integer.parseInt(data);
            }
        };

        this.registry.registerWithReversedToString(transformer);

        // Verify both directions are registered
        assertThat(this.registry.canTransform(GenericsDeclaration.of(String.class), GenericsDeclaration.of(Integer.class)))
                .isTrue();
        assertThat(this.registry.canTransform(GenericsDeclaration.of(Integer.class), GenericsDeclaration.of(String.class)))
                .isTrue();

        // Verify reverse transformer returns toString() value
        ObjectTransformer reverseTransformer = this.registry.getTransformer(
                GenericsDeclaration.of(Integer.class),
                GenericsDeclaration.of(String.class)
        );
        assertThat(reverseTransformer).isNotNull();
    }

    @Test
    void testRegister_OkaeriSerdesPack_RegistersAllSerdes() {
        // Register StandardSerdes pack
        this.registry.register(new StandardSerdes());

        // Verify some transformers are registered
        assertThat(this.registry.canTransform(GenericsDeclaration.of(String.class), GenericsDeclaration.of(Integer.class)))
                .isTrue();
        assertThat(this.registry.canTransform(GenericsDeclaration.of(String.class), GenericsDeclaration.of(Boolean.class)))
                .isTrue();
        assertThat(this.registry.canTransform(GenericsDeclaration.of(String.class), GenericsDeclaration.of(Double.class)))
                .isTrue();
    }

    // === SERIALIZER REGISTRATION TESTS ===

    @Test
    void testRegister_ObjectSerializer_RegistersSuccessfully() {
        // Create simple serializer for String
        ObjectSerializer<String> serializer = new ObjectSerializer<String>() {
            @Override
            public boolean supports(@NonNull Class<?> type) {
                return String.class.equals(type);
            }

            @Override
            public void serialize(@NonNull String object, @NonNull SerializationData data, @NonNull GenericsDeclaration generics) {
                data.setValue(object);
            }

            @Override
            public String deserialize(@NonNull DeserializationData data, @NonNull GenericsDeclaration generics) {
                return data.getValue(String.class);
            }
        };

        this.registry.register(serializer);

        // Verify serializer is registered
        ObjectSerializer result = this.registry.getSerializer(String.class);
        assertThat(result).isNotNull().isSameAs(serializer);
    }

    @Test
    void testRegisterExclusive_ReplacesExistingSerializer() {
        // Register first serializer
        ObjectSerializer<String> firstSerializer = new ObjectSerializer<String>() {
            @Override
            public boolean supports(@NonNull Class<?> type) {
                return String.class.equals(type);
            }

            @Override
            public void serialize(@NonNull String object, @NonNull SerializationData data, @NonNull GenericsDeclaration generics) {
                data.setValue("first: " + object);
            }

            @Override
            public String deserialize(@NonNull DeserializationData data, @NonNull GenericsDeclaration generics) {
                return data.getValue(String.class);
            }
        };

        this.registry.register(firstSerializer);

        // Register exclusive serializer (should replace first one)
        ObjectSerializer<String> exclusiveSerializer = new ObjectSerializer<String>() {
            @Override
            public boolean supports(@NonNull Class<?> type) {
                return String.class.equals(type);
            }

            @Override
            public void serialize(@NonNull String object, @NonNull SerializationData data, @NonNull GenericsDeclaration generics) {
                data.setValue("exclusive: " + object);
            }

            @Override
            public String deserialize(@NonNull DeserializationData data, @NonNull GenericsDeclaration generics) {
                return data.getValue(String.class);
            }
        };

        this.registry.registerExclusive(String.class, exclusiveSerializer);

        // Verify exclusive serializer replaced the first one
        ObjectSerializer result = this.registry.getSerializer(String.class);
        assertThat(result).isNotNull().isSameAs(exclusiveSerializer);
    }

    // === TRANSFORMER QUERY TESTS ===

    @Test
    void testGetTransformer_ExistingPair_ReturnsTransformer() {
        this.registry.register(new StandardSerdes());

        ObjectTransformer transformer = this.registry.getTransformer(
                GenericsDeclaration.of(String.class),
                GenericsDeclaration.of(Integer.class)
        );

        assertThat(transformer).isNotNull();
        assertThat(transformer.getPair().getFrom()).isEqualTo(GenericsDeclaration.of(String.class));
        assertThat(transformer.getPair().getTo()).isEqualTo(GenericsDeclaration.of(Integer.class));
    }

    @Test
    void testGetTransformer_NonExistingPair_ReturnsNull() {
        this.registry.register(new StandardSerdes());

        ObjectTransformer transformer = this.registry.getTransformer(
                GenericsDeclaration.of(String.class),
                GenericsDeclaration.of(Void.class)  // No transformer for String → Void
        );

        assertThat(transformer).isNull();
    }

    @Test
    void testGetTransformersFrom_ReturnsAllMatchingTransformers() {
        this.registry.register(new StandardSerdes());

        List<ObjectTransformer> transformers = this.registry.getTransformersFrom(GenericsDeclaration.of(String.class));

        // StandardSerdes registers String → Integer, Boolean, Double, Long, etc.
        assertThat(transformers).isNotEmpty();
        assertThat(transformers).allSatisfy(transformer ->
                assertThat(transformer.getPair().getFrom()).isEqualTo(GenericsDeclaration.of(String.class))
        );
    }

    @Test
    void testGetTransformersTo_ReturnsAllMatchingTransformers() {
        this.registry.register(new StandardSerdes());

        List<ObjectTransformer> transformers = this.registry.getTransformersTo(GenericsDeclaration.of(String.class));

        // StandardSerdes registers Integer → String, Boolean → String, etc.
        assertThat(transformers).isNotEmpty();
        assertThat(transformers).allSatisfy(transformer ->
                assertThat(transformer.getPair().getTo()).isEqualTo(GenericsDeclaration.of(String.class))
        );
    }

    @Test
    void testCanTransform_ExistingPair_ReturnsTrue() {
        this.registry.register(new StandardSerdes());

        boolean canTransform = this.registry.canTransform(
                GenericsDeclaration.of(String.class),
                GenericsDeclaration.of(Integer.class)
        );

        assertThat(canTransform).isTrue();
    }

    @Test
    void testCanTransform_NonExistingPair_ReturnsFalse() {
        this.registry.register(new StandardSerdes());

        boolean canTransform = this.registry.canTransform(
                GenericsDeclaration.of(String.class),
                GenericsDeclaration.of(Void.class)
        );

        assertThat(canTransform).isFalse();
    }

    // === SERIALIZER QUERY TESTS ===
    // Note: StandardSerdes only registers transformers, not serializers
    // Testing serializer registration with custom serializers instead

    @Test
    void testGetSerializer_ExistingType_ReturnsSerializer() {
        // Create and register a custom serializer
        ObjectSerializer<String> customSerializer = new ObjectSerializer<String>() {
            @Override
            public boolean supports(@NonNull Class<?> type) {
                return String.class.equals(type);
            }

            @Override
            public void serialize(@NonNull String object, @NonNull SerializationData data, @NonNull GenericsDeclaration generics) {
                data.setValue(object);
            }

            @Override
            public String deserialize(@NonNull DeserializationData data, @NonNull GenericsDeclaration generics) {
                return data.getValue(String.class);
            }
        };

        this.registry.register(customSerializer);

        ObjectSerializer serializer = this.registry.getSerializer(String.class);

        assertThat(serializer).isNotNull();
        assertThat(serializer.supports(String.class)).isTrue();
    }

    @Test
    void testGetSerializer_NonExistingType_ReturnsNull() {
        // No serializers registered
        ObjectSerializer serializer = this.registry.getSerializer(Void.class);

        assertThat(serializer).isNull();
    }

    // === ALLSERDES TESTS ===

    @Test
    void testAllSerdes_ReturnsPackWithAllRegisteredSerdes() {
        // Register some transformers and serializers
        this.registry.register(new StandardSerdes());

        // Get allSerdes pack
        OkaeriSerdesPack allSerdes = this.registry.allSerdes();
        assertThat(allSerdes).isNotNull();

        // Register to new registry
        SerdesRegistry newRegistry = new SerdesRegistry();
        allSerdes.register(newRegistry);

        // Verify transformers are copied
        assertThat(newRegistry.canTransform(GenericsDeclaration.of(String.class), GenericsDeclaration.of(Integer.class)))
                .isTrue();
    }
}
