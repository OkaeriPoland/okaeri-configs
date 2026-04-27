package eu.okaeri.configs.schema;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that static fields are excluded from config declarations.
 * Static fields are class-level state, not per-instance config state, and including
 * them would produce confusing serialized output (and crash for non-trivial types).
 *
 * <p>Historically only {@code serialVersionUID} was filtered (by name). Now any static
 * field is filtered, which also fixes Kotlin companion object compatibility — see
 * {@code core-test-kotlin}.
 */
class StaticFieldFilterTest {

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class StaticFieldConfig extends OkaeriConfig {
        private static final String CONSTANT = "fixed";
        private static String mutableStatic = "shared";
        private String instanceField = "instance";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class SerialVersionUIDConfig extends OkaeriConfig {
        private static final long serialVersionUID = 1L;
        private String name = "value";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class OnlyStaticFieldsConfig extends OkaeriConfig {
        private static final int A = 1;
        private static final int B = 2;
    }

    @Test
    void staticFinalFieldExcluded() {
        StaticFieldConfig config = ConfigManager.create(StaticFieldConfig.class);
        assertThat(config.getDeclaration().getField("CONSTANT").orElse(null)).isNull();
    }

    @Test
    void staticMutableFieldExcluded() {
        StaticFieldConfig config = ConfigManager.create(StaticFieldConfig.class);
        assertThat(config.getDeclaration().getField("mutableStatic").orElse(null)).isNull();
    }

    @Test
    void instanceFieldStillIncluded() {
        StaticFieldConfig config = ConfigManager.create(StaticFieldConfig.class);
        assertThat(config.getDeclaration().getField("instanceField").isPresent()).isTrue();
    }

    @Test
    void onlyInstanceFieldsAppearInDeclaration() {
        StaticFieldConfig config = ConfigManager.create(StaticFieldConfig.class);
        assertThat(config.getDeclaration().getFields())
            .extracting(FieldDeclaration::getName)
            .containsExactly("instanceField");
    }

    @Test
    void serialVersionUIDExcluded() {
        SerialVersionUIDConfig config = ConfigManager.create(SerialVersionUIDConfig.class);
        assertThat(config.getDeclaration().getField("serialVersionUID").orElse(null)).isNull();
        assertThat(config.getDeclaration().getField("name").isPresent()).isTrue();
    }

    @Test
    void onlyStaticFieldsProducesEmptyDeclaration() {
        OnlyStaticFieldsConfig config = ConfigManager.create(OnlyStaticFieldsConfig.class);
        assertThat(config.getDeclaration().getFields()).isEmpty();
    }
}
