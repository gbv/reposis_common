package de.gbv.reposis.user.ldap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRUsageException;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;

/**
 * Low-level LDAP client for resolving user entries and verifying credentials.
 * <p>
 * Handles all JNDI context management internally. Higher-level services
 * such as {@link MCRLDAPAuthService} build on this client.
 */
@MCRConfigurationProxy(proxyClass = MCRLDAPClient.Factory.class)
public class MCRLDAPClient {

    private static final Logger LOGGER = LogManager.getLogger();

    private final Hashtable<String, String> env;
    private final String baseDn;
    private final String userIdFilterTemplate;

    /**
     * Creates a new {@code MCRLDAPClient} from typed connection settings.
     *
     * @param connectionSettings LDAP connection configuration
     * @param baseDn base DN under which users are searched
     * @param userIdFilterTemplate LDAP filter with {@code {0}} as username placeholder, e.g. {@code (uid={0})}
     */
    public MCRLDAPClient(ConnectionSettings connectionSettings, String baseDn, String userIdFilterTemplate) {
        this.env = new Hashtable<>(connectionSettings.toEnv());
        this.baseDn = baseDn;
        this.userIdFilterTemplate = userIdFilterTemplate;
    }

    /**
     * Verifies credentials by attempting a simple bind against the LDAP server.
     * <p>
     * The context is opened and immediately closed – only the result is returned.
     *
     * @param dn the fully qualified DN to bind as
     * @param credentials the password to verify
     * @return {@code true} if the bind succeeded, {@code false} if the credentials are invalid
     * @throws MCRUsageException if a connection or LDAP error occurs
     */
    public boolean bind(String dn, String credentials) {
        final Hashtable<String, String> bindEnv = new Hashtable<>(env);
        bindEnv.put(Context.SECURITY_AUTHENTICATION, "simple");
        bindEnv.put(Context.SECURITY_PRINCIPAL, dn);
        bindEnv.put(Context.SECURITY_CREDENTIALS, credentials);
        DirContext ctx = null;
        try {
            ctx = new InitialDirContext(bindEnv);
            LOGGER.debug("Credentials verified for DN: {}", dn);
            return true;
        } catch (AuthenticationException e) {
            return false;
        } catch (NamingException e) {
            throw new MCRUsageException("Could not connect to LDAP server", e);
        } finally {
            closeQuietly(ctx);
        }
    }

    /**
     * Resolves the distinguished name (DN) of a user entry identified by the given UID.
     * <p>
     * The UID is substituted into the configured filter template and used to search
     * within the configured base DN.
     *
     * @param uid the UID to search for
     * @return the fully qualified DN of the matching entry, or empty if not found
     * @throws MCRUsageException if multiple entries match, the base DN does not exist,
     *                           or an LDAP error occurs
     */
    public Optional<String> resolveDn(String uid) {
        DirContext ctx = null;
        try {
            ctx = new InitialDirContext(env);
            final SearchControls controls = new SearchControls();
            controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            controls.setReturningAttributes(new String[0]);
            String filter = userIdFilterTemplate.replace("{0}", escapeLdap(uid));
            final NamingEnumeration<SearchResult> results = ctx.search(baseDn, filter, controls);
            if (!results.hasMore()) {
                return Optional.empty();
            }
            final String userDn = results.next().getNameInNamespace();
            if (results.hasMore()) {
                throw new MCRUsageException("Ambiguous LDAP filter, multiple entries found for: " + filter);
            }
            return Optional.of(userDn);
        } catch (NameNotFoundException e) {
            throw new MCRUsageException("LDAP DN not found: " + baseDn, e);
        } catch (NamingException e) {
            throw new MCRUsageException("Could not search LDAP", e);
        } finally {
            closeQuietly(ctx);
        }
    }

    /**
     * Returns all attributes of the LDAP entry identified by the given DN.
     *
     * @param dn the fully qualified DN of the entry
     * @return map of attribute ID to list of values
     * @throws MCRUsageException if the DN does not exist or an LDAP error occurs
     */
    public Map<String, List<String>> findAttributes(String dn) {
        DirContext ctx = null;
        try {
            ctx = new InitialDirContext(env);
            final Attributes attrs = ctx.getAttributes(dn);
            final Map<String, List<String>> attributeMap = new HashMap<>();

            final NamingEnumeration<String> ids = attrs.getIDs();
            while (ids.hasMore()) {
                final String id = ids.next();
                final Attribute attr = attrs.get(id);

                final List<String> values = new ArrayList<>();
                final NamingEnumeration<?> all = attr.getAll();
                while (all.hasMore()) {
                    values.add(all.next().toString());
                }
                attributeMap.put(id, values);
            }
            return attributeMap;
        } catch (NameNotFoundException e) {
            throw new MCRUsageException("LDAP DN not found: " + dn, e);
        } catch (NamingException e) {
            throw new MCRUsageException("Could not fetch attributes for DN: " + dn, e);
        } finally {
            closeQuietly(ctx);
        }
    }

    private static void closeQuietly(DirContext ctx) {
        if (ctx != null) {
            try {
                ctx.close();
            } catch (Exception e) {
                LOGGER.warn("Failed to close LDAP context", e);
            }
        }
    }

    private static String escapeLdap(String value) {
        return value
            .replace("\\", "\\5c")
            .replace("*",  "\\2a")
            .replace("(",  "\\28")
            .replace(")",  "\\29")
            .replace("\0", "\\00");
    }

