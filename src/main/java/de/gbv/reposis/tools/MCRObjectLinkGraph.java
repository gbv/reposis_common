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

import java.io.InputStream;
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
import java.util.stream.Collectors;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.mycore.common.MCRConstants;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * Collects the links between MyCoRe objects and detects real circles in them.
 *
 * The graph is built from the same links that {@link org.mycore.tools.MCRTopologicalSort} uses to order objects for
 * loading: the <code>parent</code> element of the object structure and every <code>mods:relatedItem</code> element.
 * Each link keeps its relation name, which is <code>parent</code> for the structure link and the value of the
 * <code>type</code> attribute for a related item, e.g. <code>host</code> or <code>otherVersion</code>.
 *
 * Unlike {@link org.mycore.tools.MCRTopologicalSort}, which reports every link that is left over after the
 * topological sort has failed, this class reports the strongly connected components of the graph. Those components
 * are the circles themselves, without the objects that merely depend on them.
 */
public class MCRObjectLinkGraph {

    public static final String RELATION_PARENT = "parent";

    public static final String RELATION_UNKNOWN = "unknown";

    private static final String ELEMENT_MYCOREOBJECT = "mycoreobject";

    private static final String ELEMENT_RELATED_ITEM = "relatedItem";

    private static final String ATTRIBUTE_ID = "ID";

    private static final String ATTRIBUTE_TYPE = "type";

    private static final String ATTRIBUTE_HREF = "href";

    private final Set<String> objectIds = new LinkedHashSet<>();

    private final List<MCRObjectLink> links = new ArrayList<>();

    private final List<MCRRelatedItemCircuit> relatedItemCircuits = new ArrayList<>();

    /**
     * Repeated IDs in stored, nested MODS related items. These are XML paths, not graph components.
     * This diagnostic includes all relation types; the metadata share agent may select a subset.
     */
    public List<MCRRelatedItemCircuit> getRelatedItemCircuits() {
        return List.copyOf(relatedItemCircuits);
    }

    /**
     * Reads one MyCoRe object and adds it and its links to the graph.
     *
     * @param input the XML of the object, the stream is not closed by this method
     * @throws XMLStreamException if the XML cannot be read
     */
    public void add(InputStream input) throws XMLStreamException {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        XMLStreamReader reader = factory.createXMLStreamReader(input);

        String objectId = null;
        List<MCRObjectLink> objectLinks = new ArrayList<>();
        List<MCRRelatedItemCircuit> objectCircuits = new ArrayList<>();
        Deque<List<String>> paths = new ArrayDeque<>();
        while (reader.hasNext()) {
            if (reader.getEventType() == XMLStreamConstants.START_ELEMENT) {
                String elementName = reader.getLocalName();
                boolean relatedItem = ELEMENT_RELATED_ITEM.equals(elementName)
                    && MCRConstants.MODS_NAMESPACE.getURI().equals(reader.getNamespaceURI());
                List<String> path = new ArrayList<>();
                if (relatedItem) {
                    if (!paths.isEmpty()) {
                        path.addAll(paths.peek());
                    }
                    String target = reader.getAttributeValue(MCRConstants.XLINK_NAMESPACE.getURI(), ATTRIBUTE_HREF);
                    if (MCRObjectID.isValid(target)) {
                        boolean repeated = target.equals(objectId) || path.contains(target);
                        path.add(target);
                        if (repeated) {
                            objectCircuits.add(new MCRRelatedItemCircuit(objectId, List.copyOf(path)));
                        }
                    }
                }
                paths.push(path);
                if (ELEMENT_MYCOREOBJECT.equals(elementName)) {
                    objectId = reader.getAttributeValue(null, ATTRIBUTE_ID);
                } else if (RELATION_PARENT.equals(elementName) || ELEMENT_RELATED_ITEM.equals(elementName)) {
                    String target = reader.getAttributeValue(MCRConstants.XLINK_NAMESPACE.getURI(), ATTRIBUTE_HREF);
                    String relation = RELATION_PARENT.equals(elementName)
                        ? RELATION_PARENT
                        : relationOf(reader.getAttributeValue(null, ATTRIBUTE_TYPE));
                    if (MCRObjectID.isValid(target)) {
                        objectLinks.add(new MCRObjectLink(null, target, relation));
                    }
                }
            } else if (reader.getEventType() == XMLStreamConstants.END_ELEMENT) {
                paths.pop();
            }
            reader.next();
        }
        reader.close();

        if (objectId == null) {
            return;
        }
        objectIds.add(objectId);
        relatedItemCircuits.addAll(objectCircuits);
        for (MCRObjectLink link : objectLinks) {
            links.add(new MCRObjectLink(objectId, link.to(), link.relation()));
        }
    }

    /** Builds a graph from explicitly selected links, for repair planning. */
    public static MCRObjectLinkGraph fromLinks(Set<String> ids, List<MCRObjectLink> selectedLinks) {
        MCRObjectLinkGraph graph = new MCRObjectLinkGraph();
        graph.objectIds.addAll(ids);
        graph.links.addAll(selectedLinks);
        return graph;
    }

    /**
     * Returns the number of objects in the graph.
     */
    public int getObjectCount() {
        return objectIds.size();
    }

    /**
     * Returns the number of links that point to an object of this graph.
     */
    public int getLinkCount() {
        return (int) links.stream().filter(link -> objectIds.contains(link.to())).count();
    }

    /**
     * Returns the links that point to an object which is not part of this graph.
     */
    public List<MCRObjectLink> getDanglingLinks() {
        return links.stream()
            .filter(link -> !objectIds.contains(link.to()))
            .collect(Collectors.toList());
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

    private static String relationOf(String type) {
        return type == null || type.isBlank() ? RELATION_UNKNOWN : type;
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

    /** A repeated ID along a relatedItem ancestor chain in the stored XML of an object. */
    public record MCRRelatedItemCircuit(String objectId, List<String> path) {
    }

    private record MCRVisit(String node, Iterator<String> successors) {
    }
}
