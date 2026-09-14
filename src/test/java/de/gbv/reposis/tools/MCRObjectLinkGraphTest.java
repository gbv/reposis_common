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

package de.gbv.reposis.tools;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import org.junit.Assert;
import org.junit.Test;
import org.mycore.common.MCRTestCase;

import de.gbv.reposis.tools.MCRObjectLinkGraph.MCRObjectCircle;

public class MCRObjectLinkGraphTest extends MCRTestCase {

    @Test
    public void detectsReciprocalRelatedItems() throws XMLStreamException {
        MCRObjectLinkGraph graph = new MCRObjectLinkGraph();
        graph.add(object("test_mods_00000001", relatedItem("test_mods_00000002", "otherVersion")));
        graph.add(object("test_mods_00000002", relatedItem("test_mods_00000001", "otherVersion")));

        List<MCRObjectCircle> circles = graph.detectCircles();

        Assert.assertEquals("there should be one circle", 1, circles.size());
        Assert.assertEquals("the circle should contain both objects",
            List.of("test_mods_00000001", "test_mods_00000002"), circles.get(0).objectIds());
        Assert.assertEquals("both links should be part of the circle", 2, circles.get(0).links().size());
        Assert.assertTrue("the relation should be kept",
            circles.get(0).links().stream().allMatch(link -> "otherVersion".equals(link.relation())));
    }

    @Test
    public void ignoresAcyclicHierarchies() throws XMLStreamException {
        MCRObjectLinkGraph graph = new MCRObjectLinkGraph();
        graph.add(object("test_mods_00000001", parent("test_mods_00000002")));
        graph.add(object("test_mods_00000002", parent("test_mods_00000003")));
        graph.add(object("test_mods_00000003", ""));

        Assert.assertTrue("an acyclic hierarchy has no circles", graph.detectCircles().isEmpty());
        Assert.assertEquals("both parent links should be counted", 2, graph.getLinkCount());
    }

    @Test
    public void detectsLongerCircles() throws XMLStreamException {
        MCRObjectLinkGraph graph = new MCRObjectLinkGraph();
        graph.add(object("test_mods_00000001", parent("test_mods_00000002")));
        graph.add(object("test_mods_00000002", parent("test_mods_00000003")));
        graph.add(object("test_mods_00000003", parent("test_mods_00000001")));

        List<MCRObjectCircle> circles = graph.detectCircles();

        Assert.assertEquals("there should be one circle", 1, circles.size());
        Assert.assertEquals("the circle should contain all three objects", 3, circles.get(0).objectIds().size());
    }

    @Test
    public void detectsSelfReferences() throws XMLStreamException {
        MCRObjectLinkGraph graph = new MCRObjectLinkGraph();
        graph.add(object("test_mods_00000001", relatedItem("test_mods_00000001", "references")));

        Assert.assertEquals("a self reference is a circle", 1, graph.detectCircles().size());
    }

    @Test
    public void reportsLinksToUnknownObjects() throws XMLStreamException {
        MCRObjectLinkGraph graph = new MCRObjectLinkGraph();
        graph.add(object("test_mods_00000001", relatedItem("test_mods_00000099", "host")));

        Assert.assertEquals("the link target is not part of the graph", 0, graph.getLinkCount());
        Assert.assertEquals("the link should be reported as dangling", 1, graph.getDanglingLinks().size());
    }

    @Test
    public void reportsNestedCircuitInDependentObjectWithoutGraphCycle() throws XMLStreamException {
        MCRObjectLinkGraph graph = new MCRObjectLinkGraph();
        graph.add(object("openagrar_mods_00013984",
            "<mods:relatedItem xmlns:mods=\"http://www.loc.gov/mods/v3\" type=\"host\""
                + " xlink:href=\"zimport_mods_00003460\">"
                + "<mods:relatedItem type=\"preceding\" xlink:href=\"openagrar_mods_00062243\">"
                + "<mods:relatedItem type=\"otherFormat\" xlink:href=\"zimport_mods_00003460\"/>"
                + "</mods:relatedItem></mods:relatedItem>"));

        Assert.assertTrue(graph.detectCircles().isEmpty());
        Assert.assertEquals(1, graph.getRelatedItemCircuits().size());
        Assert.assertEquals("openagrar_mods_00013984", graph.getRelatedItemCircuits().get(0).objectId());
        Assert.assertEquals(List.of("zimport_mods_00003460", "openagrar_mods_00062243", "zimport_mods_00003460"),
            graph.getRelatedItemCircuits().get(0).path());
    }

    @Test
    public void allowsSameTargetInSeparateXmlBranches() throws XMLStreamException {
        MCRObjectLinkGraph graph = new MCRObjectLinkGraph();
        String link = "<mods:relatedItem xmlns:mods=\"http://www.loc.gov/mods/v3\""
            + " type=\"host\" xlink:href=\"test_mods_00000002\"/>";
        graph.add(object("test_mods_00000001", link + link));
        Assert.assertTrue(graph.getRelatedItemCircuits().isEmpty());
    }

    @Test
    public void reportsOwnIdInNestedXml() throws XMLStreamException {
        MCRObjectLinkGraph graph = new MCRObjectLinkGraph();
        graph.add(object("test_mods_00000001",
            "<mods:relatedItem xmlns:mods=\"http://www.loc.gov/mods/v3\""
                + " type=\"otherFormat\" xlink:href=\"test_mods_00000001\"/>"));
        Assert.assertEquals(1, graph.getRelatedItemCircuits().size());
    }

    private static ByteArrayInputStream object(String id, String links) {
        String xml = "<mycoreobject xmlns:xlink=\"http://www.w3.org/1999/xlink\" ID=\"" + id + "\">"
            + "<structure>" + links + "</structure>"
            + "</mycoreobject>";
        return new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    }

    private static String parent(String id) {
        return "<parents><parent xlink:href=\"" + id + "\" xlink:type=\"locator\" /></parents>";
    }

    private static String relatedItem(String id, String type) {
        return "<relatedItem xlink:href=\"" + id + "\" type=\"" + type + "\" xlink:type=\"simple\" />";
    }

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();
        testProperties.put("MCR.Metadata.Type.mods", Boolean.TRUE.toString());
        return testProperties;
    }
}
