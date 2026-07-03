package de.gbv.reposis.user.mapper.role;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstanceList;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.user2.MCRRoleManager;

/**
 * Maps raw attributes to a set of roles based on a configured list of {@link MCRRoleMapping} instances.
 * <p>
 * Each configured mapping is applied independently to the given raw attributes and contributes
 * zero or more roles to the result.
 * Optionally, a default role can be added unconditionally, and a fallback role can be added if no other role was
 * derived.
 */
@MCRConfigurationProxy(proxyClass = MCRRoleMapper.Factory.class)
public class MCRRoleMapper {

    private final List<MCRRoleMapping> roleMappings;

    /**
     * Creates a new {@code MCRRoleMapper}.
     *
     * @param roleMappings the list of role mappings to apply, in order
     */
    public MCRRoleMapper(List<MCRRoleMapping> roleMappings) {
        this.roleMappings = Objects.requireNonNull(roleMappings, "roleMappings must not be null");
    }

    /**
     * Applies all configured role mappings to the given raw attributes and aggregates the
     * results.
     *
     * @param attributes the raw attributes, keyed by attribute name, with each value being the
     *                    (possibly multi-valued) list of values for that attribute
     * @return an immutable set of roles derived from the given attributes
     */
    public Set<String> map(Map<String, List<String>> attributes) {
        Set<String> roles = roleMappings.stream()
            .map(m -> m.apply(attributes))
            .flatMap(Optional::stream)
            .collect(Collectors.toCollection(HashSet::new));
        return Set.copyOf(roles);
    }

    /**
     * Factory for creating {@link MCRRoleMapper} instances from configuration properties.
     */
    public static class Factory implements Supplier<MCRRoleMapper> {

        @MCRInstanceList(name = "Roles", valueClass = MCRRoleMapping.class, required = false)
        public List<MCRRoleMapping> roles;

        @Override
        public MCRRoleMapper get() {
            return new MCRRoleMapper(roles != null ? roles : List.of());
        }
    }
}
