package de.gbv.reposis.user;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

import org.mycore.common.MCRException;
import org.mycore.common.MCRUserInformation;
import org.mycore.user2.MCRTransientUser;
import org.mycore.user2.MCRUser;

/**
 * A {@link MCRTransientUser} that is fully self-contained and does not
 * depend on realm-based attribute mapping.
 */
public class MCRStandaloneTransientUser extends MCRTransientUser {

    private final boolean initialized;

    /**
     * Creates a new standalone transient user from the given user data.
     *
     * @param userData the user data to create the user from
     */
    public MCRStandaloneTransientUser(MCRUserData userData) {
        super(wrap(userData));
        this.initialized = true;
        try {
            Method setRealm = MCRUser.class.getDeclaredMethod("setRealmID", String.class);
            setRealm.setAccessible(true);
            setRealm.invoke(this, userData.realmId());
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new MCRException(e);
        }
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
