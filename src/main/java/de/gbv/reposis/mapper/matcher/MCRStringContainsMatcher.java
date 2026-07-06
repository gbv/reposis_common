package de.gbv.reposis.mapper.matcher;

import java.util.Objects;
import java.util.function.Supplier;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;

/**
 * A {@link MCRMatcher} that checks whether an extracted string value contains a configured
 * expected substring. A {@code null} value never matches.
 */
@MCRConfigurationProxy(proxyClass = MCRStringContainsMatcher.Factory.class)
public class MCRStringContainsMatcher implements MCRMatcher<String> {

    private final String expectedValue;

    /**
     * Creates a new {@code MCRStringContainsMatcher}.
     *
     * @param expectedValue the substring that must be contained in the extracted value
     */
    public MCRStringContainsMatcher(String expectedValue) {
        this.expectedValue = Objects.requireNonNull(expectedValue, "expectedValue must not be null");
    }

    @Override
    public boolean matches(String extracted) {
        return extracted != null && extracted.contains(expectedValue);
    }

    /**
     * Factory for creating {@link MCRStringContainsMatcher} instances from configuration properties.
     */
    public static class Factory implements Supplier<MCRStringContainsMatcher> {

        @MCRProperty(name = "Value")
        public String value;

        @Override
        public MCRStringContainsMatcher get() {
            return new MCRStringContainsMatcher(value);
        }
    }
}
