package de.gbv.reposis.user.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mycore.common.MCRException;
import org.mycore.common.MCRJPATestCase;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.impl.MCRCategoryDAOImplTest;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserManager;

public class MCRUserUpdateOverwriteStrategyTest extends MCRJPATestCase {

    private final MCRUserUpdateOverwriteStrategy strategy = new MCRUserUpdateOverwriteStrategy();

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        MCRCategory groupsCategory = MCRCategoryDAOImplTest.loadClassificationResource("/mcr-roles.xml");
        MCRCategoryDAO DAO = MCRCategoryDAOFactory.getInstance();
        DAO.addCategory(null, groupsCategory);
    }

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();
        testProperties.put("MCR.user2.Realms.URI", "resource:realms.xml");
        return testProperties;
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
