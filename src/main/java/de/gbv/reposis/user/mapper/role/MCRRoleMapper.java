package de.gbv.reposis.user.mapper.role;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstanceList;

import de.gbv.reposis.mapper.source.MCRValueSource;

/**
 * Maps raw attributes to a set of roles based on a configured list of {@link MCRRoleMapping} instances.
 *
 * @see MCRRoleMapping
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
     * Applies all configured role mappings to the given attribute source and aggregates the
     * results.
     *
     * @param source the source providing the raw attribute values
     * @return an immutable set of roles derived from the given source
     */
    public Set<String> map(MCRValueSource<String> source) {
        return roleMappings.stream()
            .map(m -> m.apply(source))
            .flatMap(Optional::stream)
            .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Factory for creating {@link MCRRoleMapper} instances from configuration properties.
     */
    public static class Factory implements Supplier<MCRRoleMapper> {

        @MCRInstanceList(name = "Roles", valueClass = MCRRoleMapping.class, required = false)
        public List<MCRRoleMapping> roles;

        @Override
        public MCRRoleMapper get() {
            return new MCRRoleMapper(Optional.ofNullable(roles).orElse(List.of()));
        }
    }
}
