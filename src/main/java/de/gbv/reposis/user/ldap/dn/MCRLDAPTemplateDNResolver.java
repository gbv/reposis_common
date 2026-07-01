package de.gbv.reposis.user.ldap.dn;

import java.util.Optional;
import java.util.function.Supplier;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;

/**
 * Resolves a user's distinguished name (DN) by appending a username-based RDN
 * to a configured base DN, without performing an actual LDAP lookup.
 */
@MCRConfigurationProxy(proxyClass = MCRLDAPTemplateDNResolver.Factory.class)
public class MCRLDAPTemplateDNResolver implements MCRLDAPDNResolver {

    private final String baseDN;
    private final String attributeName;

    /**
     * Creates a new {@code MCRLDAPTemplateDNResolver}.
     *
     * @param baseDN the base DN under which the resolved DN is constructed
     * @param attributeName the RDN attribute name (e.g. {@code "uid"} or {@code "cn"})
     */
    public MCRLDAPTemplateDNResolver(String baseDN, String attributeName) {
        this.baseDN = baseDN;
        this.attributeName = attributeName;
    }

    @Override
    public Optional<LdapName> resolve(String username) {
        try {
            LdapName dn = new LdapName(baseDN);
            dn.add(new Rdn(attributeName, username));
            return Optional.of(dn);
        } catch (InvalidNameException e) {
            return Optional.empty();
        }
    }

    /**
     * Factory for creating {@link MCRLDAPTemplateDNResolver} instances from configuration properties.
     */
    public static class Factory implements Supplier<MCRLDAPTemplateDNResolver> {

        @MCRProperty(name = "BaseDN")
        public String baseDN;

        @MCRProperty(name = "AttributeName")
        public String attributeName;

        @Override
        public MCRLDAPTemplateDNResolver get() {
            return new MCRLDAPTemplateDNResolver(baseDN, attributeName);
        }
    }
}
