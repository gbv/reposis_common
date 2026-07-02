package de.gbv.reposis.user.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.mycore.common.MCRJPATestCase;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserManager;

import de.gbv.reposis.user.MCRUserData;

public class MCRUserUpdateIfExistsStrategyTest extends MCRJPATestCase {

    private final MCRUserUpdateIfExistsStrategy strategy =
        new MCRUserUpdateIfExistsStrategy(new MCRUserUpdateOverwriteStrategy());

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();
        testProperties.put("MCR.user2.Realms.URI", "resource:realms.xml");
        return testProperties;
    }

    @Test
    public void shouldUpdateUserIfExists() {
        MCRUser existing = new MCRUser("foo");
        existing.setUserAttribute("department", "IT");
        MCRUserManager.createUser(existing);

        Map<String, String> attributes = new HashMap<>();
        attributes.put("department", "HR");
        MCRUserData userData = new MCRUserData("foo", "local", Set.of(), attributes);

        strategy.apply(userData);

        MCRUser persistedUser = MCRUserManager.getUser("foo");
        assertEquals("HR", persistedUser.getUserAttribute("department"));
    }

    @Test
    public void shouldNotCreateUserIfNotExists() {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("department", "HR");
        MCRUserData userData = new MCRUserData("foo", "local", Set.of(), attributes);

        strategy.apply(userData);

        assertNull(MCRUserManager.getUser("foo"));
    }
}
