package de.gbv.reposis.user.ldap.mapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;

/**
 * Default implementation of {@link MCRLDAPMapping} that maps a single LDAP attribute
 * to a target name.
 */
@MCRConfigurationProxy(proxyClass = MCRDefaultLDAPMapping.Factory.class)
public class MCRDefaultLDAPMapping implements MCRLDAPMapping {

    private final String name;

    private final String targetName;

    /**
     * Creates a new {@code MCRDefaultLDAPMapping}.
     *
     * @param name the LDAP attribute name to read from, e.g. {@code "mail"}
     * @param targetName the target name to map to, e.g. {@code "email"}
     */
    public MCRDefaultLDAPMapping(String name, String targetName) {
        this.name = name;
        this.targetName = targetName;
    }

    @Override
    public Optional<Result> apply(Map<String, List<String>> ldapAttributes) {
        return Optional.ofNullable(ldapAttributes.get(name))
            .filter(values -> !values.isEmpty())
            .map(values -> new Result(targetName, values.get(0)));
    }

    /**
     * Factory for creating {@link MCRDefaultLDAPMapping} instances from configuration properties.
     */
    public static class Factory implements Supplier<MCRDefaultLDAPMapping> {

        @MCRProperty(name = "Name")
        public String name;

        @MCRProperty(name = "TargetName")
        public String targetName;

        @Override
        public MCRDefaultLDAPMapping get() {
            return new MCRDefaultLDAPMapping(name, targetName);
        }
    }
}
