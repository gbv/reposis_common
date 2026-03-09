package de.gbv.reposis.user.ldap;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstance;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.user2.MCRUser;

import de.gbv.reposis.user.ldap.mapper.MCRLDAPAttributeMapper;

/**
 * Service for authenticating users against an LDAP server.
 */
@MCRConfigurationProxy(proxyClass = MCRLDAPAuthService.Factory.class)
public class MCRLDAPAuthService {

    private final MCRLDAPAuthClient client;
    private final MCRLDAPAttributeMapper attributeMapper;
    private final List<String> defaultRoles;
    private String realmId;

    /**
     * Creates a new {@code MCRLDAPAuthService}.
     *
     * @param client used to resolve the user's DN and verify the password
     * @param attributeMapper used to map LDAP attributes to user attributes
     * @param defaultRoles roles assigned to every successfully authenticated LDAP user
     */
    public MCRLDAPAuthService(MCRLDAPAuthClient client, MCRLDAPAttributeMapper attributeMapper, List<String> defaultRoles) {
        this.client = client;
        this.attributeMapper = attributeMapper;
        this.defaultRoles = defaultRoles;
    }

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
        MCRLDAPAuthResult ldapUser = client.authenticate(username, password);
        if (ldapUser == null) {
            throw new MCRLDAPAuthException("Invalid username or password");
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

        @MCRInstance(name = "Client", valueClass = MCRLDAPAuthClient.class)
        public MCRLDAPAuthClient client;

        @MCRInstance(name = "AttributeMapper", valueClass = MCRLDAPAttributeMapper.class)
        public MCRLDAPAttributeMapper attributeMapper;

        @MCRProperty(name = "DefaultRoles", required = false)
        public String defaultRolesString;

        @Override
        public MCRLDAPAuthService get() {
            List<String> defaultRoles = defaultRolesString == null ? Collections.emptyList() : Arrays.asList(
                defaultRolesString.split("\\s*,\\s*"));
            return new MCRLDAPAuthService(client, attributeMapper, defaultRoles);
        }
    }
}
