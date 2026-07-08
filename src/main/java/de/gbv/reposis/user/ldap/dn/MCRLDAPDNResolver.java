package de.gbv.reposis.user.ldap.dn;

import java.util.Optional;

import javax.naming.ldap.LdapName;

/**
 * Resolves the LDAP distinguished name (DN) for a given username.
 */
public interface MCRLDAPDNResolver {

    /**
     * Resolves the distinguished name (DN) for the given username.
     *
     * @param username the username to resolve
     * @return an {@link Optional} containing the resolved distinguished name (DN),
     *         or {@link Optional#empty()} if no matching entry was found
     */
    Optional<LdapName> resolve(String username);
}
