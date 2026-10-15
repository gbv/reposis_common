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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRException;
import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.mycore.mods.MCRMODSShareCascadeSimulator;
import org.mycore.mods.MCRMODSWrapper;

import de.gbv.reposis.tools.MCRCircleRepairService.PreviewLink;
import de.gbv.reposis.tools.MCRCircleRepairService.RepairCase;

/**
 * Resolves simple circles: two objects that link to each other with exactly one direct link in each direction and
 * for which the circle overview proposes a change without review of the relations. The removed
 * <code>mods:relatedItem</code> is the one proposed by the overview. All other circles are left untouched.
 *
 * Saving an object distributes its shared metadata to its children and to the objects that link to it. If another
 * circle in that cascade would make saving fail, the repair is skipped and nothing is written, see
 * {@link MCRMODSShareCascadeSimulator}. Outdated shared metadata that still contains the circle, e.g. in the children
 * of a circle member, is cleared before the repair and refilled by it. The size of the simulated cascade is limited by
 * <code>MCR.CircleRepair.MaxCascadeUpdates</code>.
 *
 * A circle that was resolved in another way, e.g. in the editor, can leave outdated shared metadata behind, which the
 * circle overview does not show because the link index no longer contains the circle. Saving any object holding such
 * a copy fails. The refresh commands rebuild these copies from the object that closes the circuit.
 */
@MCRCommandGroup(name = "Circle Repair Commands")
public class MCRCircleRepairCommands {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String MAX_CASCADE_UPDATES = "MCR.CircleRepair.MaxCascadeUpdates";

    private static final String REPAIR_SIMPLE_CIRCLE = "repair simple circle for object ";

    private static final String REFRESH_SHARED_METADATA = "refresh shared metadata of object ";

    private static final int PROGRESS_STEP = 10000;

    /**
     * Removes the proposed link of the simple circle that contains the given object.
     *
     * @param id the id of one of the two objects of the circle
     */
    @MCRCommand(syntax = REPAIR_SIMPLE_CIRCLE + "{0}",
        help = "Removes the proposed relatedItem of the simple circle (two objects, one link in each direction,"
            + " no manual review needed) that contains the object {0}. Other circles are left untouched.",
        order = 10)
    public static void repairSimpleCircle(String id) throws Exception {
        if (!MCRObjectID.isValid(id) || !MCRMetadataManager.exists(MCRObjectID.getInstance(id))) {
            LOGGER.error("Object {} does not exist.", id);
            return;
        }
        Optional<RepairCase> repairCase = MCRCircleRepairService.caseOf(MCRCircleRepairService.repository(), id);
        if (repairCase.isEmpty()) {
            LOGGER.info("Object {} is not part of a circle.", id);
            return;
        }
        Optional<PreviewLink> removal = MCRCircleRepairService.simpleRemoval(repairCase.get());
        if (removal.isEmpty()) {
            LOGGER.warn("The circle {} of object {} is not a simple circle and needs manual review.",
                repairCase.get().members(), id);
            return;
        }
        removeRelatedItem(removal.get());
    }

    /**
     * Detects all circles like the circle overview and repairs the simple ones.
     *
     * @return one repair command for every simple circle
     */
    @MCRCommand(syntax = "repair all simple circles",
        help = "Detects circles like the circle overview and runs '" + REPAIR_SIMPLE_CIRCLE + "{0}' for every simple"
            + " circle (two objects, one link in each direction, no manual review needed).",
        order = 20)
    public static List<String> repairAllSimpleCircles() throws Exception {
        List<RepairCase> cases = MCRCircleRepairService
            .previewIndexed(MCRCircleRepairService.repository(), MCRCircleRepairService.indexedLinks()).cases();
        List<String> commands = cases.stream()
            .map(MCRCircleRepairService::simpleRemoval)
            .flatMap(Optional::stream)
            .map(removal -> REPAIR_SIMPLE_CIRCLE + removal.from())
            .toList();
        LOGGER.info("Found {} circle groups, {} of them are simple and will be repaired.", cases.size(),
            commands.size());
        return commands;
    }

    /**
     * Rebuilds the shared metadata of the given object and distributes it to its children and the objects that link to
     * it, like <code>repair shared metadata for the ID {0}</code>. Outdated copies with a circuit in that cascade are
     * cleared first, so the distribution does not fail on them.
     *
     * @param id the id of the object that closes the circuit in the outdated copies
     */
    @MCRCommand(syntax = REFRESH_SHARED_METADATA + "{0}",
        help = "Rebuilds the shared metadata of object {0} and distributes it. Outdated copies with a circuit in the"
            + " resulting cascade are cleared before and refilled by the distribution. Use it on the object named in"
            + " 'contains ciruit by object {0}' after the circle itself was resolved.",
        order = 30)
    public static void refreshSharedMetadata(String id) throws MCRAccessException {
        if (!MCRObjectID.isValid(id) || !MCRMetadataManager.exists(MCRObjectID.getInstance(id))) {
            LOGGER.error("Object {} does not exist.", id);
            return;
        }
        MCRObject object = MCRMetadataManager.retrieveMCRObject(MCRObjectID.getInstance(id));
        if (saveWithCascade(object, true)) {
            LOGGER.info("Refreshed the shared metadata of {}.", id);
        }
    }

