package de.gbv.reposis.user;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Set;

import org.mycore.common.MCRException;
import org.mycore.common.MCRUserInformation;
import org.mycore.user2.MCRTransientUser;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUser2Constants;

/**
 * A {@link MCRTransientUser} that is fully self-contained and does not
 * depend on realm-based attribute mapping.
 */
public class MCRStandaloneTransientUser extends MCRTransientUser {

    private static final String SYSTEM_ROLE_PREFIX = MCRUser2Constants.getRoleRootId() + ":";

    private final boolean initialized;

    /**
     * Creates a new standalone transient user from the given user data.
     *
     * @param userData the user data to create the user from
     */
    public MCRStandaloneTransientUser(MCRUserData userData) {
        super(wrap(userData));
        this.initialized = true;
        assignExternalRoles(userData.roles());
        try {
            // MCRUser#setRealmID(String) has package-private visibility, so reflection
            // is required to initialize the realm outside the package.
            Method setRealm = MCRUser.class.getDeclaredMethod("setRealmID", String.class);
            setRealm.setAccessible(true);
            setRealm.invoke(this, userData.realmId());
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new MCRException(e);
        }
    }

    private void assignExternalRoles(Set<String> roles) {
        roles.stream()
            .filter(role -> !isSystemRole(role))
            .forEach(this::assignRole);
    }

    /**
     * Checks whether a role is a system role (unqualified, or prefixed with the
     * system namespace). Logic adopted from {@code MCRRole}.
     */
    private boolean isSystemRole(String name) {
        return !name.contains(":") || name.startsWith(SYSTEM_ROLE_PREFIX);
    }

    @Override
    public String getUserAttribute(String attribute) {
        if (!initialized && Objects.equals("realmId", attribute)) {
            return null;
        }
        return super.getUserAttribute(attribute);
    }

    private static MCRUserInformation wrap(MCRUserData userData) {
        return new MCRUserInformation() {

            @Override
            public String getUserID() {
                return userData.userId();
            }

            @Override
            public boolean isUserInRole(String s) {
                return userData.roles().contains(s);
            }

            @Override
            public String getUserAttribute(String s) {
                return userData.attributes().get(s);
            }
        };
    }
}
