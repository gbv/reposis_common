package de.gbv.reposis.user.shibboleth;

import org.mycore.user2.MCRUser;

import de.gbv.reposis.user.merger.MCRUserMerger;

/**
 * The default user merger which does not merge any attributes.
 *
 * @author Sebastian Hofmann
 */
public class MCRDefaultConfigurableShibbolethUserMerger implements MCRUserMerger {

    @Override
    public boolean merge(MCRUser existing, MCRUser userFromRequest) {
        return false;
    }

}
