package de.gbv.reposis.mapper.postprocessor;

import java.util.Objects;
import java.util.function.Supplier;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;

/**
 * A {@link MCRPostProcessor} that prepends a configured prefix to an
 * extracted string value.
 */
@MCRConfigurationProxy(proxyClass = MCRStringPrefixPostProcessor.Factory.class)
public class MCRStringPrefixPostProcessor implements MCRPostProcessor<String> {

    private final String prefix;

    /**
     * Creates a new {@code MCRStringPrefixPostProcessor}.
     *
     * @param prefix the prefix to prepend to the extracted value
     */
    public MCRStringPrefixPostProcessor(String prefix) {
        this.prefix = Objects.requireNonNull(prefix, "prefix must not be null");
    }

    @Override
    public String process(String extracted) {
        return prefix + Objects.requireNonNull(extracted, "extracted must not be null");
    }

    /**
     * Factory for creating {@link MCRStringPrefixPostProcessor} instances from configuration properties.
     */
    public static class Factory implements Supplier<MCRStringPrefixPostProcessor> {

        @MCRProperty(name = "Prefix")
        public String prefix;

        @Override
        public MCRStringPrefixPostProcessor get() {
            return new MCRStringPrefixPostProcessor(prefix);
        }
    }
}
