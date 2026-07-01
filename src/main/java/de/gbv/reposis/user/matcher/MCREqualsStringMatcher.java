package de.gbv.reposis.user.matcher;

import java.util.Objects;
import java.util.function.Supplier;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;

/**
 * A {@link MCRStringMatcher} that matches values via exact, case-sensitive equality against a
 * configured expected value.
 */
@MCRConfigurationProxy(proxyClass = MCREqualsStringMatcher.Factory.class)
public class MCREqualsStringMatcher implements MCRStringMatcher {

    private final String expectedValue;

    /**
     * Creates a new {@code MCREqualsStringMatcher}.
     *
     * @param expectedValue the value that {@link #matches(String)} compares against
     */
    public MCREqualsStringMatcher(String expectedValue) {
        this.expectedValue = Objects.requireNonNull(expectedValue, "expectedValue must not be null");
    }

    @Override
    public boolean matches(String actualValue) {
        return expectedValue.equals(actualValue);
    }

    /**
     * Factory for creating {@link MCREqualsStringMatcher} instances from configuration properties.
     */
    public static class Factory implements Supplier<MCREqualsStringMatcher> {

        @MCRProperty(name = "ExpectedValue")
        public String expectedValue;

        @Override
        public MCREqualsStringMatcher get() {
            return new MCREqualsStringMatcher(expectedValue);
        }
    }
}
