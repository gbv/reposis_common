package de.gbv.reposis.user.shibboleth;

import org.mycore.user2.MCRUser;

/**
 * The interface for a user merger that merges a MCRUser from a Shibboleth request (generated with {@link MCRShibbolethUserMapper}) with an existing MCRUser in the Database.
 *
 * @author Sebastian Hofmann
 */
public interface MCRShibbolethUserMerger {
    /**
     * Merges the user from the request with the existing user in the database.
     * @param existing the existing user in the database
     * @param userFromRequest the user from the request. built with {@link MCRShibbolethUserMapper}
     * @return true if the user was merged, false if the user was not merged
     */
    boolean merge(MCRUser existing, MCRUser userFromRequest);
}
