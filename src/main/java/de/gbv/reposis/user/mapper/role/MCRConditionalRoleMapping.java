package de.gbv.reposis.user.mapper.role;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstance;
import org.mycore.common.config.annotation.MCRProperty;

import de.gbv.reposis.user.matcher.MCRStringMatcher;

/**
 * A {@link MCRRoleMapping} that contributes a single, fixed role if at least one value of a
 * given source attribute matches a configured {@link MCRStringMatcher}.
 */
@MCRConfigurationProxy(proxyClass = MCRConditionalRoleMapping.Factory.class)
public class MCRConditionalRoleMapping implements MCRRoleMapping {

    private final String source;
    private final MCRStringMatcher matcher;
    private final String role;

    /**
     * Creates a new {@code MCRConditionalRoleMapping}.
     *
     * @param source the name of the raw attribute whose values are checked
     * @param matcher the matcher used to check the attribute's values
     * @param role the role to contribute if the matcher matches at least one value
     */
    public MCRConditionalRoleMapping(String source, MCRStringMatcher matcher, String role) {
        this.source = Objects.requireNonNull(source, "source must not be null");
        this.matcher = Objects.requireNonNull(matcher, "matcher must not be null");
        this.role = Objects.requireNonNull(role, "role must not be null");
    }

    @Override
    public Optional<String> apply(Map<String, List<String>> attributes) {
        List<String> values = attributes.getOrDefault(source, List.of());
        boolean anyMatches = values.stream().anyMatch(matcher::matches);
        return anyMatches ? Optional.of(role) : Optional.empty();
    }

    /**
     * Factory for creating {@link MCRConditionalRoleMapping} instances from configuration properties.
     */
    public static class Factory implements Supplier<MCRConditionalRoleMapping> {

        @MCRProperty(name = "Source")
        public String source;

        @MCRInstance(name = "Matcher", valueClass = MCRStringMatcher.class)
        public MCRStringMatcher matcher;

        @MCRProperty(name = "Role")
        public String role;

        @Override
        public MCRConditionalRoleMapping get() {
            return new MCRConditionalRoleMapping(source, matcher, role);
        }
    }
}
