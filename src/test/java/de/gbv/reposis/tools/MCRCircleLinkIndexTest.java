package de.gbv.reposis.tools;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.backend.hibernate.MCRHIBLinkTableStore;
import org.mycore.test.MCRJPAExtension;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@ExtendWith(MCRJPAExtension.class)
public class MCRCircleLinkIndexTest {
    @Test
    public void readsDirectedParentAndReferenceEdgesFromDatabase() {
        var store = new MCRHIBLinkTableStore();
        store.create("test_mods_00000001", "test_mods_00000002", "parent", "");
        store.create("test_mods_00000002", "test_mods_00000001", "reference", "otherVersion");
        store.create("test_mods_00000001", "test_derivate_00000001", "derivate", "");
        var links = new MCRCircleRepairServlet().indexedLinks();
        Assertions.assertEquals(2, links.size());
        Assertions.assertTrue(links.contains(new MCRObjectLinkGraph.MCRObjectLink(
            "test_mods_00000001", "test_mods_00000002", "parent")));
        Assertions.assertTrue(links.contains(new MCRObjectLinkGraph.MCRObjectLink(
            "test_mods_00000002", "test_mods_00000001", "reference")));
    }
}
