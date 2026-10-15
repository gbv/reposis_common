package de.gbv.reposis.tools;

import org.junit.Assert;
import org.junit.Test;
import org.mycore.common.MCRJPATestCase;
import org.mycore.backend.hibernate.MCRHIBLinkTableStore;

public class MCRCircleLinkIndexTest extends MCRJPATestCase {
    @Test
    public void readsDirectedParentAndReferenceEdgesFromDatabase() {
        var store = new MCRHIBLinkTableStore();
        store.create("test_mods_00000001", "test_mods_00000002", "parent", "");
        store.create("test_mods_00000002", "test_mods_00000001", "reference", "otherVersion");
        store.create("test_mods_00000001", "test_derivate_00000001", "derivate", "");
        var links = new MCRCircleRepairServlet().indexedLinks();
        Assert.assertEquals(2, links.size());
        Assert.assertTrue(links.contains(new MCRObjectLinkGraph.MCRObjectLink(
            "test_mods_00000001", "test_mods_00000002", "parent")));
        Assert.assertTrue(links.contains(new MCRObjectLinkGraph.MCRObjectLink(
            "test_mods_00000002", "test_mods_00000001", "reference")));
    }
}
