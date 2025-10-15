package eu.okaeri.configs.test.configs;

import eu.okaeri.configs.OkaeriConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Test config for primitive types and their wrappers.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class PrimitivesTestConfig extends OkaeriConfig {

    // Primitives
    private boolean boolValue = true;
    private byte byteValue = 127;
    private char charValue = 'A';
    private double doubleValue = 3.14;
    private float floatValue = 2.71f;
    private int intValue = 42;
    private long longValue = 9999999999L;
    private short shortValue = 999;

    // Wrappers
    private Boolean boolWrapper = false;
    private Byte byteWrapper = 100;
    private Character charWrapper = 'Z';
    private Double doubleWrapper = 2.718;
    private Float floatWrapper = 1.414f;
    private Integer intWrapper = 123;
    private Long longWrapper = 987654321L;
    private Short shortWrapper = 555;
}
