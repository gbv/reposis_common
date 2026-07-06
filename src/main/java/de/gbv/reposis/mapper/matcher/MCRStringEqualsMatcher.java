package de.gbv.reposis.mapper.matcher;

import java.util.Objects;
import java.util.function.Supplier;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;

/**
 * A {@link MCRMatcher} that checks whether an extracted string value equals a configured
 * expected value. A {@code null} value never matches.
 */
@MCRConfigurationProxy(proxyClass = MCRStringEqualsMatcher.Factory.class)
public class MCRStringEqualsMatcher implements MCRMatcher<String> {

    private final String expectedValue;

    /**
     * Creates a new {@code MCRStringEqualsMatcher}.
     *
     * @param expectedValue the value the extracted value must equal
     */
    public MCRStringEqualsMatcher(String expectedValue) {
        this.expectedValue = Objects.requireNonNull(expectedValue, "expectedValue must not be null");
    }

    @Override
    public boolean matches(String extracted) {
        return expectedValue.equals(extracted);
    }

    /**
     * Factory for creating {@link MCRStringEqualsMatcher} instances from configuration properties.
     */
    public static class Factory implements Supplier<MCRStringEqualsMatcher> {

        @MCRProperty(name = "Value")
        public String value;

        @Override
        public MCRStringEqualsMatcher get() {
            return new MCRStringEqualsMatcher(value);
        }
    }
}
