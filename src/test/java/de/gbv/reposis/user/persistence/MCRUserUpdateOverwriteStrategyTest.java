package de.gbv.reposis.user.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.common.MCRException;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.impl.MCRCategoryDAOImplTest;
import org.mycore.test.MCRJPAExtension;
import org.mycore.test.MyCoReTest;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserManager;

@MyCoReTest
@ExtendWith(MCRJPAExtension.class)
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.user2.Realms.URI", string = "resource:realms.xml")
})
public class MCRUserUpdateOverwriteStrategyTest {

    private final MCRUserUpdateOverwriteStrategy strategy = new MCRUserUpdateOverwriteStrategy();

    @BeforeEach
    public void setUp() throws Exception {
        MCRCategory groupsCategory = MCRCategoryDAOImplTest.loadClassificationResource("/mcr-roles.xml");
        MCRCategoryDAO DAO = MCRCategoryDAO.obtainInstance();
        DAO.addCategory(null, groupsCategory);
    }

    @Test
    public void shouldOverwriteAttributes() {
        MCRUser existing = new MCRUser("foo");
        existing.setUserAttribute("department", "IT");
        existing.setUserAttribute("phone", "1234");
        MCRUserManager.createUser(existing);

        MCRUser existingUser = MCRUserManager.getUser("foo");
        MCRUser incomingUser = new MCRUser("foo");
        incomingUser.setUserAttribute("department", "HR");

        strategy.update(existingUser, incomingUser);

        MCRUser persistedUser = MCRUserManager.getUser("foo");
        assertEquals("HR", persistedUser.getUserAttribute("department"));
        assertNull(persistedUser.getUserAttribute("phone"));
    }

    @Test
    public void shouldReplaceRoles() {
        MCRUser existing = new MCRUser("foo");
        existing.assignRole("admin");
        existing.assignRole("editor");
        MCRUserManager.createUser(existing);

        MCRUser existingUser = MCRUserManager.getUser("foo");
        MCRUser incomingUser = new MCRUser("foo");
        incomingUser.assignRole("editor");
        incomingUser.assignRole("reviewer");

        strategy.update(existingUser, incomingUser);

        MCRUser persistedUser = MCRUserManager.getUser("foo");
        assertEquals(Set.of("editor", "reviewer"), getRoleIDs(persistedUser));
    }

    @Test
    public void shouldThrowExceptionIfUserIdsDoNotMatch() {
        MCRUser existingUser = new MCRUser("foo");
        MCRUser incomingUser = new MCRUser("bar");

        try {
            strategy.update(existingUser, incomingUser);
            fail("expected MCRException due to mismatched user ids");
        } catch (MCRException e) {
            assertTrue(e.getMessage().contains("foo"));
            assertTrue(e.getMessage().contains("bar"));
        }
    }

    private Set<String> getRoleIDs(MCRUser user) {
        Set<String> roleIDs = new HashSet<>();
        roleIDs.addAll(user.getSystemRoleIDs());
        roleIDs.addAll(user.getExternalRoleIDs());
        return roleIDs;
    }
}
