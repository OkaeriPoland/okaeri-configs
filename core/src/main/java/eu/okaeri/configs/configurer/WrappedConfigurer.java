package eu.okaeri.configs.configurer;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.serdes.ConfigPath;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Delegate;

public class WrappedConfigurer extends Configurer {

    /**
     * Methods excluded from delegation - instance-specific state that must be local.
     * <p>
     * These fields represent per-config state that should NOT be shared with parent:
     * <ul>
     *   <li>parent - each config has its own OkaeriConfig reference</li>
     *   <li>basePath - each nested config has its own path context for error reporting</li>
     *   <li>rawContent - each config may have its own content context</li>
     * </ul>
     * <p>
     * The registry IS delegated because serializers should be shared across all configs.
     */
    private interface DelegateExclusions {
        OkaeriConfig getParent();
        void setParent(OkaeriConfig parent);
        ConfigPath getBasePath();
        void setBasePath(ConfigPath path);
        String getRawContent();
        void setRawContent(String content);
    }

    @Getter
    @Delegate(excludes = DelegateExclusions.class)
    private final Configurer wrapped;

    // Local instance state - not delegated to parent

    @Getter
    @Setter
    private OkaeriConfig parent;

    @Getter
    @Setter
    @NonNull
    private ConfigPath basePath = ConfigPath.root();

    @Getter
    @Setter
    private String rawContent;

    public WrappedConfigurer(@NonNull Configurer wrapped) {
        this.wrapped = wrapped;
    }
}
