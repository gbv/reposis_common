package de.gbv.reposis.user.persistence;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;
import org.mycore.common.MCRJPATestCase;
import org.mycore.common.MCRUserInformation;
import org.mycore.user2.MCRTransientUser;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserManager;

import de.gbv.reposis.user.MCRUserData;

public class MCRUserNeverPersistStrategyTest extends MCRJPATestCase {

    private final MCRUserNeverPersistStrategy strategy = new MCRUserNeverPersistStrategy();

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();
        testProperties.put("MCR.user2.Realms.URI", "resource:realms.xml");
        return testProperties;
    }

    @Test
    public void shouldReturnTransientUser() {
        MCRUserData userData = new MCRUserData("foo", "local", Set.of(), Map.of());

        MCRUser result = strategy.apply(userData);

        assertTrue(result instanceof MCRTransientUser);
    }

    @Test
    public void shouldNotPersistUser() {
        MCRUserData userData = new MCRUserData("foo", "local", Set.of(), Map.of());

        strategy.apply(userData);

        assertNull(MCRUserManager.getUser("foo"));
    }
}
