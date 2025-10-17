package eu.okaeri.configs.serdes;

import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.serializable.ConfigSerializable;
import eu.okaeri.configs.serdes.serializable.ConfigSerializableSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for SerdesRegistry serializer registration order and precedence.
 * Verifies which serializer is selected when multiple serializers support the same type.
 */
class SerdesRegistryOrderTest {

    private SerdesRegistry registry;

    @BeforeEach
    void setUp() {
        this.registry = new SerdesRegistry();
    }

    // === TEST CLASSES ===

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestType {
        private String value;
    }

    public static class FirstSerializer implements ObjectSerializer<TestType> {
        @Override
        public boolean supports(Class<?> type) {
            return TestType.class.isAssignableFrom(type);
        }

        @Override
        public void serialize(TestType object, SerializationData data, GenericsDeclaration generics) {
            data.add("value", "first-serializer");
        }

        @Override
        public TestType deserialize(DeserializationData data, GenericsDeclaration generics) {
            return new TestType("first-serializer");
        }
    }

    public static class SecondSerializer implements ObjectSerializer<TestType> {
        @Override
        public boolean supports(Class<?> type) {
            return TestType.class.isAssignableFrom(type);
        }

        @Override
        public void serialize(TestType object, SerializationData data, GenericsDeclaration generics) {
            data.add("value", "second-serializer");
        }

        @Override
        public TestType deserialize(DeserializationData data, GenericsDeclaration generics) {
            return new TestType("second-serializer");
        }
    }

    public static class ThirdSerializer implements ObjectSerializer<TestType> {
        @Override
        public boolean supports(Class<?> type) {
            return TestType.class.isAssignableFrom(type);
        }

        @Override
        public void serialize(TestType object, SerializationData data, GenericsDeclaration generics) {
            data.add("value", "third-serializer");
        }