    /**
     * Checks the stored shared metadata of all MODS objects for circuits and refreshes it from every object that closes
     * such a circuit.
     *
     * @return one refresh command for every object that closes a circuit
     */
    @MCRCommand(syntax = "refresh all outdated shared metadata",
        help = "Checks the shared metadata of all MODS objects for circuits, as saving does, and runs '"
            + REFRESH_SHARED_METADATA + "{0}' for every object that closes such a circuit.",
        order = 40)
    public static List<String> refreshAllOutdatedSharedMetadata() {
        List<MCRObjectID> ids = MCRXMLMetadataManager.instance().listIDs().stream()
            .map(MCRObjectID::getInstance)
            .filter(MCRMODSWrapper::isSupported)
            .sorted()
            .toList();
        Set<MCRObjectID> circuitObjects = new LinkedHashSet<>();
        int outdated = 0;
        for (int index = 0; index < ids.size(); index++) {
            MCRObjectID id = ids.get(index);
            Set<MCRObjectID> found = MCRMODSShareCascadeSimulator
                .findCircuitObjects(MCRMetadataManager.retrieveMCRObject(id));
            if (!found.isEmpty()) {
                outdated++;
                LOGGER.info("Outdated shared metadata in {} with a circuit by {}", id, found);
                circuitObjects.addAll(found);
            }
            if ((index + 1) % PROGRESS_STEP == 0) {
                LOGGER.info("Checked {} of {} objects.", index + 1, ids.size());
            }
        }
        LOGGER.info("Found {} objects with outdated shared metadata, closed by the objects {}.", outdated,
            circuitObjects);
        return circuitObjects.stream()
            .map(id -> REFRESH_SHARED_METADATA + id)
            .toList();
    }

    /**
     * Saves the object only if the whole cascade of distributed metadata would succeed. Outdated shared metadata with
     * a circuit in that cascade is cleared before, the save refills it.
     *
     * @param object the object to save
     * @param repairShared true to distribute the shared metadata even if the object itself does not change
     * @return true if the object was saved
     */
    private static boolean saveWithCascade(MCRObject object, boolean repairShared) throws MCRAccessException {
        MCRMODSShareCascadeSimulator simulator = MCRMODSShareCascadeSimulator
            .ofRepository(MCRConfiguration2.getInt(MAX_CASCADE_UPDATES).orElseThrow(
                () -> MCRConfiguration2.createConfigurationException(MAX_CASCADE_UPDATES)));
        Optional<MCRPersistenceException> failure = repairShared
            ? simulator.simulateSharedMetadataRepair(object)
            : simulator.simulateUpdate(object);
        if (failure.isPresent()) {
            LOGGER.warn("Skipped {}: saving it would fail while distributing the shared metadata: {} Resolve that"
                + " problem first, nothing was changed.", object.getId(), causes(failure.get()));
            return false;
        }
        List<MCRObjectID> outdated = simulator.getClearedObjects();
        if (!outdated.isEmpty()) {
            LOGGER.info("Clearing outdated shared metadata with circuits of {} objects: {}", outdated.size(),
                outdated);
        }
        for (MCRObjectID outdatedId : outdated) {
            // Store without receiving or distributing metadata; the following save refills the shared metadata.
            MCRObject outdatedObject = MCRMetadataManager.retrieveMCRObject(outdatedId);
            if (simulator.clearSharedMetadata(outdatedObject)) {
                MCRMetadataManager.fireUpdateEvent(outdatedObject);
            }
        }
        if (repairShared) {
            MCRMetadataManager.repairSharedMetadata(object);
        } else {
            MCRMetadataManager.update(object);
        }
        return true;
    }

    private static String causes(Throwable failure) {
        StringBuilder result = new StringBuilder();
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            result.append(cause.getMessage()).append(cause.getCause() == null ? "." : " > ");
        }
        return result.toString();
    }

    private static void removeRelatedItem(PreviewLink removal) throws Exception {
        MCRObject object = MCRMetadataManager.retrieveMCRObject(MCRObjectID.getInstance(removal.from()));
        List<Element> items = new MCRMODSWrapper(object).getMODS()
            .getChildren("relatedItem", MCRConstants.MODS_NAMESPACE);
        int index = removal.position() - 1;
        Element item = index >= 0 && index < items.size() ? items.get(index) : null;
        if (item == null
            || !removal.to().equals(item.getAttributeValue("href", MCRConstants.XLINK_NAMESPACE))
            || !removal.relation().equals(item.getAttributeValue("type", MCRObjectLinkGraph.RELATION_UNKNOWN))) {
            throw new MCRException("relatedItem[" + removal.position() + "] of " + removal.from()
                + " no longer links to " + removal.to() + " with type " + removal.relation() + ".");
        }
        item.detach();
        if (saveWithCascade(object, false)) {
            LOGGER.info("Removed relatedItem[{}] {} -> {} ({}) to resolve the circle.", removal.position(),
                removal.from(), removal.to(), removal.relation());
        }
    }
}
