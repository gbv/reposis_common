package de.gbv.reposis.user.matcher;

import java.util.Objects;
import java.util.function.Supplier;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;

/**
 * A {@link MCRStringMatcher} that matches values containing a configured substring.
 */
@MCRConfigurationProxy(proxyClass = MCRContainsStringMatcher.Factory.class)
public class MCRContainsStringMatcher implements MCRStringMatcher {

    private final String substring;

    /**
     * Creates a new {@code MCRContainsStringMatcher}.
     *
     * @param substring the substring that {@link #matches(String)} checks for
     */
    public MCRContainsStringMatcher(String substring) {
        this.substring = Objects.requireNonNull(substring, "substring must not be null");
    }

    @Override
    public boolean matches(String actualValue) {
        return actualValue != null && actualValue.contains(substring);
    }

    /**
     * Factory for creating {@link MCRContainsStringMatcher} instances from configuration properties.
     */
    public static class Factory implements Supplier<MCRContainsStringMatcher> {

        @MCRProperty(name = "Substring")
        public String substring;

        @Override
        public MCRContainsStringMatcher get() {
            return new MCRContainsStringMatcher(substring);
        }
    }
}
