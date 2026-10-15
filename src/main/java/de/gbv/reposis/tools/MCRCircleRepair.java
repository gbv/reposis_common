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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jdom2.Document;
import org.jdom2.Element;
import org.mycore.common.MCRConstants;
import org.mycore.datamodel.metadata.MCRObjectID;

import de.gbv.reposis.tools.MCRObjectLinkGraph.MCRObjectLink;

/** Pure, conservative repair planner. Never changes input documents. */
public final class MCRCircleRepair {
    private static final Set<String> WEAK = Set.of("otherFormat", "otherVersion");

    private static final Set<String> PROTECTED = Set.of("parent", "host");

    private MCRCircleRepair() {
    }

    public record Link(String from, String to, String relation, int position, boolean empty) {
        MCRObjectLink graphLink() {
            return new MCRObjectLink(from, to, relation);
        }
    }

    public record Decision(List<String> members, List<Link> removals, String reason) {
    }

    public static List<Element> relatedItems(Document document) {
        Element metadata = document.getRootElement().getChild("metadata");
        if (metadata == null) {
            return List.of();
        }
        Element definition = metadata.getChild("def.modsContainer");
        if (definition == null) {
            return List.of();
        }
        Element container = definition.getChild("modsContainer");
        Element mods = container == null ? null : container.getChild("mods", MCRConstants.MODS_NAMESPACE);
        return mods == null ? List.of() : mods.getChildren("relatedItem", MCRConstants.MODS_NAMESPACE);
    }

    public static List<Link> links(Document document) {
        String id = document.getRootElement().getAttributeValue("ID");
        List<Link> links = new ArrayList<>();
        Element structure = document.getRootElement().getChild("structure");
        Element parents = structure == null ? null : structure.getChild("parents");
        if (parents != null) {
            for (Element parent : parents.getChildren("parent")) {
                add(links, id, parent, "parent", -1);
            }
        }
        List<Element> items = relatedItems(document);
        for (int i = 0; i < items.size(); i++) {
            Element item = items.get(i);
            add(links, id, item, item.getAttributeValue("type", "unknown"), i);
        }
        return links;
    }

    private static void add(List<Link> links, String id, Element item, String type, int position) {
        String target = item.getAttributeValue("href", MCRConstants.XLINK_NAMESPACE);
        if (MCRObjectID.isValid(target)) {
            links.add(new Link(id, target, type, position,
                item.getChildren().isEmpty() && item.getTextTrim().isEmpty()
                    && item.getAttributes().stream().allMatch(
                        attribute -> (attribute.getNamespaceURI().isEmpty() && attribute.getName().equals("type"))
                            || (attribute.getNamespace().equals(MCRConstants.XLINK_NAMESPACE)
                                && Set.of("href", "type").contains(attribute.getName())))));
        }
    }

    public static List<Decision> plan(Set<String> ids, List<Link> links) {
        var circles = graph(ids, links).detectCircles();
        List<Decision> decisions = new ArrayList<>();
        for (var circle : circles) {
            Set<String> members = new HashSet<>(circle.objectIds());
            List<Link> internal = links.stream()
                .filter(l -> members.contains(l.from()) && members.contains(l.to())).toList();
            List<Link> candidates = internal.stream().filter(l -> l.position() >= 0 && l.empty())
                .filter(l -> !PROTECTED.contains(l.relation()))
                .filter(l -> l.from().equals(l.to()) || WEAK.contains(l.relation()))
                .sorted(java.util.Comparator.comparing(Link::from).thenComparing(Link::to)
                    .thenComparing(Link::relation).thenComparingInt(Link::position))
                .toList();
            List<Link> kept = new ArrayList<>(internal);
            kept.removeAll(candidates);
            if (!graph(members, kept).detectCircles().isEmpty()) {
                decisions.add(new Decision(circle.objectIds(), List.of(),
                    "Auch ohne automatisch entfernbare Verweise bleibt ein Kreis; Beziehungen und vorhandene Angaben fachlich prüfen."));
                continue;
            }
            List<Link> removals = new ArrayList<>();
            for (Link candidate : candidates) {
                kept.add(candidate);
                if (!graph(members, kept).detectCircles().isEmpty()) {
                    kept.remove(candidate);
                    removals.add(candidate);
                }
            }
            decisions.add(new Decision(circle.objectIds(), List.copyOf(removals),
                "Die vorgeschlagenen Entfernungen unterbrechen alle direkten Kreise dieser Gruppe."));
        }
        return decisions;
    }

    private static MCRObjectLinkGraph graph(Set<String> ids, List<Link> links) {
        return MCRObjectLinkGraph.fromLinks(ids, links.stream().map(Link::graphLink).toList());
    }

}
