package de.gbv.reposis.user.ldap;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import javax.naming.ldap.LdapName;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstance;
import org.mycore.common.config.annotation.MCRProperty;

import de.gbv.reposis.mapper.source.MCRMapValueSource;
import de.gbv.reposis.user.MCRUserData;
import de.gbv.reposis.user.ldap.dn.MCRLDAPDNResolver;
import de.gbv.reposis.user.mapper.MCRMappedUserData;
import de.gbv.reposis.user.mapper.MCRUserMapper;
import de.gbv.reposis.user.mapper.attribute.MCRUserAttributeMapper;
import de.gbv.reposis.user.mapper.role.MCRRoleMapper;

@MCRConfigurationProxy(proxyClass = MCRLDAPAuthService.Factory.class)
public class MCRLDAPAuthService {

    private static final Logger LOGGER = LogManager.getLogger();

    private final MCRLDAPDNResolver resolver;
    private final MCRLDAPAuthClient client;
    private final MCRUserMapper userMapper;
    private final List<String> defaultRoles;
    private final List<String> fallbackRoles;
    private String realmId;

    /**
     * Creates a new {@code MCRLDAPAuthService}.
     *
     * @param resolver used to resolve the user's DN
     * @param client used to verify the password
     * @param userMapper used to derive roles and user attributes from raw LDAP attributes, or
     *                   {@code null} if no roles or attributes should be derived
     * @param defaultRoles roles assigned to every successfully authenticated LDAP user
     * @param fallbackRoles roles assigned if there is no role
     */
    public MCRLDAPAuthService(MCRLDAPDNResolver resolver, MCRLDAPAuthClient client,
        MCRUserMapper userMapper, List<String> defaultRoles, List<String> fallbackRoles) {
        this.resolver = Objects.requireNonNull(resolver, "resolver must not be null");
        this.client = Objects.requireNonNull(client, "client must not be null");
        this.userMapper = userMapper != null ? userMapper : new MCRUserMapper(new MCRRoleMapper(List.of()),
            new MCRUserAttributeMapper(List.of()));
        this.defaultRoles = Objects.requireNonNull(defaultRoles, "defaultRoles must not be null");
        this.fallbackRoles = Objects.requireNonNull(fallbackRoles, "fallbackRoles must not be null");
    }

    /**
     * Initializes this authentication service for the given realm.
     *
     * @param realmId the identifier of the realm this service instance is responsible for
     */
    public void init(String realmId) {
        this.realmId = realmId;
    }

    /**
     * Authenticates a user against LDAP by resolving the given username to a distinguished name (DN)
     * and performing a bind with the supplied password.
     *
     * @param username the username to authenticate
     * @param password the user's password
     * @return the authenticated user's data
     * @throws MCRLDAPAuthException if no DN could be resolved for the username, or if the
     *                              LDAP bind failed (invalid credentials)
     */
    public MCRUserData authenticate(String username, String password) {
        String dn = resolver.resolve(username).map(LdapName::toString)
            .orElseThrow(() -> {
                LOGGER.debug("No DN found for username: {}", username);
                return new MCRLDAPAuthException("Authentication failed");
            });
        MCRLDAPAuthResult ldapResult = client.authenticate(dn, password);
        if (ldapResult == null) {
            LOGGER.debug("Authentication failed for DN: {}", dn);
            throw new MCRLDAPAuthException("Authentication failed");
        }
        return getUserData(username, realmId, ldapResult.attributes());
    }

    private MCRUserData getUserData(String username, String realmId, Map<String, List<String>> rawAttributes) {
        MCRMappedUserData mapped = userMapper.map(new MCRMapValueSource<>(rawAttributes));
        Set<String> roles = new HashSet<>(mapped.roles());
        if (!defaultRoles.isEmpty()) {
            roles.addAll(defaultRoles);
        }
        if (roles.isEmpty() && !fallbackRoles.isEmpty()) {
            roles.addAll(fallbackRoles);
        }
        return new MCRUserData(username, realmId, roles, mapped.attributes());
    }

    /**
     * Factory for creating {@link MCRLDAPAuthService} instances from configuration properties.
     */
    public static class Factory implements Supplier<MCRLDAPAuthService> {

        @MCRInstance(name = "Resolver", valueClass = MCRLDAPDNResolver.class)
        public MCRLDAPDNResolver resolver;

        @MCRInstance(name = "Client", valueClass = MCRLDAPAuthClient.class)
        public MCRLDAPAuthClient client;

        @MCRInstance(name = "UserMapper", valueClass = MCRUserMapper.class, required = false)
        public MCRUserMapper userMapper;

        @MCRProperty(name = "DefaultRoles", required = false)
        public String defaultRolesString;

        @MCRProperty(name = "FallbackRoles", required = false)
        public String fallbackRolesString;

        @Override
        public MCRLDAPAuthService get() {
            List<String> defaultRoles = splitToList(defaultRolesString);
            List<String> fallbackRoles = splitToList(fallbackRolesString);
            return new MCRLDAPAuthService(resolver, client, userMapper, defaultRoles, fallbackRoles);
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
