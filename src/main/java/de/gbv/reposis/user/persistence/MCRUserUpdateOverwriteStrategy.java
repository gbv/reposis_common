package de.gbv.reposis.user.persistence;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserManager;

/**
 * Existing user update strategy that completely overwrites an existing user with the incoming user.
 */
public class MCRUserUpdateOverwriteStrategy implements MCRUserUpdateStrategy {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public MCRUser update(MCRUser existingUser, MCRUser incomingUser) {
        requireSameUserID(existingUser, incomingUser);

        LOGGER.debug("Overwriting existing user {} with incoming user data", existingUser.getUserID());
        MCRUserManager.updateUser(incomingUser);
        LOGGER.info("Overwrote user {}", existingUser.getUserID());
        return MCRUserManager.getUser(existingUser.getUserID());
    }
}
