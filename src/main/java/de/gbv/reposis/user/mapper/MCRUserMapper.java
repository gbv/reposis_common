package de.gbv.reposis.user.mapper;

import java.util.List;
import java.util.function.Supplier;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstance;

import de.gbv.reposis.mapper.source.MCRValueSource;
import de.gbv.reposis.user.mapper.attribute.MCRUserAttributeMapper;
import de.gbv.reposis.user.mapper.role.MCRRoleMapper;

/**
 * Combines role and user-attribute mapping into a single step, typically used during
 * authentication to derive both the role and the attributes for an external user from the
 * same set of raw attributes.
 *
 * @see MCRRoleMapper
 * @see MCRUserAttributeMapper
 */
@MCRConfigurationProxy(proxyClass = MCRUserMapper.Factory.class)
public class MCRUserMapper {

    final MCRRoleMapper roleMapper;
    final MCRUserAttributeMapper attributeMapper;

    /**
     * Creates a new {@code MCRUserMapper}.
     *
     * @param roleMapper the role mapper to derivate roles from raw attributes
     * @param attributeMapper the attribute mapper to derivate user attributes from raw attributes
     */
    public MCRUserMapper(MCRRoleMapper roleMapper, MCRUserAttributeMapper attributeMapper) {
        this.roleMapper = roleMapper;
        this.attributeMapper = attributeMapper;
    }

    /**
     * Derives roles and user attributes from the given attribute source.
     *
     * @param source the source providing the raw attribute values, as received e.g. from an
     *               identity provider
     * @return the mapped user data, containing both roles and attributes
     */
    public MCRMappedUserData map(MCRValueSource<String> source) {
        return new MCRMappedUserData(roleMapper.map(source), attributeMapper.map(source));
    }

    /**
     * Factory for creating {@link MCRUserMapper} instances from configuration properties.
     */
    public static class Factory implements Supplier<MCRUserMapper> {

        @MCRInstance(name = "RoleMapper", valueClass = MCRRoleMapper.class, required = false)
        public MCRRoleMapper roleMapper;

        @MCRInstance(name = "AttributeMapper", valueClass = MCRUserAttributeMapper.class, required = false)
        public MCRUserAttributeMapper attributeMapper;

        @Override
        public MCRUserMapper get() {
            return new MCRUserMapper(
                roleMapper != null ? roleMapper : new MCRRoleMapper(List.of()),
                attributeMapper != null ? attributeMapper : new MCRUserAttributeMapper(List.of())
            );
        }
    }
}
