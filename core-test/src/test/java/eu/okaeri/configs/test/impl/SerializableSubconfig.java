package eu.okaeri.configs.test.impl;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.NameModifier;
import eu.okaeri.configs.annotation.NameStrategy;
import eu.okaeri.configs.annotation.Names;
import eu.okaeri.configs.annotation.Variable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@ToString
@AllArgsConstructor
@Names(strategy = NameStrategy.SNAKE_CASE, modifier = NameModifier.TO_UPPER_CASE)
public class SerializableSubconfig extends OkaeriConfig {
    @Variable("APP_TEST_STRING")
    private String testString;
    private Boolean testBool;
    private List<String> listOfStrings;
}
