package de.gbv.reposis.user.matcher;

import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;

/**
 * A {@link MCRStringMatcher} that matches values against a regular expression.
 * <p>
 * A value matches if the entire value matches the configured pattern, i.e. via
 * {@link Pattern#matcher(CharSequence)} and {@link java.util.regex.Matcher#matches()}, not merely
 * a substring of it.
 */
@MCRConfigurationProxy(proxyClass = MCRPatternStringMatcher.Factory.class)
public class MCRPatternStringMatcher implements MCRStringMatcher {

    private final Pattern pattern;

    /**
     * Creates a new {@code MCRPatternStringMatcher}.
     *
     * @param regex the regular expression to match values against
     * @throws java.util.regex.PatternSyntaxException if the given regex is not a valid regular expression
     */
    public MCRPatternStringMatcher(String regex) {
        this.pattern = Pattern.compile(regex);
    }

    @Override
    public boolean matches(String actualValue) {
        return actualValue != null && pattern.matcher(actualValue).matches();
    }

    /**
     * Factory for creating {@link MCRPatternStringMatcher} instances from configuration properties.
     */
    public static class Factory implements Supplier<MCRPatternStringMatcher> {

        @MCRProperty(name = "Pattern")
        public String pattern;

        @Override
        public MCRPatternStringMatcher get() {
            return new MCRPatternStringMatcher(pattern);
        }
    }
}
