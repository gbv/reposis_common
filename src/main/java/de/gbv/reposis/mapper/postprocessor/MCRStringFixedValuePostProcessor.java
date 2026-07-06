package de.gbv.reposis.mapper.postprocessor;

import java.util.Objects;
import java.util.function.Supplier;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;

/**
 * A {@link MCRPostProcessor} that ignores the extracted value and always returns a
 * fixed, configured result.
 */
@MCRConfigurationProxy(proxyClass = MCRStringFixedValuePostProcessor.Factory.class)
public class MCRStringFixedValuePostProcessor implements MCRPostProcessor<String> {

    private final String value;

    /**
     * Creates a new {@code MCRStringFixedValuePostProcessor}.
     *
     * @param value the fixed value to return for every match
     */
    public MCRStringFixedValuePostProcessor(String value) {
        this.value = Objects.requireNonNull(value, "value must not be null");
    }

    @Override
    public String process(String ignored) {
        return value;
    }

    /**
     * Factory for creating {@link MCRStringFixedValuePostProcessor} instances from
     * configuration properties.
     */
    public static class Factory implements Supplier<MCRStringFixedValuePostProcessor> {

        @MCRProperty(name = "Value")
        public String value;

        @Override
        public MCRStringFixedValuePostProcessor get() {
            return new MCRStringFixedValuePostProcessor(value);
        }
    }
}
