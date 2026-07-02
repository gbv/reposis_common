package de.gbv.reposis.user.persistence;

import java.util.Objects;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstance;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserManager;

import de.gbv.reposis.user.MCRStandaloneTransientUser;
import de.gbv.reposis.user.MCRUserData;
import de.gbv.reposis.user.MCRUserFactory;

/**
 * User persistence strategy that updates an existing user, but never creates a new one.
 */
@MCRConfigurationProxy(proxyClass = MCRUserUpdateIfExistsStrategy.Factory.class)
public class MCRUserUpdateIfExistsStrategy implements MCRUserPersistenceStrategy {

    private static final Logger LOGGER = LogManager.getLogger();

    private final MCRUserUpdateStrategy updateStrategy;

    /**
     * Creates a new strategy that updates existing users using the given update strategy.
     *
     * @param updateStrategy the strategy used to update an existing user
     */
    public MCRUserUpdateIfExistsStrategy(MCRUserUpdateStrategy updateStrategy) {
        this.updateStrategy = Objects.requireNonNull(updateStrategy, "updateStrategy must not be null");
    }

    @Override
    public MCRUser apply(MCRUserData userData) {
        String userId = userData.userId();
        LOGGER.debug("Applying update-if-exists strategy for user {}", userId);
        MCRUser existingUser = MCRUserManager.getUser(userId);
        if (existingUser == null) {
            LOGGER.debug("User {} does not exist - skipping update", userId);
            return new MCRStandaloneTransientUser(userData);
        }
        MCRUser user = MCRUserFactory.createUser(userData);
        LOGGER.debug("User {} exists - updating using {}", userId, updateStrategy.getClass().getSimpleName());
        return updateStrategy.update(existingUser, user);
    }

    /**
     * Factory for creating {@link MCRUserUpdateIfExistsStrategy} instances from configuration properties.
     */
    public static class Factory implements Supplier<MCRUserUpdateIfExistsStrategy> {

        @MCRInstance(name = "UpdateStrategy", valueClass =  MCRUserUpdateStrategy.class)
        public MCRUserUpdateStrategy updateStrategy;

        @Override
        public MCRUserUpdateIfExistsStrategy get() {
            return new MCRUserUpdateIfExistsStrategy(updateStrategy);
        }
    }
}
