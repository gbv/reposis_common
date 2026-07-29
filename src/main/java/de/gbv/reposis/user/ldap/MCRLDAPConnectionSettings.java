package de.gbv.reposis.user.ldap;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import javax.naming.Context;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;

/**
 * Base LDAP connection settings shared across authentication and search clients.
 * <p>
 * Does not include bind credentials or authentication mode, as those depend on the
 * specific use case (e.g. direct bind authentication vs. anonymous or service account search).
 *
 * @param providerUrl the LDAP server URL
 * @param protocol the transport security protocol
 * @param connectTimeoutMillis connection timeout in milliseconds
 * @param readTimeoutMillis read timeout in milliseconds
 */
@MCRConfigurationProxy(proxyClass = MCRLDAPConnectionSettings.Factory.class)
public record MCRLDAPConnectionSettings(
    String providerUrl,
    Protocol protocol,
    Integer connectTimeoutMillis,
    Integer readTimeoutMillis) {

    /**
     * Builds the base JNDI environment for this connection.
     * <p>
     * Does not set {@link Context#SECURITY_AUTHENTICATION} or bind credentials;
     * callers must add those depending on the desired authentication mode.
     *
     * @return the base JNDI environment
     */
    public Map<String, String> toEnv() {
        final Map<String, String> env = new HashMap<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, providerUrl);
        if (protocol == Protocol.SSL) {
            env.put(Context.SECURITY_PROTOCOL, "ssl");
        }
        env.put("com.sun.jndi.ldap.connect.timeout", connectTimeoutMillis.toString());
        env.put("com.sun.jndi.ldap.read.timeout", readTimeoutMillis.toString());
        return env;
    }

    /**
     * Transport security protocol for the LDAP connection.
     */
    public enum Protocol {PLAIN, SSL}

    /**
     * Factory for creating {@link MCRLDAPConnectionSettings} instances from configuration properties.
     */
    public static class Factory implements Supplier<MCRLDAPConnectionSettings> {

        private static final String DEFAULT_PROP_PREFIX = "MCRLDAPConnectionSettings.Default.";

        @MCRProperty(name = "ProviderUrl")
        public String providerUrl;

        @MCRProperty(name = "Protocol")
        public String protocol;

        @MCRProperty(name = "ConnectTimeout", defaultName = DEFAULT_PROP_PREFIX + "ConnectTimeout")
        public String connectTimeoutMillis;

        @MCRProperty(name = "ReadTimeout", defaultName = DEFAULT_PROP_PREFIX + "ReadTimeout")
        public String readTimeoutMillis;

        @Override
        public MCRLDAPConnectionSettings get() {
            return new MCRLDAPConnectionSettings(
                providerUrl,
                MCRLDAPConnectionSettings.Protocol.valueOf(protocol.toUpperCase(Locale.ROOT)),
                Integer.parseInt(connectTimeoutMillis),
                Integer.parseInt(readTimeoutMillis)
            );
        }
    }
}
