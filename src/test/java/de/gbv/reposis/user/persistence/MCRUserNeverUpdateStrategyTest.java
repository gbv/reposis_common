package de.gbv.reposis.user.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.test.MCRJPAExtension;
import org.mycore.test.MyCoReTest;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserManager;

@MyCoReTest
@ExtendWith(MCRJPAExtension.class)
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.user2.Realms.URI", string = "resource:realms.xml")
})
public class MCRUserNeverUpdateStrategyTest {

    private final MCRUserNeverUpdateStrategy strategy = new MCRUserNeverUpdateStrategy();

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
