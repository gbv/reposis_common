package de.gbv.reposis.user;

import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRUserInformation;
import org.mycore.user2.MCRRoleManager;
import org.mycore.user2.MCRUser;

/**
 * Factory for creating {@link MCRUser} instances from {@link MCRUserData}.
 */
public final class MCRUserFactory {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final Set<String> EXCLUDED_ATTRIBUTE_NAMES = Set.of(MCRUserInformation.ATT_EMAIL,
        MCRUserInformation.ATT_REAL_NAME);

    private MCRUserFactory() {
    }

    /**
     * Creates a new {@link MCRUser} from the given user data.
     *
     * @param userData the user data to create the user from
     * @return a new {@link MCRUser} instance populated with the given data
     */
    public static MCRUser createUser(MCRUserData userData) {
        return createUser(userData.username(), userData.realmId(), userData.roles(), userData.attributes());
    }

    private static MCRUser createUser(String username, String realmId, Set<String> roles,
        Map<String, String> attributes) {
        MCRUser user = new MCRUser(username, realmId);

        if (attributes.containsKey(MCRUserInformation.ATT_EMAIL)) {
            user.setEMail(attributes.get(MCRUserInformation.ATT_EMAIL));
        }

        if (attributes.containsKey(MCRUserInformation.ATT_REAL_NAME)) {
            user.setRealName(attributes.get(MCRUserInformation.ATT_REAL_NAME));
        }

        assignRoles(user, roles);
        applyAttributes(user, attributes);

        return user;
    }

    private static void assignRoles(MCRUser user, Set<String> roles) {
        for (String role : roles) {
            if (MCRRoleManager.getRole(role) != null) {
                user.assignRole(role);
            } else {
                LOGGER.warn("Ignoring unknown role '{}' for user '{}'", role, user.getUserID());
            }
        }
    }

    private static void applyAttributes(MCRUser user, Map<String, String> attributes) {
        attributes.entrySet().stream()
            .filter(entry -> !EXCLUDED_ATTRIBUTE_NAMES.contains(entry.getKey()))
            .forEach(entry -> user.setUserAttribute(entry.getKey(), entry.getValue()));
    }
}
