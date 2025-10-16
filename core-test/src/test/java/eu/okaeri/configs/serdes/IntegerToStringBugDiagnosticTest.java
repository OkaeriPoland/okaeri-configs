package eu.okaeri.configs.serdes;

import eu.okaeri.configs.configurer.InMemoryConfigurer;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.standard.StandardSerdes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Diagnostic tests to investigate the Integer → String conversion bug.
 * 
 * Bug Description:
 * ConfigGetSetTest.testGet_WithClass_TypeConversion fails because
 * Integer → String conversion doesn't work, even though it should be
 * registered via registerWithReversedToString() in StandardSerdes.
 * 
 * Hypothesis:
 * The problem is in SerdesRegistry.registerWithReversedToString(), which
 * creates a new ObjectToStringTransformer() with pair (Object → String).
 * When looking up Integer → String, the exact pair match fails because
 * Integer ≠ Object in GenericsPair equality.
 */
class IntegerToStringBugDiagnosticTest {

    private SerdesRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new SerdesRegistry();
        new StandardSerdes().register(registry);
    }

    // ========================================
    // Test 1: Check transformer registration
    // ========================================

    @Test
    void diagnostic_StringToInteger_IsRegistered() {
        // This should work - String → Integer transformer is explicitly registered
        GenericsDeclaration from = GenericsDeclaration.of(String.class);
        GenericsDeclaration to = GenericsDeclaration.of(Integer.class);
        
        ObjectTransformer transformer = registry.getTransformer(from, to);
        
        assertThat(transformer)
            .withFailMessage("String → Integer transformer should be registered")
            .isNotNull();
    }

    @Test
    void diagnostic_IntegerToString_IsRegistered() {
        // BUG: This fails! Integer → String is NOT found
        GenericsDeclaration from = GenericsDeclaration.of(Integer.class);
        GenericsDeclaration to = GenericsDeclaration.of(String.class);
        
        ObjectTransformer transformer = registry.getTransformer(from, to);
        
        assertThat(transformer)
            .withFailMessage("Integer → String transformer should be registered by registerWithReversedToString()")
            .isNotNull();
    }

    @Test
    void diagnostic_ObjectToString_IsRegistered() {
        // This should work - ObjectToStringTransformer is explicitly registered
        GenericsDeclaration from = GenericsDeclaration.of(Object.class);
        GenericsDeclaration to = GenericsDeclaration.of(String.class);
        
        ObjectTransformer transformer = registry.getTransformer(from, to);
        
        assertThat(transformer)
            .withFailMessage("Object → String transformer should be registered")
            .isNotNull();
    }

    // ========================================
    // Test 2: Check canTransform()
    // ========================================

    @Test
    void diagnostic_CanTransform_StringToInteger() {
        GenericsDeclaration from = GenericsDeclaration.of(String.class);
        GenericsDeclaration to = GenericsDeclaration.of(Integer.class);
        
        boolean canTransform = registry.canTransform(from, to);
        
        assertThat(canTransform)
            .withFailMessage("Should be able to transform String → Integer")
            .isTrue();
    }

    @Test
    void diagnostic_CanTransform_IntegerToString() {
        GenericsDeclaration from = GenericsDeclaration.of(Integer.class);
        GenericsDeclaration to = GenericsDeclaration.of(String.class);
        
        boolean canTransform = registry.canTransform(from, to);
        
        assertThat(canTransform)
            .withFailMessage("Should be able to transform Integer → String (BUG: returns false)")
            .isTrue();
    }

    // ========================================
    // Test 3: List transformers from Integer
    // ========================================

    @Test
    void diagnostic_ListTransformersFrom_Integer() {
        GenericsDeclaration from = GenericsDeclaration.of(Integer.class);
        
        var transformers = registry.getTransformersFrom(from);
        
        System.out.println("=== Transformers FROM Integer ===");
        System.out.println("Count: " + transformers.size());
        transformers.forEach(t -> {
            System.out.println("  - " + t.getClass().getSimpleName() + ": " + t.getPair());
        });
        
        assertThat(transformers)
            .withFailMessage("Expected at least Integer → String transformer")
            .isNotEmpty();
    }

    @Test
    void diagnostic_ListTransformersTo_String() {
        GenericsDeclaration to = GenericsDeclaration.of(String.class);
        
        var transformers = registry.getTransformersTo(to);
        
        System.out.println("=== Transformers TO String ===");
        System.out.println("Count: " + transformers.size());
        transformers.forEach(t -> {
            System.out.println("  - " + t.getClass().getSimpleName() + ": " + t.getPair());
        });
        
        assertThat(transformers)
            .withFailMessage("Expected multiple transformers to String (including Integer → String)")
            .hasSizeGreaterThan(1);
    }

    // ========================================
    // Test 4: Check other numeric types
    // ========================================

    @Test
    void diagnostic_LongToString_IsRegistered() {
        GenericsDeclaration from = GenericsDeclaration.of(Long.class);
        GenericsDeclaration to = GenericsDeclaration.of(String.class);
        
        ObjectTransformer transformer = registry.getTransformer(from, to);
        
        assertThat(transformer)
            .withFailMessage("Long → String should also be affected by the same bug")
            .isNotNull();
    }

    @Test
    void diagnostic_DoubleToString_IsRegistered() {
        GenericsDeclaration from = GenericsDeclaration.of(Double.class);
        GenericsDeclaration to = GenericsDeclaration.of(String.class);
        
        ObjectTransformer transformer = registry.getTransformer(from, to);
        
        assertThat(transformer)
            .withFailMessage("Double → String should also be affected by the same bug")
            .isNotNull();
    }

    @Test
    void diagnostic_BooleanToString_IsRegistered() {
        GenericsDeclaration from = GenericsDeclaration.of(Boolean.class);
        GenericsDeclaration to = GenericsDeclaration.of(String.class);
        
        ObjectTransformer transformer = registry.getTransformer(from, to);
        
        assertThat(transformer)
            .withFailMessage("Boolean → String should also be affected by the same bug")
            .isNotNull();
    }

    // ========================================
    // Test 5: Actual transformation tests
    // ========================================

    @Test
    void diagnostic_ActualTransform_IntegerToString() throws Exception {
        GenericsDeclaration from = GenericsDeclaration.of(Integer.class);
        GenericsDeclaration to = GenericsDeclaration.of(String.class);
        
        ObjectTransformer transformer = registry.getTransformer(from, to);
        
        if (transformer != null) {
            SerdesContext context = SerdesContext.of(new InMemoryConfigurer());
            String result = (String) transformer.transform(123, context);
            
            assertThat(result).isEqualTo("123");
        } else {
            System.out.println("CONFIRMED BUG: Integer → String transformer is NULL");
        }
    }

    // ========================================
    // Test 6: GenericsPair equality test
    // ========================================

    @Test
    void diagnostic_GenericsPairEquality_IntegerVsObject() {
        GenericsDeclaration integer = GenericsDeclaration.of(Integer.class);
        GenericsDeclaration object = GenericsDeclaration.of(Object.class);
        GenericsDeclaration string = GenericsDeclaration.of(String.class);

        // Test if Integer and Object are considered equal in GenericsPair
        boolean integerEqualsObject = integer.equals(object);

        System.out.println("=== GenericsPair Equality ===");
        System.out.println("Integer.equals(Object): " + integerEqualsObject);
        System.out.println("Integer: " + integer);
        System.out.println("Object: " + object);

//        assertThat(integerEqualsObject)
//            .withFailMessage("This shows why Integer → String lookup fails: Integer ≠ Object")
//            .isTrue(); // This will FAIL, proving the hypothesis
    }

    @Test
    void diagnostic_PrintAllRegisteredTransformers() {
        System.out.println("\n=== ALL REGISTERED TRANSFORMERS ===");
        
        // Get all transformers by checking common target types
        String[] targetTypes = {"String", "Integer", "Long", "Double", "Boolean", "Object"};
        
        for (String typeName : targetTypes) {
            try {
                Class<?> clazz = Class.forName("java.lang." + typeName);
                GenericsDeclaration to = GenericsDeclaration.of(clazz);
                var transformers = registry.getTransformersTo(to);
                
                System.out.println("\nTransformers TO " + typeName + " (" + transformers.size() + "):");
                transformers.forEach(t -> {
                    System.out.println("  " + t.getPair().getFrom().getType().getSimpleName() + 
                                     " → " + t.getPair().getTo().getType().getSimpleName() +
                                     " [" + t.getClass().getSimpleName() + "]");
                });
            } catch (ClassNotFoundException e) {
                // Ignore
            }
        }
    }
}
