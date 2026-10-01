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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.Filter;
import org.jdom2.filter.Filters;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.datamodel.common.MCRLinkTableManager;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * Dry run of {@link MCRMetadataManager#update(MCRObject)} for MODS objects, including the cascade of
 * {@link MCRMODSMetadataShareAgent#distributeMetadata(MCRObject)}. Nothing is stored: every object the cascade would
 * save is kept in memory and used instead of the stored version for the following steps.
 *
 * The update stores the object before it distributes the metadata. A failing cascade therefore leaves the object and
 * part of the cascade stored in the metadata store, while the database transaction is rolled back. This simulation
 * finds such failures before anything is written.
 *
 * Objects of the cascade may contain outdated shared metadata with a circuit, e.g. copies made while a circle
 * existed. The agent checks the hierarchy before it replaces that copy, so the cascade would fail on data that is about
 * to be replaced. The simulation clears the shared metadata of those objects, like
 * {@link #clearSharedMetadata(MCRObject)} does, and reports them by {@link #getClearedObjects()}. A real circle still
 * fails, because the shared metadata is rebuilt from the current objects during the simulated update.
 *
 * The class lives in the package of {@link MCRMODSMetadataShareAgent} to use the same hierarchy check and
 * inheritance code as the agent.
 */
public class MCRMODSShareCascadeSimulator {

    private final MCRMODSMetadataShareAgent agent = new MCRMODSMetadataShareAgent();

    private final Function<MCRObjectID, MCRObject> store;

    private final Function<MCRObjectID, Collection<String>> referrers;

    private final int maxUpdates;

    private final Map<MCRObjectID, Document> updated = new HashMap<>();

    private final Map<MCRObjectID, Document> cleared = new LinkedHashMap<>();

    private int updates;

    /**
     * @param store reads the stored version of an object
     * @param referrers returns the ids of the objects that reference the given object in the link table
     * @param maxUpdates the maximum number of simulated updates, the simulation fails if the cascade is bigger
     */
    public MCRMODSShareCascadeSimulator(Function<MCRObjectID, MCRObject> store,
        Function<MCRObjectID, Collection<String>> referrers, int maxUpdates) {
        this.store = store;
        this.referrers = referrers;
        this.maxUpdates = maxUpdates;
    }

    /** Simulates on the stored objects and the link table of the application. */
    public static MCRMODSShareCascadeSimulator ofRepository(int maxUpdates) {
        return new MCRMODSShareCascadeSimulator(MCRMetadataManager::retrieveMCRObject,
            id -> MCRLinkTableManager.instance().getSourceOf(id, MCRLinkTableManager.ENTRY_TYPE_REFERENCE),
            maxUpdates);
    }

    /**
     * Simulates the update of the changed object and every update of the resulting cascade.
     *
     * @param changed the changed object as it would be passed to {@link MCRMetadataManager#update(MCRObject)}, is not
     *        modified by this method
     * @return the failure the real update would throw, empty if the update and its cascade would succeed
     */
    public Optional<MCRPersistenceException> simulateUpdate(MCRObject changed) {
        return simulate(changed, false);
    }

    /**
     * Simulates {@link MCRMetadataManager#repairSharedMetadata(MCRObject)}: the object receives its shared metadata
     * again and distributes it even if its own metadata does not change.
     *
     * @param object the object whose shared metadata is repaired, is not modified by this method
     * @return the failure the real repair would throw, empty if the repair and its cascade would succeed
     */
    public Optional<MCRPersistenceException> simulateSharedMetadataRepair(MCRObject object) {
        return simulate(object, true);
    }

    /**
     * Returns the objects whose outdated shared metadata must be cleared by {@link #clearSharedMetadata(MCRObject)}
     * before the update, in the order they were found by the last simulation.
     */
    public List<MCRObjectID> getClearedObjects() {
        return List.copyOf(cleared.keySet());
    }

    /**
     * Finds the objects that occur twice in a chain of nested <code>mods:relatedItem</code> elements of the stored
     * shared metadata, like {@link MCRMODSMetadataShareAgent#checkHierarchy(MCRMODSWrapper)} does. Such an object is
     * the entry point to refresh the outdated copies: it receives a copy without the circuit, if the circle itself was
     * already resolved, and distributes it to the objects holding the outdated copies.
     *
     * @param object the object to check
     * @return the objects that close a circuit, empty if the hierarchy is valid
     */
    public static Set<MCRObjectID> findCircuitObjects(MCRObject object) {
        Set<MCRObjectID> circuitObjects = new LinkedHashSet<>();
        List<Element> leaves = new MCRMODSWrapper(object)
            .getElements(".//" + MCRMODSWrapper.LINKED_RELATED_ITEMS + "[not(mods:relatedItem)]");
        for (Element leaf : leaves) {
            Set<MCRObjectID> ids = new HashSet<>(Set.of(object.getId()));
            for (Element item = leaf; item != null && item.getName().equals("relatedItem");
                item = item.getParentElement()) {
                String href = item.getAttributeValue("href", MCRConstants.XLINK_NAMESPACE);
                if (href != null && MCRObjectID.isValid(href) && !ids.add(MCRObjectID.getInstance(href))) {
                    circuitObjects.add(MCRObjectID.getInstance(href));
                    break;
                }
            }
        }
        return circuitObjects;
    }

    /**
     * Removes the shared metadata the object received from its parent and linked objects. The next update of the
     * object receives it again.
     *
     * @return true if the stored shared metadata contains a circuit and was removed
     */
    public boolean clearSharedMetadata(MCRObject object) {
        MCRMODSWrapper wrapper = new MCRMODSWrapper(object);
        try {
            agent.checkHierarchy(wrapper);
            return false;
        } catch (MCRPersistenceException e) {
            wrapper.removeInheritedMetadata();
            return true;
        }
    }

    private Optional<MCRPersistenceException> simulate(MCRObject object, boolean forceDistribution) {
        updated.clear();
        cleared.clear();
        updates = 0;
        try {
            update(copy(object), forceDistribution);
            return Optional.empty();
        } catch (MCRPersistenceException e) {
            return Optional.of(e);
        }
    }

    private void update(MCRObject object) {
        update(object, false);
    }

    private void update(MCRObject object, boolean forceDistribution) {
        if (++updates > maxUpdates) {
            throw new MCRPersistenceException("The update cascade exceeds " + maxUpdates + " objects.");
        }
        MCRObject old = retrieve(object.getId());
        receiveMetadata(object);
        updated.put(object.getId(), object.createXML());
        if (forceDistribution
            || !MCRXMLHelper.deepEqual(old.getMetadata().createXML(), object.getMetadata().createXML())) {
            distributeMetadata(object);
        }
    }

    /** Same steps as {@link MCRMODSMetadataShareAgent#receiveMetadata(MCRObject)}, reading simulated versions. */
    private void receiveMetadata(MCRObject child) {
        MCRMODSWrapper childWrapper = new MCRMODSWrapper(child);
        MCRObjectID parentID = child.getStructure().getParentID();
        childWrapper.removeInheritedMetadata();
        if (parentID != null && MCRMODSWrapper.isSupported(parentID)) {
            agent.inheritToChild(new MCRMODSWrapper(retrieve(parentID)), childWrapper);
        }
        for (Element relatedItem : childWrapper.getLinkedRelatedItems()) {
            String type = relatedItem.getAttributeValue("type");
            String holderId = relatedItem.getAttributeValue("href", MCRConstants.XLINK_NAMESPACE);
            if ((holderId == null || parentID != null && parentID.toString().equals(holderId))
                && MCRMODSRelationshipType.host.name().equals(type)) {
                continue;
            }
            MCRObjectID holderObjectID = MCRObjectID.getInstance(holderId);
            if (MCRMODSWrapper.isSupported(holderObjectID)) {
                relatedItem.addContent(clearContent(new MCRMODSWrapper(retrieve(holderObjectID))));
            }
        }
        agent.checkHierarchy(childWrapper);
    }

    /** Same steps as {@link MCRMODSMetadataShareAgent#distributeMetadata(MCRObject)}. */
    private void distributeMetadata(MCRObject holder) {
        MCRMODSWrapper holderWrapper = new MCRMODSWrapper(holder);
        List<MCRObjectID> childIds = holder.getStructure().getChildren().stream()
            .map(MCRMetaLinkID::getXLinkHrefID)
            .filter(MCRMODSWrapper::isSupported)
            .toList();
        for (MCRObjectID childId : childIds) {
            MCRMODSWrapper childWrapper = new MCRMODSWrapper(retrieveForUpdate(childId));
            agent.inheritToChild(holderWrapper, childWrapper);
            checkHierarchy(childWrapper, "Error while updating inherited metadata");
            update(childWrapper.getMCRObject());
        }
        List<MCRObjectID> recipientIds = referrers.apply(holder.getId()).stream()
            .map(MCRObjectID::getInstance)
            .filter(MCRMODSWrapper::isSupported)
            .toList();
        for (MCRObjectID recipientId : recipientIds) {
            MCRMODSWrapper recipientWrapper = new MCRMODSWrapper(retrieveForUpdate(recipientId));
            for (Element relatedItem : recipientWrapper.getLinkedRelatedItems()) {
                if (holder.getId().toString()
                    .equals(relatedItem.getAttributeValue("href", MCRConstants.XLINK_NAMESPACE))) {
                    @SuppressWarnings("unchecked")
                    Filter<Content> sharedMetadata = (Filter<Content>) Filters.element("part",
                        MCRConstants.MODS_NAMESPACE).negate();
                    relatedItem.removeContent(sharedMetadata);
                    relatedItem.addContent(clearContent(holderWrapper));
                    checkHierarchy(recipientWrapper, "Error while updating shared metadata");
                    update(recipientWrapper.getMCRObject());
                }
            }
        }
    }

    private void checkHierarchy(MCRMODSWrapper wrapper, String message) {
        try {
            agent.checkHierarchy(wrapper);
        } catch (MCRPersistenceException e) {
            throw new MCRPersistenceException(message, e);
        }
    }

    /** Same result as the private <code>getClearContent</code> of {@link MCRMODSMetadataShareAgent}. */
    private List<Content> clearContent(MCRMODSWrapper targetWrapper) {
        List<Content> content = targetWrapper.getMODS().cloneContent();
        content.stream()
            .filter(Element.class::isInstance)
            .map(Element.class::cast)
            .filter(element -> element.getName().equals("relatedItem"))
            .filter(agent::isClearableRelatedItem)
            .forEach(Element::removeContent);
        return content;
    }

    /** Retrieves an object the cascade will update, without outdated shared metadata that contains a circuit. */
    private MCRObject retrieveForUpdate(MCRObjectID id) {
        if (!updated.containsKey(id) && !cleared.containsKey(id)) {
            MCRObject stored = store.apply(id);
            if (clearSharedMetadata(stored)) {
                cleared.put(id, stored.createXML());
            }
        }
        return retrieve(id);
    }

    private MCRObject retrieve(MCRObjectID id) {
        Document document = updated.containsKey(id) ? updated.get(id) : cleared.get(id);
        return document == null ? store.apply(id) : new MCRObject(document.clone());
    }

    private static MCRObject copy(MCRObject object) {
        return new MCRObject(object.createXML());
    }
}
