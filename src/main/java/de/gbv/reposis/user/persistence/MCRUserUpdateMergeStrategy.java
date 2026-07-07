package de.gbv.reposis.user.persistence;

import java.util.Objects;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstance;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserManager;

import de.gbv.reposis.user.merger.MCRUserMerger;

/**
 * Existing user update strategy that merges the existing user in an incoming user.
 * <p>
 * User roles are always updated during the merge process.
 * Therefore, the user is persisted regardless of whether the merge reports other changes.
 */
@MCRConfigurationProxy(proxyClass = MCRUserUpdateMergeStrategy.Factory.class)
public class MCRUserUpdateMergeStrategy implements MCRUserUpdateStrategy {

    private static final Logger LOGGER = LogManager.getLogger();

    private final MCRUserMerger userMerger;

    public MCRUserUpdateMergeStrategy(MCRUserMerger userMerger) {
        this.userMerger = Objects.requireNonNull(userMerger, "userMerger must not be null");
    }

    @Override
    public MCRUser update(MCRUser existingUser, MCRUser incomingUser) {
        requireSameUserID(existingUser, incomingUser);

        LOGGER.debug("Merging incoming user data into existing user {}", existingUser.getUserID());
        boolean changed = userMerger.merge(existingUser, incomingUser);
        if (changed) {
            LOGGER.info("Merged and updated user {}", existingUser.getUserID());
        } else {
            LOGGER.debug("No changes detected while merging user {}", existingUser.getUserID());
        }
        MCRUserManager.updateUser(incomingUser);
        return MCRUserManager.getUser(existingUser.getUserID());
    }

    /**
     * Factory for creating {@link MCRUserUpdateMergeStrategy} instances from configuration properties.
     */
    public static class Factory implements Supplier<MCRUserUpdateMergeStrategy> {

        @MCRInstance(name = "UserMerger", valueClass =  MCRUserMerger.class)
        public MCRUserMerger userMerger;

        @Override
        public MCRUserUpdateMergeStrategy get() {
            return new MCRUserUpdateMergeStrategy(userMerger);
        }
    }
}
