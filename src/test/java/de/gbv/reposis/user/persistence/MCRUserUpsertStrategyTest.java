package de.gbv.reposis.user.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.test.MCRJPAExtension;
import org.mycore.test.MyCoReTest;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserManager;

import de.gbv.reposis.user.MCRUserData;

@MyCoReTest
@ExtendWith(MCRJPAExtension.class)
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.user2.Realms.URI", string = "resource:realms.xml")
})
public class MCRUserUpsertStrategyTest {

    private final MCRUserUpsertStrategy strategy = new MCRUserUpsertStrategy(new MCRUserUpdateOverwriteStrategy());

    @Test
    public void shouldCreateUserIfNotExists() {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("department", "IT");
        MCRUserData userData = new MCRUserData("foo", "local", Set.of(), attributes);

        strategy.apply(userData);

        MCRUser persistedUser = MCRUserManager.getUser("foo");
        assertNotNull(persistedUser);
        assertEquals("IT", persistedUser.getUserAttribute("department"));
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
}
