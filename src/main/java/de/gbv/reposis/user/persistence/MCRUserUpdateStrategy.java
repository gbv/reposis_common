package de.gbv.reposis.user.persistence;

import java.util.Objects;

import org.mycore.common.MCRException;
import org.mycore.user2.MCRUser;

/**
 * Strategy for updating an existing {@link MCRUser} with data from an incoming user.
 */
public interface MCRUserUpdateStrategy {

    /**
     * Updates the given existing user using the data from the incoming user.
     *
     * @param existingUser the user currently stored in the persistence layer
     * @param incomingUser the incoming user containing the new data
     * @return the updated user instance to be used after the update operation
     */
    MCRUser update(MCRUser existingUser, MCRUser incomingUser);

    /**
     * Ensures that the existing user and the incoming user share the same user ID.
     *
     * @param existingUser the user currently stored in the persistence layer
     * @param incomingUser the incoming user containing the new data
     * @throws MCRException if the user IDs do not match
     */
    default void requireSameUserID(MCRUser existingUser, MCRUser incomingUser) {
        if (!Objects.equals(existingUser.getUserID(), incomingUser.getUserID())) {
            throw new MCRException("User ids do not match: existing=" + existingUser.getUserID()
                + ", incoming=" + incomingUser.getUserID());
        }
    }
}
