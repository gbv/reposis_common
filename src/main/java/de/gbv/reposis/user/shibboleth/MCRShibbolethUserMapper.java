package de.gbv.reposis.user.shibboleth;

import org.mycore.user2.MCRUser;

/**
 * The interface for a user mapper that maps attributes from a Shibboleth request to a MCRUser.
 *
 * @author Sebastian Hofmann
 */
public interface MCRShibbolethUserMapper {

    /**
     * Maps the attributes from the request to a MCRUser.
     * @param remoteUser the remote user from the request
     * @param requestAttributeResolver the resolver for the attributes from the request
     * @return the mapped MCRUser
     */
    MCRUser mapAttributes(String remoteUser, MCRRequestAttributeResolver requestAttributeResolver);

}
