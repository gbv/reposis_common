package de.gbv.reposis.user.ldap.dn;

import java.util.Hashtable;
import java.util.Optional;
import java.util.function.Supplier;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapName;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRUsageException;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstance;
import org.mycore.common.config.annotation.MCRProperty;

import de.gbv.reposis.user.ldap.MCRLDAPConnectionSettings;

/**
 * Resolves a user's distinguished name (DN) via an LDAP search.
 * <p>
 * The search is performed either anonymously or, if bind credentials are configured,
 * using a service account. Exactly one matching entry is expected; if none or multiple
 * entries are found, the resolution fails.
 */
@MCRConfigurationProxy(proxyClass = MCRLDAPSearchDNResolver.Factory.class)
public class MCRLDAPSearchDNResolver implements MCRLDAPDNResolver {

    private static final Logger LOGGER = LogManager.getLogger();

    private final MCRLDAPConnectionSettings connectionSettings;
    private final String baseDN;
    private final String searchFilterTemplate;
    private final String bindDN;
    private final String bindPassword;

    /**
     * Creates a new {@code MCRLDAPSearchDNResolver}.
     *
     * @param connectionSettings base connection settings (URL, protocol, timeouts)
     * @param baseDN the base DN to search under
     * @param searchFilterTemplate the search filter template, with {@code {0}} as placeholder for the username
     * @param bindDN the DN of the service account, or {@code null}/blank for an anonymous search
     * @param bindPassword the password of the service account, or {@code null} for an anonymous search
     */
    public MCRLDAPSearchDNResolver(MCRLDAPConnectionSettings connectionSettings, String baseDN,
        String searchFilterTemplate, String bindDN, String bindPassword) {
        this.connectionSettings = connectionSettings;
        this.baseDN = baseDN;
        this.searchFilterTemplate = searchFilterTemplate;
        this.bindDN = bindDN;
        this.bindPassword = bindPassword;
    }

    @Override
    public Optional<LdapName> resolve(String username) {
        if (!searchFilterTemplate.contains("{0}")) {
            throw new MCRUsageException(
                "SearchFilter must contain a '{0}' placeholder for the username: " + searchFilterTemplate);
        }
        String filter = searchFilterTemplate.replace("{0}", escapeSearchFilter(username));

        DirContext ctx = null;
        NamingEnumeration<SearchResult> results = null;
        try {
            ctx = new InitialDirContext(buildEnv());

            SearchControls controls = new SearchControls();
            controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            controls.setReturningAttributes(new String[0]);
            controls.setCountLimit(2);

            results = ctx.search(baseDN, filter, controls);
            if (!results.hasMore()) {
                LOGGER.debug("No LDAP entry found for username: {}", username);
                return Optional.empty();
            }

            SearchResult result = results.next();
            if (results.hasMore()) {
                LOGGER.warn("Multiple LDAP entries found for username: {}", username);
                return Optional.empty();
            }

            return Optional.of(new LdapName(result.getNameInNamespace()));
        } catch (NamingException e) {
            throw new MCRUsageException("Could not search LDAP for username: " + username, e);
        } finally {
            closeQuietly(results);
            closeQuietly(ctx);
        }
    }

    private Hashtable<String, String> buildEnv() {
        Hashtable<String, String> env = new Hashtable<>(connectionSettings.toEnv());
        if (bindDN != null && !bindDN.isBlank()) {
            if (bindPassword == null || bindPassword.isBlank()) {
                throw new MCRUsageException("BindDN is set but BindPassword is missing");
            }
            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            env.put(Context.SECURITY_PRINCIPAL, bindDN);
            env.put(Context.SECURITY_CREDENTIALS, bindPassword);
        } else {
            env.put(Context.SECURITY_AUTHENTICATION, "none");
        }
        return env;
    }

    private static String escapeSearchFilter(String value) {
        StringBuilder sb = new StringBuilder();
        for (char c : value.toCharArray()) {
            switch (c) {
                case '\\' -> sb.append("\\5c");
                case '*' -> sb.append("\\2a");
                case '(' -> sb.append("\\28");
                case ')' -> sb.append("\\29");
                case '\0' -> sb.append("\\00");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    private static void closeQuietly(NamingEnumeration<?> enumeration) {
        if (enumeration != null) {
            try {
                enumeration.close();
            } catch (Exception e) {
                LOGGER.warn("Failed to close LDAP search results", e);
            }
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

    /**
     * Factory for creating {@link MCRLDAPSearchDNResolver} instances from configuration properties.
     */
    public static class Factory implements Supplier<MCRLDAPSearchDNResolver> {

        @MCRInstance(name = "ConnectionSettings", valueClass = MCRLDAPConnectionSettings.class)
        public MCRLDAPConnectionSettings connectionSettings;

        @MCRProperty(name = "BaseDN")
        public String baseDN;

        @MCRProperty(name = "SearchFilter")
        public String searchFilter;

        @MCRProperty(name = "BindDN", required = false)
        public String bindDN;

        @MCRProperty(name = "BindPassword", required = false)
        public String bindPassword;

        @Override
        public MCRLDAPSearchDNResolver get() {
            return new MCRLDAPSearchDNResolver(connectionSettings, baseDN, searchFilter, bindDN, bindPassword);
        }
    }
}
