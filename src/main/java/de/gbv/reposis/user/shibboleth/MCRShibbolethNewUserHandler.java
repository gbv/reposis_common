package de.gbv.reposis.user.shibboleth;

import org.mycore.user2.MCRUser;

/**
 * This interface is used to handle new users which are created by the {@link MCRShibbolethLoginServlet2}.
 * I can be used to email to the administrator.
 */
public interface MCRShibbolethNewUserHandler {
    void handleNewUser(MCRUser user);
}
