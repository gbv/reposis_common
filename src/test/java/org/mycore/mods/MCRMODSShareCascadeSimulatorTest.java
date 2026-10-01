/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.mycore.mods;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRTestCase;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;

public class MCRMODSShareCascadeSimulatorTest extends MCRTestCase {
    private static final String A = "test_mods_00000001";

    private static final String B = "test_mods_00000002";

    private static final String C = "test_mods_00000003";

    private static final String D = "test_mods_00000004";

    private final Map<String, String> xml = new HashMap<>();

    @Test
    public void unchangedCircleFails() throws Exception {
        circle(A, B);
        Assert.assertTrue(simulate(retrieve(B)).isPresent());
    }

    @Test
    public void repairedSimpleCircleSucceeds() throws Exception {
        circle(A, B);
        xml.put(C, object(C, link(A)));
        Assert.assertTrue(simulate(withoutLinks(retrieve(B))).isEmpty());
    }

    @Test
    public void otherCircleInCascadeBlocksRepair() throws Exception {
        circle(A, B);
        circle(C, D);
        // C receives metadata from A, so the update of B reaches the circle between C and D.
        xml.put(C, object(C, link(A) + link(D)));
        Optional<MCRPersistenceException> failure = simulate(withoutLinks(retrieve(B)));
        Assert.assertTrue(failure.isPresent());
        Throwable cause = failure.get();
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        Assert.assertTrue(cause.getMessage(), cause.getMessage().contains(C));
        Assert.assertEquals("simulation must not change the stored objects", object(C, link(A) + link(D)),
            xml.get(C));
    }

    @Test
    public void outdatedCopiesOfTheCircleAreCleared() throws Exception {
        String grandchild = "test_mods_00000005";
        // B is the parent of C and C of the grandchild; their stored copies were made while the circle existed.
        String staleB = "<mods:relatedItem type=\"host\" xlink:href=\"" + B + "\" xlink:type=\"simple\">"
            + link(A).replace("/>", ">" + link(B) + "</mods:relatedItem>") + "</mods:relatedItem>";
        xml.put(A, object(A, link(B)));
        xml.put(B, object(B, "", "", children(C)));
        xml.put(B, xml.get(B).replace("</mods:mods>", link(A).replace("/>", ">" + link(B) + "</mods:relatedItem>")
            + "</mods:mods>"));
        xml.put(C, object(C, staleB, parent(B), children(grandchild)));
        xml.put(grandchild, object(grandchild, "<mods:relatedItem type=\"host\" xlink:href=\"" + C
            + "\" xlink:type=\"simple\">" + staleB + "</mods:relatedItem>", parent(C), ""));
        String storedChild = xml.get(C);

        MCRMODSShareCascadeSimulator simulator = simulator();
        Assert.assertEquals(Optional.empty(), simulator.simulateUpdate(withoutLinks(retrieve(B))));
        Assert.assertEquals(List.of(MCRObjectID.getInstance(C), MCRObjectID.getInstance(grandchild)),
            simulator.getClearedObjects());
        Assert.assertEquals("simulation must not change the stored objects", storedChild, xml.get(C));

        MCRObject child = retrieve(C);
        Assert.assertTrue(simulator.clearSharedMetadata(child));
        Assert.assertFalse(simulator.clearSharedMetadata(child));
        Assert.assertEquals("clearing keeps the hierarchy", MCRObjectID.getInstance(B),
            child.getStructure().getParentID());
    }

    @Test
    public void realCircleIsNotHiddenByClearing() throws Exception {
        circle(A, B);
        circle(C, D);
        String staleD = link(D).replace("/>", ">" + link(C) + "</mods:relatedItem>");
        xml.put(C, object(C, link(A) + staleD));
        MCRMODSShareCascadeSimulator simulator = simulator();
        Assert.assertTrue(simulator.simulateUpdate(withoutLinks(retrieve(B))).isPresent());
    }

    @Test
    public void circuitObjectsOfOutdatedCopiesAreFound() throws Exception {
        // A and B linked each other; C still holds a copy of A made while the circle existed.
        xml.put(C, object(C, link(A).replace("/>", ">" + link(B).replace("/>", ">" + link(A)
            + "</mods:relatedItem>") + "</mods:relatedItem>")));
        xml.put(D, object(D, link(A)));
        Assert.assertEquals(Set.of(MCRObjectID.getInstance(A)),
            MCRMODSShareCascadeSimulator.findCircuitObjects(retrieve(C)));
        Assert.assertEquals(Set.of(), MCRMODSShareCascadeSimulator.findCircuitObjects(retrieve(D)));
    }

