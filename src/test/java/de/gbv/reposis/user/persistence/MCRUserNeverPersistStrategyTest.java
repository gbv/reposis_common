package de.gbv.reposis.user.persistence;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.test.MCRJPAExtension;
import org.mycore.test.MyCoReTest;
import org.mycore.user2.MCRTransientUser;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserManager;

import de.gbv.reposis.user.MCRUserData;

@MyCoReTest
@ExtendWith(MCRJPAExtension.class)
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.user2.Realms.URI", string = "resource:realms.xml")
})
public class MCRUserNeverPersistStrategyTest {

    private final MCRUserNeverPersistStrategy strategy = new MCRUserNeverPersistStrategy();

    @Test
    public void shouldReturnTransientUser() {
        MCRUserData userData = new MCRUserData("foo", "local", Set.of(), Map.of());

        MCRUser result = strategy.apply(userData);

        assertInstanceOf(MCRTransientUser.class, result);
    }

    @Test
    public void shouldNotPersistUser() {
        MCRUserData userData = new MCRUserData("foo", "local", Set.of(), Map.of());

        strategy.apply(userData);

        assertNull(MCRUserManager.getUser("foo"));
    }
}
