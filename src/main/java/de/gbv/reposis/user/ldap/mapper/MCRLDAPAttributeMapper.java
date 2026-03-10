package de.gbv.reposis.user.ldap.mapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstanceList;

/**
 * Maps LDAP attributes to MCR user attributes based on a configured mapping.
 */
@MCRConfigurationProxy(proxyClass = MCRLDAPAttributeMapper.Factory.class)
public class MCRLDAPAttributeMapper {

    private final List<MCRLDAPMapping> attributeMappings;

    /**
     * Creates a new {@code MCRLDAPAttributeMapper}.
     *
     * @param attributeMappings a list of mappings
     */
    public MCRLDAPAttributeMapper(List<MCRLDAPMapping> attributeMappings) {
        this.attributeMappings = attributeMappings;
    }

    /**
     * Maps raw LDAP attributes.
     *
     * @param ldapAttributes raw attributes returned by the LDAP server
     * @return aggregated result of all applied mappings
     */
    public MappingsResult map(Map<String, List<String>> ldapAttributes) {
        Map<String, String> userAttributes = attributeMappings.stream()
            .map(m -> m.apply(ldapAttributes))
            .flatMap(Optional::stream)
            .collect(Collectors.toMap(
                MCRLDAPMapping.Result::key,
                MCRLDAPMapping.Result::value
            ));
        return new MappingsResult(userAttributes);
    }

    /**
     * Factory for creating {@link MCRLDAPAttributeMapper} instances from configuration properties.
     */
    public static class Factory implements Supplier<MCRLDAPAttributeMapper> {

        @MCRInstanceList(name = "Attributes", valueClass = MCRLDAPMapping.class, required = false)
        public List<MCRLDAPMapping> attributeMappings;

        @Override
        public MCRLDAPAttributeMapper get() {
            return new MCRLDAPAttributeMapper(attributeMappings);
        }
    }

    /**
     * Aggregated result of all applied mappings.
     *
     * @param userAttributes MCR user attribute name → value
     */
    public record MappingsResult(Map<String, String> userAttributes) {}
}