    @Test
    public void sharedMetadataRepairReachesOutdatedCopiesOfAnUnchangedObject() throws Exception {
        // The circle between A and B was resolved and A is clean already, only its child C holds the old copy.
        xml.put(B, object(B, ""));
        String cleanB = link(B).replace("/>", "><mods:titleInfo><mods:title>" + B
            + "</mods:title></mods:titleInfo></mods:relatedItem>");
        xml.put(A, object(A, cleanB, "", children(C)));
        String staleA = "<mods:relatedItem type=\"host\" xlink:href=\"" + A + "\" xlink:type=\"simple\">"
            + link(B).replace("/>", ">" + link(A) + "</mods:relatedItem>") + "</mods:relatedItem>";
        xml.put(C, object(C, staleA, parent(A), ""));

        MCRMODSShareCascadeSimulator simulator = simulator();
        Assert.assertEquals(Optional.empty(), simulator.simulateUpdate(retrieve(A)));
        Assert.assertEquals("an unchanged object does not distribute", List.of(), simulator.getClearedObjects());
        Assert.assertEquals(Optional.empty(), simulator.simulateSharedMetadataRepair(retrieve(A)));
        Assert.assertEquals(List.of(MCRObjectID.getInstance(C)), simulator.getClearedObjects());
    }

    private MCRMODSShareCascadeSimulator simulator() {
        return new MCRMODSShareCascadeSimulator(id -> retrieve(id.toString()), this::referrers, 100);
    }

    private Optional<MCRPersistenceException> simulate(MCRObject changed) {
        return simulator().simulateUpdate(changed);
    }

    private Collection<String> referrers(MCRObjectID id) {
        return xml.entrySet().stream()
            .filter(entry -> entry.getValue().contains("xlink:href=\"" + id + "\""))
            .map(Map.Entry::getKey)
            .toList();
    }

    private void circle(String first, String second) {
        xml.put(first, object(first, link(second)));
        xml.put(second, object(second, link(first)));
    }

    private MCRObject retrieve(String id) {
        try {
            return new MCRObject(new SAXBuilder()
                .build(new ByteArrayInputStream(xml.get(id).getBytes(StandardCharsets.UTF_8))));
        } catch (Exception e) {
            throw new MCRPersistenceException("Could not retrieve " + id, e);
        }
    }

    private static MCRObject withoutLinks(MCRObject object) {
        List<Element> items = List.copyOf(new MCRMODSWrapper(object).getMODS()
            .getChildren("relatedItem", MCRConstants.MODS_NAMESPACE));
        items.forEach(Element::detach);
        return object;
    }

    private static String parent(String id) {
        return "<parents class=\"MCRMetaLinkID\"><parent inherited=\"0\" xlink:type=\"locator\" xlink:href=\""
            + id + "\"/></parents>";
    }

    private static String children(String id) {
        return "<children class=\"MCRMetaLinkID\"><child inherited=\"0\" xlink:type=\"locator\" xlink:href=\""
            + id + "\"/></children>";
    }

    private static String object(String id, String items) {
        return object(id, items, "", "");
    }

    private static String object(String id, String items, String parents, String children) {
        return "<mycoreobject ID=\"" + id + "\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
            + " xsi:noNamespaceSchemaLocation=\"datamodel-mods.xsd\" xmlns:mods=\"http://www.loc.gov/mods/v3\""
            + " xmlns:xlink=\"http://www.w3.org/1999/xlink\"><structure>" + parents + children
            + "</structure><metadata>"
            + "<def.modsContainer class=\"MCRMetaXML\"><modsContainer inherited=\"0\"><mods:mods>"
            + "<mods:titleInfo><mods:title>" + id + "</mods:title></mods:titleInfo>" + items
            + "</mods:mods></modsContainer></def.modsContainer></metadata><service/></mycoreobject>";
    }

    private static String link(String target) {
        return "<mods:relatedItem type=\"otherVersion\" xlink:href=\"" + target + "\" xlink:type=\"simple\"/>";
    }
}
