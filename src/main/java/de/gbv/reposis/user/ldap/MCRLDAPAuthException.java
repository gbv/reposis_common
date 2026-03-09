package de.gbv.reposis.user.ldap;

import org.mycore.common.MCRException;

/**
 * Thrown by {@link MCRLDAPAuthService} when a user's credentials could not be verified
 * against the LDAP server.
 */
public class MCRLDAPAuthException extends MCRException {

    /**
     * Creates a new {@code MCRLDAPAuthenticationException} with the given message.
     *
     * @param message a description of the authentication failure
     */
    public MCRLDAPAuthException(String message) {
        super(message);
    }
}
