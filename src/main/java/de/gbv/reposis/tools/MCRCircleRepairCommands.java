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
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.mycore.mods.MCRMODSWrapper;

import de.gbv.reposis.tools.MCRCircleRepairService.PreviewLink;
import de.gbv.reposis.tools.MCRCircleRepairService.RepairCase;

/**
 * Resolves simple circles: two objects that link to each other with exactly one direct link in each direction and
 * for which the circle overview proposes a change without review of the relations. The removed
 * <code>mods:relatedItem</code> is the one proposed by the overview. All other circles are left untouched.
 *
 * Saving validates the object first, a circle that still exists in the stored objects makes the save fail before
 * anything is written.
 */
@MCRCommandGroup(name = "Circle Repair Commands")
public class MCRCircleRepairCommands {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String REPAIR_SIMPLE_CIRCLE = "repair simple circle for object ";

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
        try {
            MCRMetadataManager.validateObject(object);
        } catch (MCRPersistenceException e) {
            LOGGER.warn("Skipped {}: saving it would fail: {} Resolve that problem first, nothing was changed.",
                object.getId(), causes(e));
            return;
        }
        MCRMetadataManager.update(object);
        LOGGER.info("Removed relatedItem[{}] {} -> {} ({}) to resolve the circle.", removal.position(),
            removal.from(), removal.to(), removal.relation());
    }
}
