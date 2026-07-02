package de.gbv.reposis.user.merger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserAttribute;

/**
 * A {@link MCRUserMerger} that copies attributes present on {@code source} but
 * absent on {@code target} onto {@code target}. Attributes already present on {@code target}
 * are left untouched, i.e. {@code target}'s own values always take precedence over {@code source}.
 * <p>
 * This merger only deals with attributes and does not touch roles.
 */
public class MCRUserAttributeGapFillMerger implements MCRUserMerger {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public boolean merge(MCRUser source, MCRUser target) {
        boolean changed = false;
        for (MCRUserAttribute attribute : source.getAttributes()) {
            if (target.getUserAttribute(attribute.getName()) == null) {
                target.setUserAttribute(attribute.getName(), attribute.getValue());
                changed = true;
                LOGGER.debug("Added missing attribute '{}' for user {}.", attribute.getName(), source.getUserID());
            }
        }
        return changed;
    }
}
