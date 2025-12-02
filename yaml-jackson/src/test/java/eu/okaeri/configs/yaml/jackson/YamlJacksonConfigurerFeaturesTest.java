package eu.okaeri.configs.yaml.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests YamlJacksonConfigurer-specific features.
 * Only tests for backend-specific functionality not covered by parameterized tests.
 */
class YamlJacksonConfigurerFeaturesTest {

    // ==================== Constructor Tests ====================

    @Test
    void testDefaultConstructor() {
        YamlJacksonConfigurer configurer = new YamlJacksonConfigurer();
        assertThat(configurer).isNotNull();
    }

    @Test
    void testConstructorWithMapper() {
        ObjectMapper mapper = new YAMLMapper();
        YamlJacksonConfigurer configurer = new YamlJacksonConfigurer(mapper);
        assertThat(configurer).isNotNull();
    }

    @Test
    void testConstructorWithMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("key", "value");
        YamlJacksonConfigurer configurer = new YamlJacksonConfigurer(map);
        assertThat(configurer).isNotNull();
        assertThat(configurer.getValue("key")).isEqualTo("value");
    }

    // ==================== getExtensions Tests ====================

    @Test
    void testGetExtensions() {
        YamlJacksonConfigurer configurer = new YamlJacksonConfigurer();
        assertThat(configurer.getExtensions()).containsExactly("yml", "yaml");
    }
}
