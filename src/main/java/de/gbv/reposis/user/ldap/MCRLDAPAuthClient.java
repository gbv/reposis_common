package de.gbv.reposis.user.ldap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRUsageException;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstance;
import org.mycore.common.config.annotation.MCRProperty;

/**
 * Low-level LDAP client that authenticates a user via direct bind and returns all attributes.
 * <p>
 * Authentication is performed as a simple LDAP bind using a principal derived from the
 * username via a configurable template. No service account or prior DN lookup is required.
 */
@MCRConfigurationProxy(proxyClass = MCRLDAPAuthClient.Factory.class)
public class MCRLDAPAuthClient {

    private static final Logger LOGGER = LogManager.getLogger();

    private final Hashtable<String, String> baseEnv;
    private final String principalTemplate;

    /**
     * Creates a new {@code MCRLDAPAuthClient}.
     *
     * @param connectionSettings base connection settings (URL, protocol, timeouts)
     * @param principalTemplate  template for building the bind principal;
     *                           {@code {0}} is replaced with the LDAP-escaped username
     */
    public MCRLDAPAuthClient(MCRLDAPConnectionSettings connectionSettings, String principalTemplate) {
        this.baseEnv = new Hashtable<>(connectionSettings.toEnv());
        this.principalTemplate = principalTemplate;
    }

    /**
     * Authenticates a user via direct LDAP bind and returns all attributes on success.
     * <p>
     * The bind principal is derived from the username using the configured template.
     * If the bind succeeds, all LDAP attributes of the entry are fetched and returned.
     *
     * @param username the login name (inserted into the principal template)
     * @param password the user's password
     * @return all LDAP attributes of the authenticated user or {@code null} if the credentials are invalid
     * @throws MCRUsageException if a connection or LDAP server error occurs
     */
    public MCRLDAPAuthResult authenticate(String username, String password) {
        final Hashtable<String, String> bindEnv = new Hashtable<>(baseEnv);
        String principal = buildPrincipal(username);
        bindEnv.put(Context.SECURITY_AUTHENTICATION, "simple");
        bindEnv.put(Context.SECURITY_PRINCIPAL, principal);
        bindEnv.put(Context.SECURITY_CREDENTIALS, password);
        DirContext ctx = null;
        try {
            ctx = new InitialDirContext(bindEnv);
            LOGGER.debug("Credentials verified for principal: {}", principal);
            Map<String, List<String>> attributes = getAttributes(ctx, principal);
            return new MCRLDAPAuthResult(attributes);
        } catch (AuthenticationException e) {
            return null;
        } catch (NamingException e) {
            throw new MCRUsageException("Could not connect to LDAP server", e);
        } finally {
            closeQuietly(ctx);
        }
    }

    private String buildPrincipal(String username) {
        return principalTemplate.replace("{0}", escapeLdap(username));
    }

    private Map<String, List<String>> getAttributes(DirContext ctx, String principal) {
        try {
            final Attributes attrs = ctx.getAttributes(principal);
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
            throw new MCRUsageException("LDAP DN not found: " + principal, e);
        } catch (NamingException e) {
            throw new MCRUsageException("Could not fetch attributes for DN: " + principal, e);
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
            .replace("*", "\\2a")
            .replace("(", "\\28")
            .replace(")", "\\29")
            .replace("\0", "\\00");
    }

    /**
     * Factory for creating {@link MCRLDAPAuthClient} instances from configuration properties.
     */
    public static class Factory implements Supplier<MCRLDAPAuthClient> {

        @MCRInstance(name = "ConnectionSettings", valueClass = MCRLDAPConnectionSettings.class)
        public MCRLDAPConnectionSettings connectionSettings;

        @MCRProperty(name = "PrincipalTemplate")
        public String principalTemplate;

        @Override
        public MCRLDAPAuthClient get() {
            return new MCRLDAPAuthClient(connectionSettings, principalTemplate);
        }
    }
}
