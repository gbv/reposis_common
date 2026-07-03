package de.gbv.reposis.user.postprocessor;

import java.util.Objects;
import java.util.function.Supplier;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;

/**
 * A {@link MCRStringPostProcessor} that prepends a fixed prefix to the processed value.
 */
@MCRConfigurationProxy(proxyClass = MCRPrefixStringPostProcessor.Factory.class)
public class MCRPrefixStringPostProcessor implements MCRStringPostProcessor {

    private final String prefix;

    /**
     * Creates a new {@code MCRPrefixStringPostProcessor}.
     *
     * @param prefix the prefix to prepend; must not be {@code null}
     */
    public MCRPrefixStringPostProcessor(String prefix) {
        this.prefix = Objects.requireNonNull(prefix, "prefix must not be null");
    }

    @Override
    public String process(String value) {
        return prefix + value;
    }

    /**
     * Factory for creating {@link MCRPrefixStringPostProcessor} instances from configuration properties.
     */
    public static class Factory implements Supplier<MCRPrefixStringPostProcessor> {

        @MCRProperty(name = "Prefix")
        public String prefix;

        @Override
        public MCRPrefixStringPostProcessor get() {
            return new MCRPrefixStringPostProcessor(prefix);
        }
    }
}
