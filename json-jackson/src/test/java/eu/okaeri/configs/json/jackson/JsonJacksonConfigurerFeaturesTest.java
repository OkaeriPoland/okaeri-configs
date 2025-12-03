package eu.okaeri.configs.json.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests JsonJacksonConfigurer-specific features.
 * Only tests for backend-specific functionality not covered by parameterized tests.
 */
class JsonJacksonConfigurerFeaturesTest {

    // ==================== Constructor Tests ====================

    @Test
    void testDefaultConstructor() {
        JsonJacksonConfigurer configurer = new JsonJacksonConfigurer();
        assertThat(configurer).isNotNull();
    }

    @Test
    void testConstructorWithMapper() {
        ObjectMapper mapper = new ObjectMapper();
        JsonJacksonConfigurer configurer = new JsonJacksonConfigurer(mapper);
        assertThat(configurer).isNotNull();
    }


    // ==================== getExtensions Tests ====================

    @Test
    void testGetExtensions() {
        JsonJacksonConfigurer configurer = new JsonJacksonConfigurer();
        assertThat(configurer.getExtensions()).containsExactly("json");
    }
}
