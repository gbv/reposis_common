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

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.gbv.reposis.tools.MCRObjectLinkGraph.MCRObjectCircle;
import de.gbv.reposis.tools.MCRObjectLinkGraph.MCRObjectLink;

public class MCRObjectLinkGraphTest {

    private static final String A = "test_mods_00000001";

    private static final String B = "test_mods_00000002";

    private static final String C = "test_mods_00000003";

    @Test
    public void detectsReciprocalRelatedItems() {
        MCRObjectLinkGraph graph = MCRObjectLinkGraph.fromLinks(Set.of(A, B), List.of(
            new MCRObjectLink(A, B, "otherVersion"), new MCRObjectLink(B, A, "otherVersion")));

        List<MCRObjectCircle> circles = graph.detectCircles();

        Assertions.assertEquals(1, circles.size(), "there should be one circle");
        Assertions.assertEquals(List.of(A, B), circles.get(0).objectIds(), "the circle should contain both objects");
        Assertions.assertEquals(2, circles.get(0).links().size(), "both links should be part of the circle");
        Assertions.assertTrue(
            circles.get(0).links().stream().allMatch(link -> "otherVersion".equals(link.relation())),
            "the relation should be kept");
    }

    @Test
    public void ignoresAcyclicHierarchies() {
        MCRObjectLinkGraph graph = MCRObjectLinkGraph.fromLinks(Set.of(A, B, C), List.of(
            new MCRObjectLink(A, B, "parent"), new MCRObjectLink(B, C, "parent")));

        Assertions.assertTrue(graph.detectCircles().isEmpty(), "an acyclic hierarchy has no circles");
    }

    @Test
    public void detectsLongerCircles() {
        MCRObjectLinkGraph graph = MCRObjectLinkGraph.fromLinks(Set.of(A, B, C), List.of(
            new MCRObjectLink(A, B, "parent"), new MCRObjectLink(B, C, "parent"),
            new MCRObjectLink(C, A, "parent")));

        List<MCRObjectCircle> circles = graph.detectCircles();

        Assertions.assertEquals(1, circles.size(), "there should be one circle");
        Assertions.assertEquals(3, circles.get(0).objectIds().size(), "the circle should contain all three objects");
    }

    @Test
    public void detectsSelfReferences() {
        MCRObjectLinkGraph graph = MCRObjectLinkGraph.fromLinks(Set.of(A),
            List.of(new MCRObjectLink(A, A, "references")));

        Assertions.assertEquals(1, graph.detectCircles().size(), "a self reference is a circle");
    }

    @Test
    public void ignoresLinksToUnknownObjects() {
        MCRObjectLinkGraph graph = MCRObjectLinkGraph.fromLinks(Set.of(A, B), List.of(
            new MCRObjectLink(A, B, "otherVersion"), new MCRObjectLink(B, "test_mods_00000099", "host"),
            new MCRObjectLink("test_mods_00000099", A, "host")));

        Assertions.assertTrue(graph.detectCircles().isEmpty(), "links outside the graph do not close a circle");
    }
}
