package de.gbv.reposis.user.persistence;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.user2.MCRUser;

/**
 * User update strategy that never updates the existing user.
 * <p>
 * The {@code incomingUser} is ignored entirely; the {@code existingUser} is
 * returned unchanged.
 */
public class MCRUserNeverUpdateStrategy implements MCRUserUpdateStrategy {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public MCRUser update(MCRUser existingUser, MCRUser incomingUser) {
        LOGGER.debug("Skipping update for user {}", existingUser.getUserID());
        return existingUser;
    }
}
