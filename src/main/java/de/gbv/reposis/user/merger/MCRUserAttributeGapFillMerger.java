package de.gbv.reposis.user.merger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserAttribute;

/**
 * A {@link MCRUserMerger} that copies attributes, e-mail address, and real name present
 * on {@code source} but absent on {@code target} onto {@code target}. Values already
 * present on {@code target} are left untouched, i.e. {@code target}'s own values take
 * precedence over {@code source} for these fields.
 * <p>
 * As an exception to this gap-fill behavior, if {@code source} is disabled or locked,
 * {@code target} is also set to disabled or locked, respectively, regardless of its
 * current state. This ensures a disabled or locked state cannot be lost during a merge.
 * <p>
 * This merger does not touch roles.
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
        if (target.getEMailAddress() == null && source.getEMailAddress() != null) {
            target.setEMail(source.getEMailAddress());
            changed = true;
            LOGGER.debug("Added missing e-mail address for user {}.", source.getUserID());
        }
        if (target.getRealName() == null && source.getRealName() != null) {
            target.setRealName(source.getRealName());
            changed = true;
            LOGGER.debug("Added missing real name for user {}.", source.getUserID());
        }
        if (source.isDisabled() && !target.isDisabled()) {
            target.setDisabled(true);
            changed = true;
            LOGGER.info("Marked user {} as disabled due to disabled source account.", source.getUserID());
        }
        if (source.isLocked() && !target.isLocked()) {
            target.setLocked(true);
            changed = true;
            LOGGER.info("Marked user {} as locked due to locked source account.", source.getUserID());
        }
        return changed;
    }
}
