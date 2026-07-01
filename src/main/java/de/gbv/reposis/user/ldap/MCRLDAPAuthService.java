package de.gbv.reposis.user.ldap;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import javax.naming.ldap.LdapName;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstance;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.user2.MCRUser;

import de.gbv.reposis.user.ldap.mapper.MCRLDAPAttributeMapper;
import de.gbv.reposis.user.ldap.dn.MCRLDAPDNResolver;

/**
 * Service for authenticating users against an LDAP server.
 */
@MCRConfigurationProxy(proxyClass = MCRLDAPAuthService.Factory.class)
public class MCRLDAPAuthService {

    private final MCRLDAPDNResolver resolver;
    private final MCRLDAPAuthClient client;
    private final MCRLDAPAttributeMapper attributeMapper;
    private final List<String> defaultRoles;
    private String realmId;

    /**
     * Creates a new {@code MCRLDAPAuthService}.
     *
     * @param resolver used to resolve the user's DN
     * @param client used to verify the password
     * @param attributeMapper used to map LDAP attributes to user attributes
     * @param defaultRoles roles assigned to every successfully authenticated LDAP user
     */
    public MCRLDAPAuthService(MCRLDAPDNResolver resolver, MCRLDAPAuthClient client,
        MCRLDAPAttributeMapper attributeMapper, List<String> defaultRoles) {
        this.resolver = Objects.requireNonNull(resolver, "resolver must not be null");
        this.client = Objects.requireNonNull(client, "client must not be null");
        this.attributeMapper = attributeMapper;
        this.defaultRoles = Objects.requireNonNull(defaultRoles, "defaultRoles must not be null");
    }

    /**
     * Initializes this service with the ID of the realm it is responsible for.
     *
     * @param realmId the realm ID
     */
    public void init(String realmId) {
        this.realmId = realmId;
    }

    /**
     * Authenticates a user against the LDAP server.
     * <p>
     * Resolves the user's DN by UID, then verifies the password via a simple bind.
     * The same exception is thrown regardless of whether the user was not found or
     * the password was incorrect – to avoid leaking information about existing accounts.
     *
     * @param username the login name (UID)
     * @param password the plain-text password
     * @return {@link MCRUser} for the authenticated user
     * @throws MCRLDAPAuthException if the user is not found or the password is invalid
     * @throws org.mycore.common.MCRUsageException if the user is ambiguous or an LDAP error occurs
     */
    public MCRUser authenticate(String username, String password) {
        String dn = resolver.resolve(username).map(LdapName::toString)
            .orElseThrow(() -> new MCRLDAPAuthException("No DN found for username: " + username));
        MCRLDAPAuthResult ldapUser = client.authenticate(dn, password);
        if (ldapUser == null) {
            throw new MCRLDAPAuthException("Authentication failed for DN: " + dn);
        }
        MCRLDAPAttributeMapper.MappingsResult mappingsResult = attributeMapper.map(ldapUser.attributes());
        MCRUser user = new MCRUser(username, realmId);
        mappingsResult.userAttributes().forEach(user::setUserAttribute);
        defaultRoles.forEach(user::assignRole);
        return user;
    }

    /**
     * Factory for creating {@link MCRLDAPAuthService} instances from configuration properties.
     */
    public static class Factory implements Supplier<MCRLDAPAuthService> {

        @MCRInstance(name = "Resolver", valueClass = MCRLDAPDNResolver.class)
        public MCRLDAPDNResolver resolver;

        @MCRInstance(name = "Client", valueClass = MCRLDAPAuthClient.class)
        public MCRLDAPAuthClient client;

        @MCRInstance(name = "AttributeMapper", valueClass = MCRLDAPAttributeMapper.class, required = false)
        public MCRLDAPAttributeMapper attributeMapper;

        @MCRProperty(name = "DefaultRoles", required = false)
        public String defaultRolesString;

        @Override
        public MCRLDAPAuthService get() {
            List<String> defaultRoles = defaultRolesString == null ? Collections.emptyList() : Arrays.asList(
                defaultRolesString.split("\\s*,\\s*"));
            return new MCRLDAPAuthService(resolver, client, attributeMapper, defaultRoles);
        }
    }
}
