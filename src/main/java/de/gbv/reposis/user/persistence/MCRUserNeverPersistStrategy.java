package de.gbv.reposis.user.persistence;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserManager;

import de.gbv.reposis.user.MCRStandaloneTransientUser;
import de.gbv.reposis.user.MCRUserData;

/**
 * User persistence strategy that never persists the user in the database.
 * <p>
 * If a user with the same ID already exists, it is deleted. The returned
 * user is a {@link MCRStandaloneTransientUser}, which is safe to use
 * without ever being saved.
 */
public class MCRUserNeverPersistStrategy implements MCRUserPersistenceStrategy {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public MCRUser apply(MCRUserData userData) {
        String userId = userData.userId();
        LOGGER.debug("Applying never-persist strategy for user {}", userId);
        if (MCRUserManager.exists(userData.username(), userData.realmId())) {
            MCRUserManager.deleteUser(userId);
            LOGGER.info("User {} has been deleted", userId);
        }
        return new MCRStandaloneTransientUser(userData);
    }
}
