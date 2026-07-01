package de.gbv.reposis.user.ldap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import javax.naming.ldap.LdapName;

import org.mycore.common.MCRUserInformation;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstance;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.user2.MCRUser;

import de.gbv.reposis.user.ldap.dn.MCRLDAPDNResolver;
import de.gbv.reposis.user.mapper.attribute.MCRAttributeMapper;
import de.gbv.reposis.user.mapper.role.MCRRoleMapper;

/**
 * Service for authenticating users against an LDAP server.
 */
@MCRConfigurationProxy(proxyClass = MCRLDAPAuthService.Factory.class)
public class MCRLDAPAuthService {

    private static final Set<String> EXCLUDED_ATTRIBUTE_NAMES =
        Set.of(MCRUserInformation.ATT_EMAIL, MCRUserInformation.ATT_REAL_NAME);

    private final MCRLDAPDNResolver resolver;
    private final MCRLDAPAuthClient client;
    private final MCRAttributeMapper attributeMapper;
    private final MCRRoleMapper roleMapper;
    private final List<String> defaultRoles;
    private final List<String> fallbackRoles;
    private String realmId;

    /**
     * Creates a new {@code MCRLDAPAuthService}.
     *
     * @param resolver used to resolve the user's DN
     * @param client used to verify the password
     * @param attributeMapper used to map LDAP attributes to user attributes
     * @param roleMapper used to map LDAP attributes to roles
     * @param defaultRoles roles assigned to every successfully authenticated LDAP user
     * @param fallbackRoles roles assigned if there is no role
     */
    public MCRLDAPAuthService(MCRLDAPDNResolver resolver, MCRLDAPAuthClient client,
        MCRAttributeMapper attributeMapper, MCRRoleMapper roleMapper, List<String> defaultRoles,
        List<String> fallbackRoles) {
        this.resolver = Objects.requireNonNull(resolver, "resolver must not be null");
        this.client = Objects.requireNonNull(client, "client must not be null");
        this.attributeMapper = attributeMapper;
        this.roleMapper = roleMapper;
        this.defaultRoles = Objects.requireNonNull(defaultRoles, "defaultRoles must not be null");
        this.fallbackRoles = Objects.requireNonNull(fallbackRoles, "fallbackRoles must not be null");
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
        MCRUser user = new MCRUser(username, realmId);
        if (attributeMapper != null) {
            assignAttributes(user, ldapUser.attributes());
        }
        assignRoles(user, ldapUser.attributes());
        return user;
    }

    private void assignAttributes(MCRUser user, Map<String, List<String>> rawAttributes) {
        Map<String, String> attributes = attributeMapper.map(rawAttributes).userAttributes();
        attributes.entrySet().stream()
            .filter(entry -> !EXCLUDED_ATTRIBUTE_NAMES.contains(entry.getKey()))
            .forEach(entry -> user.setUserAttribute(entry.getKey(), entry.getValue()));
        String eMail = attributes.get(MCRUserInformation.ATT_EMAIL);
        if (eMail != null) {
            user.setEMail(eMail);
        }
        String realName = attributes.get(MCRUserInformation.ATT_REAL_NAME);
        if (realName != null) {
            user.setRealName(realName);
        }
    }

    private void assignRoles(MCRUser user, Map<String, List<String>> rawAttributes) {
        Set<String> roles = new HashSet<>();
        if (roleMapper != null) {
            roles.addAll(roleMapper.map(rawAttributes));
        }
        if (!defaultRoles.isEmpty()) {
            roles.addAll(defaultRoles);
        }
        if (roles.isEmpty() && !fallbackRoles.isEmpty()) {
            roles.addAll(fallbackRoles);
        }
        roles.forEach(user::assignRole);
    }

    /**
     * Factory for creating {@link MCRLDAPAuthService} instances from configuration properties.
     */
    public static class Factory implements Supplier<MCRLDAPAuthService> {

        @MCRInstance(name = "Resolver", valueClass = MCRLDAPDNResolver.class)
        public MCRLDAPDNResolver resolver;

        @MCRInstance(name = "Client", valueClass = MCRLDAPAuthClient.class)
        public MCRLDAPAuthClient client;

        @MCRInstance(name = "AttributeMapper", valueClass = MCRAttributeMapper.class, required = false)
        public MCRAttributeMapper attributeMapper;

        @MCRInstance(name = "RoleMapper", valueClass = MCRRoleMapper.class, required = false)
        public MCRRoleMapper roleMapper;

        @MCRProperty(name = "DefaultRoles", required = false)
        public String defaultRolesString;

        @MCRProperty(name = "FallbackRoles", required = false)
        public String fallbackRolesString;

        @Override
        public MCRLDAPAuthService get() {
            List<String> defaultRoles = splitToList(defaultRolesString);
            List<String> fallbackRoles = splitToList(fallbackRolesString);
            return new MCRLDAPAuthService(resolver, client, attributeMapper, roleMapper, defaultRoles, fallbackRoles);
        }

        private static List<String> splitToList(String value) {
            if (value == null || value.isBlank()) {
                return List.of();
            }
            return Arrays.stream(value.split(","))
                .map(String::strip)
                .filter(s -> !s.isEmpty())
                .toList();
        }
    }
}
