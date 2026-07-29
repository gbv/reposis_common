package de.gbv.reposis.user.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.Map;

import org.junit.Test;
import org.mycore.common.MCRJPATestCase;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserManager;

public class MCRUserNeverUpdateStrategyTest extends MCRJPATestCase {

    private final MCRUserNeverUpdateStrategy strategy = new MCRUserNeverUpdateStrategy();

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();
        testProperties.put("MCR.user2.Realms.URI", "resource:realms.xml");
        return testProperties;
    }

    @Test
    public void shouldReturnTheExistingUserInstanceUnmodified() {
        MCRUser existingUser = new MCRUser("foo");
        MCRUser incomingUser = new MCRUser("foo");

        MCRUser result = strategy.update(existingUser, incomingUser);

        assertSame(existingUser, result);
    }

    @Test
    public void shouldNotPersistChangesFromIncomingUser() {
        MCRUser existing = new MCRUser("foo");
        existing.setUserAttribute("department", "IT");
        MCRUserManager.createUser(existing);

        MCRUser existingUser = MCRUserManager.getUser("foo");
        MCRUser incomingUser = new MCRUser("foo");
        incomingUser.setUserAttribute("department", "HR");

        strategy.update(existingUser, incomingUser);

        MCRUser persistedUser = MCRUserManager.getUser("foo");
        assertEquals("IT", persistedUser.getUserAttribute("department"));
    }
}