        @Override
        public TestType deserialize(DeserializationData data, GenericsDeclaration generics) {
            return new TestType("third-serializer");
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfigSerializableType implements ConfigSerializable {
        private String value;

        @Override
        public void serialize(SerializationData data, GenericsDeclaration generics) {
            data.add("value", "config-serializable");
        }

        public static ConfigSerializableType deserialize(DeserializationData data, GenericsDeclaration generics) {
            return new ConfigSerializableType("config-serializable");
        }
    }

    public static class ConfigSerializableOverrideSerializer implements ObjectSerializer<ConfigSerializableType> {
        @Override
        public boolean supports(Class<?> type) {
            return ConfigSerializableType.class.isAssignableFrom(type);
        }

        @Override
        public void serialize(ConfigSerializableType object, SerializationData data, GenericsDeclaration generics) {
            data.add("value", "override-serializer");
        }

        @Override
        public ConfigSerializableType deserialize(DeserializationData data, GenericsDeclaration generics) {
            return new ConfigSerializableType("override-serializer");
        }
    }

    // === BASIC ORDERING TESTS ===

    @Test
    void testRegistrationOrder_LastRegisteredWins() {
        this.registry.register(new FirstSerializer());
        this.registry.register(new SecondSerializer());

        ObjectSerializer<TestType> serializer = this.registry.getSerializer(TestType.class);

        assertThat(serializer).isInstanceOf(SecondSerializer.class);
    }

    @Test
    void testRegistrationOrder_ThreeSerializers_LastWins() {
        this.registry.register(new FirstSerializer());
        this.registry.register(new SecondSerializer());
        this.registry.register(new ThirdSerializer());

        ObjectSerializer<TestType> serializer = this.registry.getSerializer(TestType.class);

        assertThat(serializer).isInstanceOf(ThirdSerializer.class);
    }

    @Test
    void testRegistrationOrder_ReverseOrder_LastWins() {
        this.registry.register(new ThirdSerializer());
        this.registry.register(new SecondSerializer());
        this.registry.register(new FirstSerializer());

        ObjectSerializer<TestType> serializer = this.registry.getSerializer(TestType.class);

        assertThat(serializer).isInstanceOf(FirstSerializer.class);
    }

    // === CONFIGSERIALIZABLE PRECEDENCE TESTS ===

    @Test
    void testConfigSerializable_NoExplicitSerializer_UsesConfigSerializableSerializer() {
        this.registry.register(new ConfigSerializableSerializer());

        ObjectSerializer<ConfigSerializableType> serializer = this.registry.getSerializer(ConfigSerializableType.class);

        assertThat(serializer).isInstanceOf(ConfigSerializableSerializer.class);
    }

    @Test
    void testConfigSerializable_ExplicitSerializerRegisteredAfter_OverridesConfigSerializable() {
        this.registry.register(new ConfigSerializableSerializer());
        this.registry.register(new ConfigSerializableOverrideSerializer());

        ObjectSerializer<ConfigSerializableType> serializer = this.registry.getSerializer(ConfigSerializableType.class);

        assertThat(serializer).isInstanceOf(ConfigSerializableOverrideSerializer.class);
    }

    @Test
    void testConfigSerializable_ExplicitSerializerRegisteredBefore_ConfigSerializableWins() {
        this.registry.register(new ConfigSerializableOverrideSerializer());
        this.registry.register(new ConfigSerializableSerializer());

        ObjectSerializer<ConfigSerializableType> serializer = this.registry.getSerializer(ConfigSerializableType.class);

        assertThat(serializer).isInstanceOf(ConfigSerializableSerializer.class);
    }

    // === SUPPORTS() METHOD TESTS ===

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParentType {
        private String value;
    }

    @Data
    @NoArgsConstructor
    @EqualsAndHashCode(callSuper = false)
    public static class ChildType extends ParentType {
        public ChildType(String value) {
            super(value);
        }
    }

    public static class ParentSerializer implements ObjectSerializer<ParentType> {
        @Override
        public boolean supports(Class<?> type) {
            return ParentType.class.isAssignableFrom(type);
        }

        @Override
        public void serialize(ParentType object, SerializationData data, GenericsDeclaration generics) {
            data.add("value", "parent-serializer");
        }

        @Override
        public ParentType deserialize(DeserializationData data, GenericsDeclaration generics) {
            return new ParentType("parent-serializer");
        }
    }

    public static class ChildSerializer implements ObjectSerializer<ChildType> {
        @Override
        public boolean supports(Class<?> type) {
            return ChildType.class.isAssignableFrom(type);
        }

        @Override
        public void serialize(ChildType object, SerializationData data, GenericsDeclaration generics) {
            data.add("value", "child-serializer");
        }

        @Override
        public ChildType deserialize(DeserializationData data, GenericsDeclaration generics) {
            return new ChildType("child-serializer");
        }
    }

    @Test
    void testInheritance_ChildTypeWithParentSerializer_ParentSerializerSupportsIt() {
        this.registry.register(new ParentSerializer());

        ObjectSerializer<?> serializer = this.registry.getSerializer(ChildType.class);

        assertThat(serializer).isInstanceOf(ParentSerializer.class);
    }

    @Test
    void testInheritance_BothSerializers_ChildSerializerWinsIfRegisteredLast() {
        this.registry.register(new ParentSerializer());
        this.registry.register(new ChildSerializer());

        ObjectSerializer<?> serializer = this.registry.getSerializer(ChildType.class);

        assertThat(serializer).isInstanceOf(ChildSerializer.class);
    }

    @Test
    void testInheritance_BothSerializers_ParentSerializerWinsIfRegisteredLast() {
        this.registry.register(new ChildSerializer());
        this.registry.register(new ParentSerializer());

        ObjectSerializer<?> serializer = this.registry.getSerializer(ChildType.class);

        assertThat(serializer).isInstanceOf(ParentSerializer.class);
    }

    @Test
    void testInheritance_ParentType_OnlyParentSerializerSupports() {
        this.registry.register(new ParentSerializer());
        this.registry.register(new ChildSerializer());

        ObjectSerializer<?> serializer = this.registry.getSerializer(ParentType.class);

        assertThat(serializer).isInstanceOf(ParentSerializer.class);
    }

    // === EXCLUSIVE REGISTRATION TESTS ===

    @Test
    void testExclusiveRegistration_SingleSerializer_Succeeds() {
        this.registry.registerExclusive(TestType.class, new FirstSerializer());

        ObjectSerializer<TestType> serializer = this.registry.getSerializer(TestType.class);
        assertThat(serializer).isInstanceOf(FirstSerializer.class);
    }

    @Test
    void testExclusiveRegistration_AlreadyRegistered_RemovesOldAddsNew() {
        this.registry.registerExclusive(TestType.class, new FirstSerializer());
        this.registry.registerExclusive(TestType.class, new SecondSerializer());

        ObjectSerializer<TestType> serializer = this.registry.getSerializer(TestType.class);
        assertThat(serializer).isInstanceOf(SecondSerializer.class);
    }

    @Test
    void testExclusiveRegistration_AfterNormalRegistration_RemovesOldAddsNew() {
        this.registry.register(new FirstSerializer());
        this.registry.registerExclusive(TestType.class, new SecondSerializer());

        ObjectSerializer<TestType> serializer = this.registry.getSerializer(TestType.class);
        assertThat(serializer).isInstanceOf(SecondSerializer.class);
    }

    @Test
    void testNormalRegistration_AfterExclusiveRegistration_BothPresent_LastWins() {
        this.registry.registerExclusive(TestType.class, new FirstSerializer());
        this.registry.register(new SecondSerializer());

        ObjectSerializer<TestType> serializer = this.registry.getSerializer(TestType.class);
        assertThat(serializer).isInstanceOf(SecondSerializer.class);
    }

    // === MULTIPLE SERIALIZERS QUERY TESTS ===

    @Test
    void testGetSerializer_NoMatch_ReturnsNull() {
        ObjectSerializer<?> serializer = this.registry.getSerializer(String.class);

        assertThat(serializer).isNull();
    }

    @Test
    void testGetSerializer_MultipleMatches_ReturnsLastRegistered() {
        this.registry.register(new FirstSerializer());
        this.registry.register(new SecondSerializer());
        this.registry.register(new ThirdSerializer());

        ObjectSerializer<TestType> serializer = this.registry.getSerializer(TestType.class);

        assertThat(serializer).isInstanceOf(ThirdSerializer.class);
    }

    // === REAL-WORLD SCENARIO TESTS ===

    @Test
    void testRealWorld_StandardSerdesThenCustom_CustomWins() {
        this.registry.register(new ConfigSerializableSerializer());
        this.registry.register(new ConfigSerializableOverrideSerializer());

        ObjectSerializer<ConfigSerializableType> serializer = this.registry.getSerializer(ConfigSerializableType.class);

        assertThat(serializer).isInstanceOf(ConfigSerializableOverrideSerializer.class);
    }

    @Test
    void testRealWorld_CustomThenStandardSerdes_StandardSerdesWins() {
        this.registry.register(new ConfigSerializableOverrideSerializer());
        this.registry.register(new ConfigSerializableSerializer());

        ObjectSerializer<ConfigSerializableType> serializer = this.registry.getSerializer(ConfigSerializableType.class);

        assertThat(serializer).isInstanceOf(ConfigSerializableSerializer.class);
    }

    // === REGISTER FIRST TESTS ===

    @Test
    void testRegisterFirst_SingleSerializer_AddedAtBeginning() {
        this.registry.register(new SecondSerializer());
        this.registry.registerFirst(new FirstSerializer());

        ObjectSerializer<TestType> serializer = this.registry.getSerializer(TestType.class);

        // With reverse iteration, SecondSerializer at end is checked first
        assertThat(serializer).isInstanceOf(SecondSerializer.class);
    }

    @Test
    void testRegisterFirst_MultipleFirst_OrderPreserved() {
        this.registry.register(new ThirdSerializer());
        this.registry.registerFirst(new FirstSerializer());
        this.registry.registerFirst(new SecondSerializer());

        ObjectSerializer<TestType> serializer = this.registry.getSerializer(TestType.class);

        // With reverse iteration: checks Third (index 2), then First (index 1), then Second (index 0)
        assertThat(serializer).isInstanceOf(ThirdSerializer.class);
    }

    @Test
    void testRegisterFirst_BeforeNormalRegister_NormalRegisterWins() {
        this.registry.registerFirst(new FirstSerializer());
        this.registry.register(new SecondSerializer());

        ObjectSerializer<TestType> serializer = this.registry.getSerializer(TestType.class);

        // With reverse iteration, SecondSerializer at end is checked first
        assertThat(serializer).isInstanceOf(SecondSerializer.class);
    }
}
