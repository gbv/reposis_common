package de.gbv.reposis.mapper.matcher;

import java.util.Objects;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;

/**
 * A {@link MCRValueMatcher} that checks whether a given string value matches a configured regular
 * expression. A {@code null} value never matches.
 */
@MCRConfigurationProxy(proxyClass = MCRStringRegexMatcher.Factory.class)
public class MCRStringRegexMatcher implements MCRValueMatcher<String> {

    private final Pattern pattern;

    /**
     * Creates a new {@code MCRStringRegexMatcher}.
     *
     * @param pattern the pattern the extracted value must match
     */
    public MCRStringRegexMatcher(Pattern pattern) {
        this.pattern = Objects.requireNonNull(pattern, "pattern must not be null");
    }

    @Override
    public boolean test(String extracted) {
        return extracted != null && pattern.matcher(extracted).matches();
    }

    /**
     * Factory for creating {@link MCRStringRegexMatcher} instances from configuration properties.
     */
    public static class Factory implements Supplier<MCRStringRegexMatcher> {

        @MCRProperty(name = "Pattern")
        public String pattern;

        @Override
        public MCRStringRegexMatcher get() {
            return new MCRStringRegexMatcher(resolvePattern());
        }

        private Pattern resolvePattern() {
            try {
                return Pattern.compile(pattern);
            } catch (PatternSyntaxException e) {
                throw new MCRConfigurationException("Pattern is not a valid regular expression: " + pattern, e);
            }
        }
    }
}
