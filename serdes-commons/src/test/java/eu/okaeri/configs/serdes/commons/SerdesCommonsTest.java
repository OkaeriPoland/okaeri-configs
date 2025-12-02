package eu.okaeri.configs.serdes.commons;

import eu.okaeri.configs.serdes.BidirectionalTransformer;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.serdes.SerdesAnnotationResolver;
import eu.okaeri.configs.serdes.SerdesRegistry;
import eu.okaeri.configs.serdes.commons.duration.DurationAttachmentResolver;
import eu.okaeri.configs.serdes.commons.duration.DurationTransformer;
import eu.okaeri.configs.serdes.commons.serializer.InstantSerializer;
import eu.okaeri.configs.serdes.commons.transformer.LocaleTransformer;
import eu.okaeri.configs.serdes.commons.transformer.PatternTransformer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
class SerdesCommonsTest {

    @Test
    void testRegister_AllComponents() {
        SerdesRegistry registry = mock(SerdesRegistry.class);
        SerdesCommons serdesCommons = new SerdesCommons();

        serdesCommons.register(registry);

        // Capture all registered transformers
        ArgumentCaptor<BidirectionalTransformer<?, ?>> transformerCaptor = ArgumentCaptor.forClass(BidirectionalTransformer.class);
        verify(registry, times(3)).register(transformerCaptor.capture());

        List<BidirectionalTransformer<?, ?>> transformers = transformerCaptor.getAllValues();
        assertThat(transformers).hasSize(3);
        assertThat(transformers).anyMatch(t -> t instanceof DurationTransformer);
        assertThat(transformers).anyMatch(t -> t instanceof LocaleTransformer);
        assertThat(transformers).anyMatch(t -> t instanceof PatternTransformer);

        // Verify serializer
        ArgumentCaptor<ObjectSerializer<?>> serializerCaptor = ArgumentCaptor.forClass(ObjectSerializer.class);
        verify(registry).register(serializerCaptor.capture());
        assertThat(serializerCaptor.getValue()).isInstanceOf(InstantSerializer.class);

        // Verify annotation resolver
        ArgumentCaptor<SerdesAnnotationResolver<?, ?>> resolverCaptor = ArgumentCaptor.forClass(SerdesAnnotationResolver.class);
        verify(registry).register(resolverCaptor.capture());
        assertThat(resolverCaptor.getValue()).isInstanceOf(DurationAttachmentResolver.class);
    }
}
