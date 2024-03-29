package eu.okaeri.configs.test.old.impl;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Names(strategy = NameStrategy.SNAKE_CASE, modifier = NameModifier.TO_UPPER_CASE)
public class SerializableSubconfig extends OkaeriConfig {
    @Comment("Default value")
    @Comment("multiline subcomment")
    private final String defaultValue = "some value";
    @Variable("APP_TEST_STRING")
    private String testString;
    @Comment("siema")
    private Boolean testBool;
    @Comment("siema")
    private Boolean testBool2 = true;
    @Comment("lista bk")
    private List<String> listOfStrings;
    @Comment("hmmm enum")
    private TestEnum testEnum;
}
