package de.gbv.reposis.user.merger;

import org.mycore.user2.MCRUser;

/**
 * Updates one user with data from another user.
 */
public interface MCRUserMerger {

    /**
     * Updates {@code target} with data from {@code source}.
     *
     * @param target the user to update
     * @param source the user to update from
     * @return {@code true} if {@code target} was modified as a result of the merge,
     *         {@code false} otherwise
     */
    boolean merge(MCRUser target, MCRUser source);
}
