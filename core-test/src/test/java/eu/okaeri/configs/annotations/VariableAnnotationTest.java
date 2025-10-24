package eu.okaeri.configs.annotations;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Variable;
import eu.okaeri.configs.annotation.VariableMode;
import eu.okaeri.configs.configurer.InMemoryConfigurer;
import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.schema.FieldDeclaration;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.Serializable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for @Variable annotation.
 * <p>
 * Verifies:
 * - Load from system property
 * - Load from environment variable
 * - System property takes precedence over env var
 * - Fallback to config value when variable not set
 * - Variable value is captured in declaration
 * - Variable with type conversion (String → Integer)
 * - Variable mode (RUNTIME vs WRITE)
 * - Multiple variables in same config
 */
class VariableAnnotationTest {

    @AfterEach
    void cleanup() {
        // Clean up system properties after each test
        System.clearProperty("TEST_VAR");
        System.clearProperty("TEST_INT_VAR");
        System.clearProperty("TEST_VAR_1");
        System.clearProperty("TEST_VAR_2");
        System.clearProperty("PRECEDENCE_TEST");
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class SimpleVariableConfig extends OkaeriConfig {
        @Variable("TEST_VAR")
        private String variableField = "default value";

        private String normalField = "normal value";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class TypeConversionVariableConfig extends OkaeriConfig {
        @Variable("TEST_INT_VAR")
        private int intVariable = 42;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class MultipleVariablesConfig extends OkaeriConfig {
        @Variable("TEST_VAR_1")
        private String var1 = "default1";

        @Variable("TEST_VAR_2")
        private String var2 = "default2";

        private String normalField = "normal";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class VariableModeConfig extends OkaeriConfig {
        @Variable(value = "RUNTIME_VAR", mode = VariableMode.RUNTIME)
        private String runtimeVar = "runtime default";

        @Variable(value = "WRITE_VAR", mode = VariableMode.WRITE)
        private String writeVar = "write default";
    }

    // Tests

    @Test
    void testVariable_InDeclaration() {
        // Given
        SimpleVariableConfig config = ConfigManager.create(SimpleVariableConfig.class);

        // When
        ConfigDeclaration declaration = config.getDeclaration();
        FieldDeclaration field = declaration.getField("variableField").orElse(null);

        // Then
        assertThat(field).isNotNull();
        assertThat(field.getVariable()).isNotNull();
        assertThat(field.getVariable().value()).isEqualTo("TEST_VAR");
        assertThat(field.getVariable().mode()).isEqualTo(VariableMode.RUNTIME);
    }

    @Test
    void testVariable_FromSystemProperty_LoadedOnUpdate() {
        // Given
        System.setProperty("TEST_VAR", "from system property");
        SimpleVariableConfig config = ConfigManager.create(SimpleVariableConfig.class);
        config.withConfigurer(new InMemoryConfigurer());

        // When
        config.update();

        // Then
        assertThat(config.getVariableField()).isEqualTo("from system property");
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class EnvVarConfig extends OkaeriConfig {
        @Variable("PATH")
        private String pathVar = "default";
    }

    @Test
    void testVariable_FromEnvironmentVariable_LoadedOnUpdate() {
        // Given - Assuming PATH exists in environment variables
        // We can't easily set env vars in tests, so we'll use an existing one
        EnvVarConfig config = ConfigManager.create(EnvVarConfig.class);
        config.withConfigurer(new InMemoryConfigurer());

        // When
        config.update();

        // Then - PATH should exist in environment
        String pathEnv = System.getenv("PATH");
        if (pathEnv != null) {
            assertThat(config.getPathVar()).isEqualTo(pathEnv);
        }
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class PrecedenceConfig extends OkaeriConfig {
        @Variable("PATH")
        private String pathVar = "default";
    }

    @Test
    void testVariable_SystemPropertyPrecedence_OverEnvVar() {
        // Given - Set both system property and assume PATH env var exists
        System.setProperty("PATH", "system property wins");

        try {
            // When
            PrecedenceConfig config = ConfigManager.create(PrecedenceConfig.class);
            config.withConfigurer(new InMemoryConfigurer());
            config.update();

            // Then
            assertThat(config.getPathVar()).isEqualTo("system property wins");
        } finally {
            System.clearProperty("PATH");
        }
    }

    @Test
    void testVariable_NotSet_FallsBackToDefault() {
        // Given - No TEST_VAR set
        SimpleVariableConfig config = ConfigManager.create(SimpleVariableConfig.class);
        config.withConfigurer(new InMemoryConfigurer());

        // When
        config.update();

        // Then
        assertThat(config.getVariableField()).isEqualTo("default value");
    }

    @Test
    void testVariable_TypeConversion_StringToInt() {
        // Given
        System.setProperty("TEST_INT_VAR", "999");
        TypeConversionVariableConfig config = ConfigManager.create(TypeConversionVariableConfig.class);
        config.withConfigurer(new InMemoryConfigurer());

        // When
        config.update();

        // Then
        assertThat(config.getIntVariable()).isEqualTo(999);
    }

    @Test
    void testVariable_MultipleVariables_AllLoaded() {
        // Given
        System.setProperty("TEST_VAR_1", "loaded1");
        System.setProperty("TEST_VAR_2", "loaded2");
        MultipleVariablesConfig config = ConfigManager.create(MultipleVariablesConfig.class);
        config.withConfigurer(new InMemoryConfigurer());

        // When
        config.update();

        // Then
        assertThat(config.getVar1()).isEqualTo("loaded1");
        assertThat(config.getVar2()).isEqualTo("loaded2");
        assertThat(config.getNormalField()).isEqualTo("normal");
    }

    @Test
    void testVariable_NormalField_UnaffectedByUpdate() {
        // Given
        SimpleVariableConfig config = ConfigManager.create(SimpleVariableConfig.class);
        config.withConfigurer(new InMemoryConfigurer());
        config.setNormalField("modified");

        // When
        config.update();

        // Then
        assertThat(config.getNormalField()).isEqualTo("modified");
    }

    @Test
    void testVariable_Mode_RUNTIME() {
        // Given
        VariableModeConfig config = ConfigManager.create(VariableModeConfig.class);
        ConfigDeclaration declaration = config.getDeclaration();

        // When
        FieldDeclaration field = declaration.getField("runtimeVar").orElse(null);

        // Then
        assertThat(field).isNotNull();
        assertThat(field.getVariable()).isNotNull();
        assertThat(field.getVariable().mode()).isEqualTo(VariableMode.RUNTIME);
    }

    @Test
    void testVariable_Mode_WRITE() {
        // Given
        VariableModeConfig config = ConfigManager.create(VariableModeConfig.class);
        ConfigDeclaration declaration = config.getDeclaration();

        // When
        FieldDeclaration field = declaration.getField("writeVar").orElse(null);

        // Then
        assertThat(field).isNotNull();
        assertThat(field.getVariable()).isNotNull();
        assertThat(field.getVariable().mode()).isEqualTo(VariableMode.WRITE);
    }

    @Test
    void testVariable_UpdateCalledMultipleTimes_AlwaysReloads() {
        // Given
        SimpleVariableConfig config = ConfigManager.create(SimpleVariableConfig.class);
        config.withConfigurer(new InMemoryConfigurer());

        // When - Update with no system property
        config.update();
        String firstValue = config.getVariableField();

        // Then
        assertThat(firstValue).isEqualTo("default value");

        // When - Set system property and update again
        System.setProperty("TEST_VAR", "updated value");
        config.update();

        // Then
        assertThat(config.getVariableField()).isEqualTo("updated value");
    }

    @Test
    void testVariable_EmptySystemProperty_LoadsEmptyString() {
        // Given
        System.setProperty("TEST_VAR", "");
        SimpleVariableConfig config = ConfigManager.create(SimpleVariableConfig.class);
        config.withConfigurer(new InMemoryConfigurer());

        // When
        config.update();

        // Then
        assertThat(config.getVariableField()).isEmpty();
    }

    // Subconfig tests

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class SubconfigWithVariable extends OkaeriConfig {
        @Variable("NESTED_VAR")
        private String nestedVar = "nested default";

        private String normalNestedField = "normal nested";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class ConfigWithNestedVariable extends OkaeriConfig {
        private String topLevelField = "top level";

        private SubconfigWithVariable nested = new SubconfigWithVariable();
    }

    @Test
    void testVariable_InOkaeriConfigSubconfig_LoadedOnUpdate() {
        // Given
        System.setProperty("NESTED_VAR", "from system in nested");
        ConfigWithNestedVariable config = ConfigManager.create(ConfigWithNestedVariable.class);
        config.withConfigurer(new InMemoryConfigurer());

        // When
        config.update();

        // Then - Variable in nested config is loaded
        assertThat(config.getNested().getNestedVar()).isEqualTo("from system in nested");
        assertThat(config.getNested().getNormalNestedField()).isEqualTo("normal nested");
        assertThat(config.getTopLevelField()).isEqualTo("top level");

        // Cleanup
        System.clearProperty("NESTED_VAR");
    }

    @Test
    void testVariable_InOkaeriConfigSubconfig_FallsBackToDefault() {
        // Given - No NESTED_VAR set
        ConfigWithNestedVariable config = ConfigManager.create(ConfigWithNestedVariable.class);
        config.withConfigurer(new InMemoryConfigurer());

        // When
        config.update();

        // Then - Falls back to default
        assertThat(config.getNested().getNestedVar()).isEqualTo("nested default");
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SerializableWithVariable implements Serializable {
        private static final long serialVersionUID = 1L;

        @Variable("SERIALIZABLE_VAR")
        private String serializableVar = "serializable default";

        private String normalSerializableField = "normal serializable";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class ConfigWithSerializableVariable extends OkaeriConfig {
        private String topLevelField = "top level";

        private SerializableWithVariable serializable = new SerializableWithVariable();
    }

    @Test
    @Disabled("@Variable in Serializable can cause infinite recursion")
    void testVariable_InSerializableSubconfig_LoadedOnUpdate() {
        // Given
        System.setProperty("SERIALIZABLE_VAR", "from system in serializable");
        ConfigWithSerializableVariable config = ConfigManager.create(ConfigWithSerializableVariable.class);
        config.withConfigurer(new InMemoryConfigurer());

        // When
        config.update();

        // Then - Variable in serializable is loaded
        assertThat(config.getSerializable().getSerializableVar()).isEqualTo("from system in serializable");
        assertThat(config.getSerializable().getNormalSerializableField()).isEqualTo("normal serializable");

        // Cleanup
        System.clearProperty("SERIALIZABLE_VAR");
    }

    @Test
    @Disabled("@Variable in Serializable can cause infinite recursion")
    void testVariable_InSerializableSubconfig_FallsBackToDefault() {
        // Given - No SERIALIZABLE_VAR set
        ConfigWithSerializableVariable config = ConfigManager.create(ConfigWithSerializableVariable.class);
        config.withConfigurer(new InMemoryConfigurer());

        // When
        config.update();

        // Then - Falls back to default
        assertThat(config.getSerializable().getSerializableVar()).isEqualTo("serializable default");
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class Level2Subconfig extends OkaeriConfig {
        @Variable("LEVEL2_VAR")
        private String level2Var = "level2 default";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class Level1Subconfig extends OkaeriConfig {
        @Variable("LEVEL1_VAR")
        private String level1Var = "level1 default";

        private Level2Subconfig level2 = new Level2Subconfig();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class DeepNestedVariableConfig extends OkaeriConfig {
        @Variable("ROOT_VAR")
        private String rootVar = "root default";

        private Level1Subconfig level1 = new Level1Subconfig();
    }

    @Test
    void testVariable_InDeeplyNestedSubconfigs_AllLevelsLoaded() {
        // Given
        System.setProperty("ROOT_VAR", "root from system");
        System.setProperty("LEVEL1_VAR", "level1 from system");
        System.setProperty("LEVEL2_VAR", "level2 from system");

        DeepNestedVariableConfig config = ConfigManager.create(DeepNestedVariableConfig.class);
        config.withConfigurer(new InMemoryConfigurer());

        // When
        config.update();

        // Then - All levels are updated
        assertThat(config.getRootVar()).isEqualTo("root from system");
        assertThat(config.getLevel1().getLevel1Var()).isEqualTo("level1 from system");
        assertThat(config.getLevel1().getLevel2().getLevel2Var()).isEqualTo("level2 from system");

        // Cleanup
        System.clearProperty("ROOT_VAR");
        System.clearProperty("LEVEL1_VAR");
        System.clearProperty("LEVEL2_VAR");
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class MixedNestedConfig extends OkaeriConfig {
        @Variable("TOP_VAR")
        private String topVar = "top default";

        private SubconfigWithVariable okaeriNested = new SubconfigWithVariable();
        private SerializableWithVariable serializableNested = new SerializableWithVariable();
    }

    @Test
    @Disabled("@Variable in Serializable can cause infinite recursion")
    void testVariable_InMixedNestedTypes_BothTypesWork() {
        // Given
        System.setProperty("TOP_VAR", "top from system");
        System.setProperty("NESTED_VAR", "okaeri nested from system");
        System.setProperty("SERIALIZABLE_VAR", "serializable nested from system");

        MixedNestedConfig config = ConfigManager.create(MixedNestedConfig.class);
        config.withConfigurer(new InMemoryConfigurer());

        // When
        config.update();

        // Then - All variables are loaded
        assertThat(config.getTopVar()).isEqualTo("top from system");
        assertThat(config.getOkaeriNested().getNestedVar()).isEqualTo("okaeri nested from system");
        assertThat(config.getSerializableNested().getSerializableVar()).isEqualTo("serializable nested from system");

        // Cleanup
        System.clearProperty("TOP_VAR");
        System.clearProperty("NESTED_VAR");
        System.clearProperty("SERIALIZABLE_VAR");
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class SelfRefVariableConfig extends OkaeriConfig {
        @Variable("SELF_REF_VAR")
        private String varField = "default";

        private String normalField = "normal";
        private SelfRefVariableConfig child = null;
    }

    /**
     * Self-referencing config with @Variable
     * Regression test: ensures processVariablesRecursively doesn't infinite loop on circular references
     */
    @Test
    void testVariable_SelfReferencingConfig_DoesNotInfiniteLoop() {

        // Given - Create self-referencing config
        System.setProperty("SELF_REF_VAR", "from system");
        SelfRefVariableConfig config = ConfigManager.create(SelfRefVariableConfig.class);
        config.withConfigurer(new InMemoryConfigurer());

        // When - This should not infinite loop
        config.update();

        // Then - Variable is loaded
        assertThat(config.getVarField()).isEqualTo("from system");
        assertThat(config.getNormalField()).isEqualTo("normal");

        // Cleanup
        System.clearProperty("SELF_REF_VAR");
    }
}
