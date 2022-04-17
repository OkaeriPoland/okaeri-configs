package eu.okaeri.configs.configurer;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Delegate;

@AllArgsConstructor
public class WrappedConfigurer extends Configurer {

    @Getter
    @Delegate
    private final Configurer wrapped;
}
