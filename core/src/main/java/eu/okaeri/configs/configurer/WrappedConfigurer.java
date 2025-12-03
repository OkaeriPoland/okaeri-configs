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

    protected String rawContent;

    public WrappedConfigurer(@NonNull Configurer wrapped) {
        this.wrapped = wrapped;
    }

    /**
     * Sets the raw content for this configurer and propagates it to the wrapped configurer.
     * <p>
     * This propagation is necessary because format-specific methods like createSourceWalker()
     * are delegated to the wrapped configurer, and they call getRawContent() on themselves
     * (not on the wrapper). Without propagation, the wrapped configurer would have null rawContent.
     *
     * @param rawContent the raw content to set
     */
    public void setRawContent(String rawContent) {
        this.rawContent = rawContent;
        this.wrapped.setRawContent(rawContent);
    }

    /**
     * Gets the raw content for this configurer.
     * <p>
     * This method is overridden to fix a field shadowing bug: the parent class (Configurer)
     * has a rawContent field with getter, and this class has its own rawContent field with
     * setter only. Without this override, getRawContent() would read the parent's field
     * (always null) while setRawContent() writes to this class's field.
     * <p>
     * If this configurer doesn't have its own rawContent, delegates to wrapped configurer.
     * This allows subconfigs to access parent's rawContent for error reporting.
     *
     * @return the raw content from this configurer or from the wrapped configurer chain
     */
    @Override
    public String getRawContent() {
        if (this.rawContent != null) {
            return this.rawContent;
        }
        return this.wrapped.getRawContent();
    }
}
