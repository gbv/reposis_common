package de.gbv.reposis.user.persistence;

import org.mycore.user2.MCRUser;

import de.gbv.reposis.user.MCRUserData;

/**
 * Strategy for handling persistence behavior of {@link MCRUser} instances.
 * <p>
 * Depending on the implementation, applying this strategy may create,
 * update, or delete the persisted user, or leave persistence untouched
 * entirely.
 */
public interface MCRUserPersistenceStrategy {

    /**
     * Creates the user described by the given user data and applies this
     * strategy's persistence behavior to it.
     *
     * @param userData the incoming user data
     * @return the user instance to be used after the strategy has been applied
     */
    MCRUser apply(MCRUserData userData);
}
