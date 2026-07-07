package de.gbv.reposis.user.persistence;

import java.util.Objects;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstance;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserManager;

import de.gbv.reposis.user.MCRUserData;
import de.gbv.reposis.user.MCRUserFactory;

/**
 * User persistence strategy that always persists the given user.
 */
@MCRConfigurationProxy(proxyClass = MCRUserUpsertStrategy.Factory.class)
public class MCRUserUpsertStrategy implements MCRUserPersistenceStrategy {

    private static final Logger LOGGER = LogManager.getLogger();

    private final MCRUserUpdateStrategy updateStrategy;

    /**
     * Creates a new always-persist user strategy.
     *
     * @param updateStrategy strategy used to update an existing user
     */
    public MCRUserUpsertStrategy(MCRUserUpdateStrategy updateStrategy) {
        this.updateStrategy = Objects.requireNonNull(updateStrategy, "updateStrategy must not be null");
    }

    @Override
    public MCRUser apply(MCRUserData userData) {
        String userId = userData.userId();
        LOGGER.debug("Applying upsert strategy for user {}", userId);
        MCRUser user = MCRUserFactory.createUser(userData);
        MCRUser existingUser = MCRUserManager.getUser(userId);
        if (existingUser != null) {
            LOGGER.debug("User {} already exists - delegating update to strategy", userId);
            return updateStrategy.update(existingUser, user);
        }
        MCRUserManager.createUser(user);
        LOGGER.info("Created User {}", userId);
        return MCRUserManager.getUser(userId);
    }

    /**
     * Factory for creating {@link MCRUserUpsertStrategy} instances from configuration properties.
     */
    public static class Factory implements Supplier<MCRUserUpsertStrategy> {

        @MCRInstance(name = "UpdateStrategy", valueClass = MCRUserUpdateStrategy.class)
        public MCRUserUpdateStrategy updateStrategy;

        @Override
        public MCRUserUpsertStrategy get() {
            return new MCRUserUpsertStrategy(updateStrategy);
        }
    }
}
