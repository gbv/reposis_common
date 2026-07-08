package de.gbv.reposis.user.shibboleth;

import org.mycore.common.config.MCRConfiguration2;

import de.gbv.reposis.user.merger.MCRUserMerger;

/**
 * Factory for {@link MCRShibbolethUserMapper} and {@link MCRUserMerger}.
 * They will be configured in mycore.properties using the properties {@link #MCR_USER_MAPPER_CLASS} and {@link #MCR_USER_MERGER_CLASS}.
 *
 * @author Sebastian Hofmann
 */
public class MCRShibbolethUserActionFactory {

    public static final String MCR_USER_MAPPER_CLASS = "MCR.User.Shibboleth.Mapper";

    public static final String MCR_USER_MERGER_CLASS = "MCR.User.Shibboleth.Merger";

    public static final String MCR_USER_NEW_USER_HANDLER_CLASS = "MCR.User.Shibboleth.NewUserHandler";

    public static MCRShibbolethUserMapper getUserMapper() {
        return MCRConfiguration2.<MCRShibbolethUserMapper>getSingleInstanceOf(MCR_USER_MAPPER_CLASS)
            .orElseThrow(() -> MCRConfiguration2.createConfigurationException(MCR_USER_MAPPER_CLASS));
    }

    public static MCRUserMerger getUserMerger() {
        return MCRConfiguration2.<MCRUserMerger>getSingleInstanceOf(MCR_USER_MERGER_CLASS)
            .orElseThrow(() -> MCRConfiguration2.createConfigurationException(MCR_USER_MERGER_CLASS));
    }

    public static MCRShibbolethNewUserHandler getNewUserHandler() {
        return MCRConfiguration2.<MCRShibbolethNewUserHandler>getSingleInstanceOf(MCR_USER_NEW_USER_HANDLER_CLASS)
            .orElseThrow(() -> MCRConfiguration2.createConfigurationException(MCR_USER_NEW_USER_HANDLER_CLASS));
    }
}
