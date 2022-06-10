package eu.okaeri.configs.test.old.impl;

import eu.okaeri.configs.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Header("################################################################")
@Header("#                                                              #")
@Header("#                              hi                              #")
@Header("#                                                              #")
@Header("################################################################")
@Names(strategy = NameStrategy.SNAKE_CASE, modifier = NameModifier.TO_LOWER_CASE)
public class TestConfig extends TestConfigParent {

    @Comment("Hi. I'm new here!")
    private String newProp = "right";

}
