package eu.okaeri.configs.serdes.commons.duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DurationAttachmentResolverTest {

    private DurationAttachmentResolver resolver;

    @BeforeEach
    void setUp() {
        this.resolver = new DurationAttachmentResolver();
    }

    @Test
    void testGetAnnotationType() {
        assertThat(this.resolver.getAnnotationType()).isEqualTo(DurationSpec.class);
    }

    @Test
    void testResolveAttachment_FieldLevel() throws NoSuchFieldException {
        Field field = TestConfig.class.getDeclaredField("customDuration");
        DurationSpec annotation = field.getAnnotation(DurationSpec.class);

        Optional<DurationSpecData> result = this.resolver.resolveAttachment(field, annotation);

        assertThat(result).isPresent();
        assertThat(result.get().getFallbackUnit()).isEqualTo(ChronoUnit.MINUTES);
        assertThat(result.get().getFormat()).isEqualTo(DurationFormat.ISO);
    }

    @Test
    void testResolveAttachment_DefaultValues() throws NoSuchFieldException {
        Field field = TestConfig.class.getDeclaredField("defaultDuration");
        DurationSpec annotation = field.getAnnotation(DurationSpec.class);

        Optional<DurationSpecData> result = this.resolver.resolveAttachment(field, annotation);

        assertThat(result).isPresent();
        assertThat(result.get().getFallbackUnit()).isEqualTo(ChronoUnit.SECONDS);
        assertThat(result.get().getFormat()).isEqualTo(DurationFormat.SIMPLIFIED);
    }

    @Test
    void testResolveClassAttachment() {
        DurationSpec annotation = ClassLevelConfig.class.getAnnotation(DurationSpec.class);

        Optional<DurationSpecData> result = this.resolver.resolveClassAttachment(ClassLevelConfig.class, annotation);

        assertThat(result).isPresent();
        assertThat(result.get().getFallbackUnit()).isEqualTo(ChronoUnit.HOURS);
        assertThat(result.get().getFormat()).isEqualTo(DurationFormat.ISO);
    }

    // ==================== Test Config Classes ====================

    static class TestConfig {
        @DurationSpec(fallbackUnit = ChronoUnit.MINUTES, format = DurationFormat.ISO)
        private Duration customDuration;

        @DurationSpec
        private Duration defaultDuration;
    }

    @DurationSpec(fallbackUnit = ChronoUnit.HOURS, format = DurationFormat.ISO)
    static class ClassLevelConfig {
        private Duration duration;
    }
}
