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

import javax.xml.stream.XMLStreamException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.test.MyCoReTest;

import de.gbv.reposis.tools.MCRObjectLinkGraph.MCRObjectCircle;

@MyCoReTest
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.Metadata.Type.mods", string = "true")
})
public class MCRObjectLinkGraphTest {

    @Test
    public void detectsReciprocalRelatedItems() throws XMLStreamException {
        MCRObjectLinkGraph graph = new MCRObjectLinkGraph();
        graph.add(object("test_mods_00000001", relatedItem("test_mods_00000002", "otherVersion")));
        graph.add(object("test_mods_00000002", relatedItem("test_mods_00000001", "otherVersion")));

        List<MCRObjectCircle> circles = graph.detectCircles();

        Assertions.assertEquals(1, circles.size(), "there should be one circle");
        Assertions.assertEquals(List.of("test_mods_00000001", "test_mods_00000002"), circles.get(0).objectIds(),
            "the circle should contain both objects");
        Assertions.assertEquals(2, circles.get(0).links().size(), "both links should be part of the circle");
        Assertions.assertTrue(
            circles.get(0).links().stream().allMatch(link -> "otherVersion".equals(link.relation())),
            "the relation should be kept");
    }

    @Test
    public void ignoresAcyclicHierarchies() throws XMLStreamException {
        MCRObjectLinkGraph graph = new MCRObjectLinkGraph();
        graph.add(object("test_mods_00000001", parent("test_mods_00000002")));
        graph.add(object("test_mods_00000002", parent("test_mods_00000003")));
        graph.add(object("test_mods_00000003", ""));

        Assertions.assertTrue(graph.detectCircles().isEmpty(), "an acyclic hierarchy has no circles");
        Assertions.assertEquals(2, graph.getLinkCount(), "both parent links should be counted");
    }

    @Test
    public void detectsLongerCircles() throws XMLStreamException {
        MCRObjectLinkGraph graph = new MCRObjectLinkGraph();
        graph.add(object("test_mods_00000001", parent("test_mods_00000002")));
        graph.add(object("test_mods_00000002", parent("test_mods_00000003")));
        graph.add(object("test_mods_00000003", parent("test_mods_00000001")));

        List<MCRObjectCircle> circles = graph.detectCircles();

        Assertions.assertEquals(1, circles.size(), "there should be one circle");
        Assertions.assertEquals(3, circles.get(0).objectIds().size(), "the circle should contain all three objects");
    }

    @Test
    public void detectsSelfReferences() throws XMLStreamException {
        MCRObjectLinkGraph graph = new MCRObjectLinkGraph();
        graph.add(object("test_mods_00000001", relatedItem("test_mods_00000001", "references")));

        Assertions.assertEquals(1, graph.detectCircles().size(), "a self reference is a circle");
    }

    @Test
    public void reportsLinksToUnknownObjects() throws XMLStreamException {
        MCRObjectLinkGraph graph = new MCRObjectLinkGraph();
        graph.add(object("test_mods_00000001", relatedItem("test_mods_00000099", "host")));

        Assertions.assertEquals(0, graph.getLinkCount(), "the link target is not part of the graph");
        Assertions.assertEquals(1, graph.getDanglingLinks().size(), "the link should be reported as dangling");
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

        Assertions.assertTrue(graph.detectCircles().isEmpty());
        Assertions.assertEquals(1, graph.getRelatedItemCircuits().size());
        Assertions.assertEquals("openagrar_mods_00013984", graph.getRelatedItemCircuits().get(0).objectId());
        Assertions.assertEquals(List.of("zimport_mods_00003460", "openagrar_mods_00062243", "zimport_mods_00003460"),
            graph.getRelatedItemCircuits().get(0).path());
    }

    @Test
    public void allowsSameTargetInSeparateXmlBranches() throws XMLStreamException {
        MCRObjectLinkGraph graph = new MCRObjectLinkGraph();
        String link = "<mods:relatedItem xmlns:mods=\"http://www.loc.gov/mods/v3\""
            + " type=\"host\" xlink:href=\"test_mods_00000002\"/>";
        graph.add(object("test_mods_00000001", link + link));
        Assertions.assertTrue(graph.getRelatedItemCircuits().isEmpty());
    }

    @Test
    public void reportsOwnIdInNestedXml() throws XMLStreamException {
        MCRObjectLinkGraph graph = new MCRObjectLinkGraph();
        graph.add(object("test_mods_00000001",
            "<mods:relatedItem xmlns:mods=\"http://www.loc.gov/mods/v3\""
                + " type=\"otherFormat\" xlink:href=\"test_mods_00000001\"/>"));
        Assertions.assertEquals(1, graph.getRelatedItemCircuits().size());
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
}