    /**
     * LDAP connection configuration including provider URL, authentication, and timeouts.
     *
     * @param providerUrl the LDAP server URL (e.g. {@code ldap://host:389})
     * @param securitySettings authentication configuration
     * @param connectTimeoutMillis connection timeout in milliseconds
     * @param readTimeoutMillis read timeout in milliseconds
     */
    public record ConnectionSettings(
        String providerUrl,
        SecuritySettings securitySettings,
        Integer connectTimeoutMillis,
        Integer readTimeoutMillis) {

        public Map<String, String> toEnv() {
            final Map<String, String> env = new HashMap<>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.PROVIDER_URL, providerUrl);
            env.putAll(securitySettings.toLdapSettings());
            env.put("com.sun.jndi.ldap.connect.timeout", connectTimeoutMillis.toString());
            env.put("com.sun.jndi.ldap.read.timeout", readTimeoutMillis.toString());
            return env;
        }
    }

    /**
     * Defines different LDAP security (authentication) configurations.
     */
    public interface SecuritySettings {

        Map<String, String> toLdapSettings();

        /** Supported LDAP authentication types. */
        enum Authentication {
            /** No authentication (anonymous bind). */
            NONE,
            /** External SASL authentication. */
            EXTERNAL,
            /** Simple authentication (username/password). */
            SIMPLE
        }

        /** Supported security protocols. */
        enum Protocol {
            /** Clear text. */
            PLAIN,
            /** Secure SSL. */
            SSL
        }

        /**
         * No authentication (anonymous).
         *
         * @see <a href="https://datatracker.ietf.org/doc/html/rfc4513#section-5.1.1">RFC 4513 / 5.1.1</a>
         */
        record None(Protocol protocol) implements SecuritySettings {
            @Override
            public Map<String, String> toLdapSettings() {
                Map<String, String> settings = new HashMap<>();
                settings.put(Context.SECURITY_AUTHENTICATION, "none");
                if (protocol == Protocol.SSL) {
                    settings.put(Context.SECURITY_PROTOCOL, "ssl");
                }
                return settings;
            }
        }

        /**
         * External SASL authentication.
         *
         * @see <a href="https://datatracker.ietf.org/doc/html/rfc4513#section-5.2.3">RFC 4513 / 5.2.3</a>
         */
        record External() implements SecuritySettings {
            @Override
            public Map<String, String> toLdapSettings() {
                return Map.of(
                    Context.SECURITY_AUTHENTICATION, "EXTERNAL",
                    Context.SECURITY_PROTOCOL, "ssl");
            }
        }

        /**
         * Simple authentication (clear-text password).
         *
         * @param protocol the security protocol (PLAIN or SSL)
         * @param principal the username or DN
         * @param credentials the password
         * @see <a href="https://datatracker.ietf.org/doc/html/rfc4513#section-5.1.3">RFC 4513 / 5.1.3</a>
         */
        record Simple(Protocol protocol, String principal, String credentials) implements SecuritySettings {
            @Override
            public Map<String, String> toLdapSettings() {
                Map<String, String> settings = new HashMap<>();
                settings.put(Context.SECURITY_AUTHENTICATION, "simple");
                settings.put(Context.SECURITY_PRINCIPAL, principal);
                settings.put(Context.SECURITY_CREDENTIALS, credentials);
                if (protocol == Protocol.SSL) {
                    settings.put(Context.SECURITY_PROTOCOL, "ssl");
                }
                return settings;
            }
        }
    }

    /**
     * Factory for creating {@link MCRLDAPClient} instances from configuration properties.
     */
    public static class Factory implements Supplier<MCRLDAPClient> {

        @MCRProperty(name = "ProviderUrl")
        public String providerUrl;

        @MCRProperty(name = "SecurityAuthentication")
        public String securityAuthentication;

        @MCRProperty(name = "SecurityProtocol")
        public String securityProtocol;

        @MCRProperty(name = "SecurityPrincipal", required = false)
        public String securityPrincipal;

        @MCRProperty(name = "SecurityCredentials", required = false)
        public String securityCredentials;

        @MCRProperty(name = "ConnectTimeout")
        public String connectTimeoutMillis;

        @MCRProperty(name = "ReadTimeout")
        public String readTimeoutMillis;

        @MCRProperty(name ="BaseDn")
        public String baseDn;

        @MCRProperty(name = "UserFilterTemplate")
        public String userIdFilterTemplate;

        @Override
        public MCRLDAPClient get() {
            return new MCRLDAPClient(buildConnectionSettings(), baseDn, userIdFilterTemplate);
        }

        private ConnectionSettings buildConnectionSettings() {
            return new ConnectionSettings(
                providerUrl,
                switch (SecuritySettings.Authentication.valueOf(securityAuthentication.toUpperCase(Locale.ROOT))) {
                    case NONE -> new SecuritySettings.None(getProtocol());
                    case EXTERNAL -> new SecuritySettings.External();
                    case SIMPLE ->
                        new SecuritySettings.Simple(getProtocol(), securityPrincipal, securityCredentials);
                },
                Integer.parseInt(connectTimeoutMillis),
                Integer.parseInt(readTimeoutMillis));
        }

        private SecuritySettings.Protocol getProtocol() {
            return SecuritySettings.Protocol.valueOf(securityProtocol.toUpperCase(Locale.ROOT));
        }
    }
}
