package de.gbv.reposis.mapper.extractor;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;

/**
 * Extracts a value from a given string using a regular expression. The pattern must match the
 * entire value (not just a part of it); use {@code .*} around the relevant part if only a
 * portion of the value is variable.
 */
@MCRConfigurationProxy(proxyClass = MCRStringRegexExtractor.Factory.class)
public class MCRStringRegexExtractor implements MCRExtractor<String> {

    private final Pattern pattern;
    private final int group;

    /**
     * Creates a new {@code MCRStringRegexExtractor}.
     *
     * @param pattern the regular expression to apply to the given value
     * @param group the capture group to extract on a match (0 for the whole match)
     */
    public MCRStringRegexExtractor(Pattern pattern, int group) {
        this.pattern = Objects.requireNonNull(pattern, "pattern must not be null");
        int maxGroup = pattern.matcher("").groupCount();
        if (group < 0 || group > maxGroup) {
            throw new IllegalArgumentException(
                "group must be between 0 and " + maxGroup + " for the given pattern, but was " + group);
        }
        this.group = group;
    }

    @Override
    public Optional<String> extract(String value) {
        Objects.requireNonNull(value, "value must not be null");
        Matcher matcher = pattern.matcher(value);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        return Optional.of(matcher.group(group));
    }

    /**
     * Factory for creating {@link MCRStringRegexExtractor} instances from configuration
     * properties. If no {@code Group} is configured, the first capture group (index 1) is used.
     */
    public static class Factory implements Supplier<MCRStringRegexExtractor> {

        @MCRProperty(name = "Pattern")
        public String pattern;

        @MCRProperty(name = "Group", required = false)
        public String groupString;

        @Override
        public MCRStringRegexExtractor get() {
            return new MCRStringRegexExtractor(resolvePattern(), resolveGroup());
        }

        private Pattern resolvePattern() {
            try {
                return Pattern.compile(pattern);
            } catch (PatternSyntaxException e) {
                throw new MCRConfigurationException("Pattern is not a valid regular expression: " + pattern, e);
            }
        }

        private int resolveGroup() {
            try {
                return Optional.ofNullable(groupString).map(Integer::parseInt).orElse(1);
            } catch (NumberFormatException e) {
                throw new MCRConfigurationException("Group must be a valid integer, but was: " + groupString, e);
            }
        }
    }
}
