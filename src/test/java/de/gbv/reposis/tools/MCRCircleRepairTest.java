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

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.mycore.common.MCRTestCase;

public class MCRCircleRepairTest extends MCRTestCase {
    private static final String A = "test_mods_00000001";
    private static final String B = "test_mods_00000002";

    @Test
    public void indexedPreviewReadsOnlyCycleCandidatesAndVerifiesXml() throws Exception {
        MemoryRepository repository = pair("otherVersion", "otherVersion");
        var reads = new java.util.ArrayList<String>();
        var reader = new MCRCircleRepairService.Reader() {
            public byte[] read(String id) {
                reads.add(id);
                if (!repository.xml.containsKey(id))
                    throw new AssertionError("Read outside cycle: " + id);
                return repository.read(id);
            }
        };
        var indexed = new java.util.ArrayList<MCRObjectLinkGraph.MCRObjectLink>();
        indexed.add(new MCRObjectLinkGraph.MCRObjectLink(A, B, "reference"));
        indexed.add(new MCRObjectLinkGraph.MCRObjectLink(B, A, "reference"));
        for (int i = 3; i < 10003; i++) {
            indexed.add(new MCRObjectLinkGraph.MCRObjectLink("test_mods_" + String.format("%08d", i), A, "parent"));
        }
        var result = MCRCircleRepairService.previewIndexed(reader, indexed);
        Assert.assertEquals(10002, result.objectCount());
        Assert.assertEquals(1, result.cases().size());
        Assert.assertEquals(java.util.Set.of(A, B), new java.util.HashSet<>(reads));
        Assert.assertEquals(2, reads.size());
        Assert.assertEquals("otherVersion", result.cases().get(0).links().get(0).relation());
        // A stale positive index entry must not become a spurious editor suggestion.
        repository.xml.put(B, object(B, ""));
        Assert.assertTrue(MCRCircleRepairService.previewIndexed(reader, indexed).cases().isEmpty());
    }

    @Test
    public void acyclicIndexNeedsNoXmlReads() throws Exception {
        var reader = new MCRCircleRepairService.Reader() {
            public byte[] read(String id) {
                throw new AssertionError("Unexpected XML read");
            }
        };
        Assert.assertTrue(MCRCircleRepairService.previewIndexed(reader, List.of(
            new MCRObjectLinkGraph.MCRObjectLink(A, B, "parent"))).cases().isEmpty());
    }

    @Test
    public void suggestionsPreserveChronologyAndDoNotChangeXml() throws Exception {
        var repository = pair("preceding", "otherFormat");
        byte[] original = repository.read(B).clone();
        var result = previewPair(repository).cases().get(0);
        Assert.assertTrue(result.suggested());
        var removals = result.links().stream().filter(MCRCircleRepairService.PreviewLink::remove).toList();
        Assert.assertEquals(1, removals.size());
        Assert.assertEquals(B, removals.get(0).from());
        Assert.assertEquals("otherFormat", removals.get(0).relation());
        Assert.assertArrayEquals(original, repository.read(B));
    }

    @Test
    public void suggestionsProtectHierarchyAndLocalMetadata() throws Exception {
        Assert.assertFalse(previewPair(pair("host", "host")).cases().get(0).suggested());
        var repository = pair("preceding", "otherFormat");
        repository.xml.put(B, object(B, link(A, "otherFormat").replace("/>",
            "><mods:part><mods:text>local pages</mods:text></mods:part></mods:relatedItem>")));
        var result = previewPair(repository).cases().get(0);
        Assert.assertFalse(result.suggested());
        Assert.assertTrue(result.links().stream().noneMatch(MCRCircleRepairService.PreviewLink::remove));
    }

    private static MCRCircleRepairService.Preview previewPair(MemoryRepository repository) throws Exception {
        return MCRCircleRepairService.previewIndexed(repository, List.of(
            new MCRObjectLinkGraph.MCRObjectLink(A, B, "reference"),
            new MCRObjectLinkGraph.MCRObjectLink(B, A, "reference")));
    }

    private static MemoryRepository pair(String forward, String backward) {
        MemoryRepository repository = new MemoryRepository();
        repository.xml.put(A, object(A, link(B, forward)));
        repository.xml.put(B, object(B, link(A, backward)));
        return repository;
    }

    private static byte[] object(String id, String items) {
        return ("<mycoreobject ID=\"" + id + "\" xmlns:mods=\"http://www.loc.gov/mods/v3\""
            + " xmlns:xlink=\"http://www.w3.org/1999/xlink\"><structure/><metadata><def.modsContainer>"
            + "<modsContainer><mods:mods>" + items + "</mods:mods></modsContainer>"
            + "</def.modsContainer></metadata></mycoreobject>").getBytes(StandardCharsets.UTF_8);
    }

    private static String link(String target, String relation) {
        return "<mods:relatedItem type=\"" + relation + "\" xlink:href=\"" + target + "\"/>";
    }

    private static class MemoryRepository implements MCRCircleRepairService.Reader {
        private final Map<String, byte[]> xml = new HashMap<>();

        public byte[] read(String id) {
            return xml.get(id);
        }
    }
}
