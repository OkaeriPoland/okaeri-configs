package eu.okaeri.configs.test.configs;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Test config for various annotations.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Header("Test Header Line 1")
@Header("Test Header Line 2")
public class AnnotationsTestConfig extends OkaeriConfig {

    @Comment("This is a simple comment")
    private String commentedField = "value";

    @Comment({"Multi-line comment", "Line 2"})
    private String multiCommentField = "value2";

    @CustomKey("custom-key-name")
    private String customKeyField = "custom value";

    @Variable("TEST_VAR")
    private String variableField = "default";

    @Exclude
    private String excludedField = "should not serialize";

    private String normalField = "normal";
}
