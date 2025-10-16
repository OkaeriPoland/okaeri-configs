package eu.okaeri.configs.annotations;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Variable;
import eu.okaeri.configs.annotation.VariableMode;
import eu.okaeri.configs.configurer.InMemoryConfigurer;
import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.schema.FieldDeclaration;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for @Variable annotation.
 * 
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
}
