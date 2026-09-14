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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;

import de.gbv.reposis.tools.MCRCircleRepair.Decision;
import de.gbv.reposis.tools.MCRCircleRepair.Link;

/** Read-only diagnosis and suggestions for manual editing. */
public final class MCRCircleRepairService {
    @FunctionalInterface
    public interface Reader {
        byte[] read(String id) throws Exception;
    }

    private record Snapshot(List<Link> links, List<Decision> decisions) {
    }

    private MCRCircleRepairService() {
    }

    public record Preview(List<RepairCase> cases, int objectCount) {
    }

    public record RepairCase(List<String> members, List<PreviewLink> links, boolean suggested, String explanation) {
    }

    public record PreviewLink(String from, String to, String relation, int position, boolean remove, String reason) {
    }

    /** Uses the link index to select candidates; never reads XML outside cyclic components. */
    public static Preview previewIndexed(Reader repository,
        List<MCRObjectLinkGraph.MCRObjectLink> indexedLinks) throws Exception {
        Set<String> ids = new HashSet<>();
        List<MCRObjectLinkGraph.MCRObjectLink> links = indexedLinks.stream()
            .filter(link -> org.mycore.datamodel.metadata.MCRObjectID.isValid(link.from())
                && org.mycore.datamodel.metadata.MCRObjectID.isValid(link.to()))
            .filter(link -> !link.from().contains("_derivate_") && !link.to().contains("_derivate_"))
            .toList();
        links.forEach(link -> {
            ids.add(link.from());
            ids.add(link.to());
        });
        var candidates = new java.util.TreeSet<String>();
        MCRObjectLinkGraph.fromLinks(ids, links).detectCircles()
            .forEach(circle -> candidates.addAll(circle.objectIds()));
        List<Link> verifiedLinks = new ArrayList<>();
        for (String id : candidates) {
            byte[] xml = repository.read(id);
            Document document = parse(xml);
            if (!id.equals(document.getRootElement().getAttributeValue("ID"))) {
                throw new IllegalStateException("Abweichende Dokument-ID beim Lesen von " + id);
            }
            verifiedLinks.addAll(MCRCircleRepair.links(document));
        }
        // Suggestions concern each actual circle.
        return preview(new Snapshot(verifiedLinks,
            MCRCircleRepair.plan(candidates, verifiedLinks)), ids.size());
    }

    private static Preview preview(Snapshot snapshot, int objectCount) {
        List<RepairCase> cases = new ArrayList<>();
        for (Decision decision : snapshot.decisions()) {
            List<Link> internal = snapshot.links().stream().filter(link -> decision.members().contains(link.from())
                && decision.members().contains(link.to())).toList();
            List<Link> kept = internal.stream().filter(link -> !decision.removals().contains(link)).toList();
            List<PreviewLink> rows = new ArrayList<>();
            for (Link link : internal) {
                boolean remove = decision.removals().contains(link);
                String reason = protectedReason(link);
                if (remove) {
                    reason = link.from().equals(link.to()) ? "Dieser Verweis zeigt auf das Dokument selbst."
                        : "Dieser Verweis schließt den Kreis über den erhaltenen Rückweg: "
                            + returnPath(link.to(), link.from(), kept)
                            + ". Bei gleichrangigen Verweisen entscheidet die aufsteigende Quell-/Ziel-ID.";
                } else if (reason == null) {
                    reason =
                        "Dieser Verweis kann erhalten bleiben, ohne den vorgeschlagenen Ergebnisgraphen zyklisch zu machen.";
                    if (decision.removals().isEmpty()) {
                        reason =
                            "Einzelne Löschungen reichen hier nicht aus; geschützte Verweise bilden weiterhin einen Kreis.";
                    }
                }
                rows.add(new PreviewLink(link.from(), link.to(), link.relation(), link.position() + 1, remove, reason));
            }
            boolean suggested = !decision.removals().isEmpty();
            cases.add(new RepairCase(decision.members(), rows, suggested,
                !suggested ? "Hier bleiben Kreise aus geschützten Verweisen oder Verweisen mit zusätzlichen Angaben. "
                    + "Bitte die markierten Beziehungen fachlich prüfen; es wird nichts automatisch gelöscht."
                    : "Vorschlag: Die markierten Verweise im Editor prüfen und gegebenenfalls entfernen. "
                        + "Diese Übersicht ändert keine Dokumente."));
        }
        return new Preview(List.copyOf(cases), objectCount);
    }

    static Document parse(byte[] xml) throws Exception {
        SAXBuilder builder = new SAXBuilder();
        builder.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        builder.setFeature("http://xml.org/sax/features/external-general-entities", false);
        builder.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        return builder.build(new ByteArrayInputStream(xml));
    }

    private static String protectedReason(Link link) {
        if (Set.of("parent", "host").contains(link.relation())) {
            return "Dieser Verweis beschreibt die übergeordnete Publikation bzw. die Objekthierarchie.";
        }
        if (!link.empty()) {
            return "Das Element enthält Metadaten oder zusätzliche Attribute. Automatisches Löschen könnte Angaben verlieren.";
        }
        if (!link.from().equals(link.to()) && !Set.of("otherFormat", "otherVersion").contains(link.relation())) {
            return "Für die Relation '" + link.relation() + "' ist keine automatische Löschregel festgelegt.";
        }
        return null;
    }

    private static String returnPath(String start, String target, List<Link> links) {
        var queue = new java.util.ArrayDeque<String>();
        Map<String, Link> previous = new java.util.HashMap<>();
        Set<String> visited = new HashSet<>(Set.of(start));
        queue.add(start);
        while (!queue.isEmpty()) {
            String current = queue.remove();
            if (current.equals(target)) {
                List<Link> path = new ArrayList<>();
                while (!current.equals(start)) {
                    Link link = previous.get(current);
                    path.add(link);
                    current = link.from();
                }
                java.util.Collections.reverse(path);
                StringBuilder result = new StringBuilder(start);
                path.forEach(link -> result.append(" --").append(link.relation()).append("--> ").append(link.to()));
                return result.toString();
            }
            for (Link link : links) {
                if (link.from().equals(current) && visited.add(link.to())) {
                    previous.put(link.to(), link);
                    queue.add(link.to());
                }
            }
        }
        throw new IllegalStateException(
            "Kein Rückweg für den vorgeschlagenen Kreisverweis gefunden: " + start + " -> " + target);
    }

}
