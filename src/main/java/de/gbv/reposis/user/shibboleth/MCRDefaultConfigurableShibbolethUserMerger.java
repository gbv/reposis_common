package de.gbv.reposis.user.shibboleth;

import org.mycore.user2.MCRUser;

/**
 * The default user merger which does not merge any attributes.
 *
 * @author Sebastian Hofmann
 */
public class MCRDefaultConfigurableShibbolethUserMerger implements MCRShibbolethUserMerger {

    @Override
    public boolean merge(MCRUser existing, MCRUser userFromRequest) {
        return false;
    }

}
