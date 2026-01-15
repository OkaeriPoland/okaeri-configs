package eu.okaeri.configs.serdes;

import lombok.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Chains multiple {@link ValuePreProcessor} instances, processing values in order.
 * <p>
 * Each processor receives the output of the previous one. The final result
 * reflects whether any processor modified the value and whether the result
 * should be written to file.
 * <p>
 * <b>Write-to-file behavior:</b> If ANY processor in the chain returns
 * {@link PreProcessResult#transformed(Object)}, the final result will also
 * be marked for writing to file. This ensures persistent transformations
 * are not lost when combined with runtime-only processors.
 * <p>
 * Example usage:
 * <pre>{@code
 * opt.valuePreProcessor(
 *     new EnvironmentPlaceholderProcessor(),
 *     new MyCustomProcessor()
 * );
 * }</pre>
 *
 * @see ValuePreProcessor
 * @see PreProcessResult
 */
public class ChainedPreProcessor implements ValuePreProcessor {

    private final List<ValuePreProcessor> processors;

    /**
     * Creates a chained processor from the given processors.
     *
     * @param processors the processors to chain, in order
     */
    public ChainedPreProcessor(@NonNull ValuePreProcessor... processors) {
        this.processors = Arrays.asList(processors);
    }

    /**
     * Creates a chained processor from the given list of processors.
     *
     * @param processors the processors to chain, in order
     */
    public ChainedPreProcessor(@NonNull List<ValuePreProcessor> processors) {
        this.processors = new ArrayList<>(processors);
    }

    @Override
    public PreProcessResult process(Object value, @NonNull SerdesContext context) {
        Object currentValue = value;
        boolean anyModified = false;
        boolean writeToFile = false;

        for (ValuePreProcessor processor : this.processors) {
            PreProcessResult result = processor.process(currentValue, context);
            if (result.isModified()) {
                currentValue = result.getValue();
                anyModified = true;
                if (result.isWriteToFile()) {
                    writeToFile = true;
                }
            }
        }

        if (!anyModified) {
            return PreProcessResult.noop();
        }

        return writeToFile
            ? PreProcessResult.transformed(currentValue)
            : PreProcessResult.runtimeOnly(currentValue);
    }
}
