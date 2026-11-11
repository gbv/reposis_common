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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Collects the links between MyCoRe objects and detects real circles in them.
 *
 * The graph is built from explicitly given links, e.g. the <code>parent</code> and <code>reference</code> entries of
 * the link index. Each link keeps its relation name, e.g. <code>parent</code>, <code>host</code> or
 * <code>otherVersion</code>.
 *
 * Unlike {@link org.mycore.tools.MCRTopologicalSort}, which reports every link that is left over after the
 * topological sort has failed, this class reports the strongly connected components of the graph. Those components
 * are the circles themselves, without the objects that merely depend on them.
 */
public class MCRObjectLinkGraph {

    public static final String RELATION_UNKNOWN = "unknown";

    private final Set<String> objectIds = new LinkedHashSet<>();

    private final List<MCRObjectLink> links = new ArrayList<>();

    /** Builds a graph from explicitly selected links, for repair planning. */
    public static MCRObjectLinkGraph fromLinks(Set<String> ids, List<MCRObjectLink> selectedLinks) {
        MCRObjectLinkGraph graph = new MCRObjectLinkGraph();
        graph.objectIds.addAll(ids);
        graph.links.addAll(selectedLinks);
        return graph;
    }

    /**
     * Detects the circles of the graph, biggest circle first.
     *
     * @return the strongly connected components that contain more than one object or a self reference
     */
    public List<MCRObjectCircle> detectCircles() {
        SortedMap<String, SortedMap<String, Set<String>>> edges = buildEdges();
        List<List<String>> components = findStronglyConnectedComponents(edges);

        List<MCRObjectCircle> circles = new ArrayList<>(components.size());
        for (List<String> component : components) {
            Set<String> members = new HashSet<>(component);
            List<MCRObjectLink> circleLinks = new ArrayList<>();
            for (String source : component) {
                edges.getOrDefault(source, Collections.emptySortedMap())
                    .forEach((target, relations) -> {
                        if (members.contains(target)) {
                            relations.forEach(relation -> circleLinks.add(
                                new MCRObjectLink(source, target, relation)));
                        }
                    });
            }
            circles.add(new MCRObjectCircle(component, circleLinks));
        }
        circles.sort(Comparator.comparingInt((MCRObjectCircle circle) -> circle.objectIds().size()).reversed()
            .thenComparing(circle -> circle.objectIds().get(0)));
        return circles;
    }

    private SortedMap<String, SortedMap<String, Set<String>>> buildEdges() {
        SortedMap<String, SortedMap<String, Set<String>>> edges = new TreeMap<>();
        for (MCRObjectLink link : links) {
            if (objectIds.contains(link.to())) {
                edges.computeIfAbsent(link.from(), source -> new TreeMap<>())
                    .computeIfAbsent(link.to(), target -> new TreeSet<>())
                    .add(link.relation());
            }
        }
        return edges;
    }

    /**
     * Iterative version of Tarjan's algorithm, see
     * https://en.wikipedia.org/wiki/Tarjan%27s_strongly_connected_components_algorithm
     *
     * A recursive version would overflow the stack on repositories of this size.
     */
    private List<List<String>> findStronglyConnectedComponents(
        SortedMap<String, SortedMap<String, Set<String>>> edges) {
        Map<String, Integer> indexes = new HashMap<>();
        Map<String, Integer> lowLinks = new HashMap<>();
        Deque<String> componentStack = new ArrayDeque<>();
        Set<String> onComponentStack = new HashSet<>();
        Deque<MCRVisit> visits = new ArrayDeque<>();
        List<List<String>> components = new ArrayList<>();
        int counter = 0;

        for (String root : new TreeSet<>(objectIds)) {
            if (indexes.containsKey(root)) {
                continue;
            }
            indexes.put(root, counter);
            lowLinks.put(root, counter);
            counter++;
            componentStack.push(root);
            onComponentStack.add(root);
            visits.push(new MCRVisit(root, targetsOf(edges, root).iterator()));

            while (!visits.isEmpty()) {
                MCRVisit visit = visits.peek();
                boolean descended = false;
                while (visit.successors().hasNext()) {
                    String successor = visit.successors().next();
                    if (!indexes.containsKey(successor)) {
                        indexes.put(successor, counter);
                        lowLinks.put(successor, counter);
                        counter++;
                        componentStack.push(successor);
                        onComponentStack.add(successor);
                        visits.push(new MCRVisit(successor, targetsOf(edges, successor).iterator()));
                        descended = true;
                        break;
                    } else if (onComponentStack.contains(successor)) {
                        lowLinks.merge(visit.node(), indexes.get(successor), Math::min);
                    }
                }
                if (descended) {
                    continue;
                }

                visits.pop();
                if (!visits.isEmpty()) {
                    lowLinks.merge(visits.peek().node(), lowLinks.get(visit.node()), Math::min);
                }
                if (lowLinks.get(visit.node()).equals(indexes.get(visit.node()))) {
                    List<String> component = new ArrayList<>();
                    String member;
                    do {
                        member = componentStack.pop();
                        onComponentStack.remove(member);
                        component.add(member);
                    } while (!member.equals(visit.node()));
                    if (component.size() > 1 || targetsOf(edges, visit.node()).contains(visit.node())) {
                        Collections.sort(component);
                        components.add(component);
                    }
                }
            }
        }
        return components;
    }

    private static Set<String> targetsOf(SortedMap<String, SortedMap<String, Set<String>>> edges, String source) {
        return edges.getOrDefault(source, Collections.emptySortedMap()).keySet();
    }

    /**
     * A link from one MyCoRe object to another one.
     *
     * @param from the id of the linking object
     * @param to the id of the linked object
     * @param relation <code>parent</code> or the type of the related item, e.g. <code>host</code>
     */
    public record MCRObjectLink(String from, String to, String relation) {
    }

    /**
     * A set of objects that link to each other directly or indirectly.
     *
     * @param objectIds the ids of the objects of the circle
     * @param links the links between those objects
     */
    public record MCRObjectCircle(List<String> objectIds, List<MCRObjectLink> links) {
    }

    private record MCRVisit(String node, Iterator<String> successors) {
    }
}
