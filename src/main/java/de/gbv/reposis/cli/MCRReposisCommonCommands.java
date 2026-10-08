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

package de.gbv.reposis.cli;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.access.MCRAccessException;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.mycore.mods.MCRMODSWrapper;

import de.gbv.reposis.mods.MCRMODSNamespaceNormalizer;

/**
 * The command line commands of reposis_common.
 */
@MCRCommandGroup(name = "Reposis Common Commands")
public class MCRReposisCommonCommands {

    private static final Logger LOGGER = LogManager.getLogger();

    @MCRCommand(
        syntax = "normalize mods namespaces of type mods",
        help = "Declares the used namespaces of every mods document at its mods element and drops the redundant"
            + " and the unused declarations below it. Objects that are already normalized are not stored again.",
        order = 10)
    public static List<String> normalizeObjectsOfTypeMods() {
        return toCommands(MCRXMLMetadataManager.instance().listIDsOfType(MCRMODSWrapper.MODS_OBJECT_TYPE));
    }

    @MCRCommand(
        syntax = "normalize mods namespaces of type mods and project {0}",
        help = "Like 'normalize mods namespaces of type mods', but only for the objects of the project {0}, like"
            + " 'openagrar'.",
        order = 20)
    public static List<String> normalizeObjectsOfTypeModsAndProject(String project) {
        String base = project + "_" + MCRMODSWrapper.MODS_OBJECT_TYPE;
        return toCommands(MCRXMLMetadataManager.instance().listIDsForBase(base));
    }

    @MCRCommand(
        syntax = "normalize mods namespaces of object {0}",
        help = "Declares the used namespaces of the mods document of the object {0} at its mods element and drops"
            + " the redundant and the unused declarations below it. The object is only stored if it changed.",
        order = 30)
    public static void normalizeObject(String objectIdStr) throws MCRAccessException {
        if (!MCRObjectID.isValid(objectIdStr)) {
            LOGGER.error("The string {} is not a valid object id!", objectIdStr);
            return;
        }

        MCRObjectID objectId = MCRObjectID.getInstance(objectIdStr);
        if (!MCRMetadataManager.exists(objectId)) {
            LOGGER.error("Object {} does not exist!", objectIdStr);
            return;
        }
        if (!MCRMODSWrapper.isSupported(objectId)) {
            LOGGER.info("Object {} is no mods object, nothing to do.", objectIdStr);
            return;
        }

        MCRObject object = MCRMetadataManager.retrieveMCRObject(objectId);
        Element mods = new MCRMODSWrapper(object).getMODS();
        if (mods == null) {
            LOGGER.warn("Object {} has no mods element, nothing to do.", objectIdStr);
            return;
        }

        if (!MCRMODSNamespaceNormalizer.normalize(mods)) {
            LOGGER.info("The namespace declarations of {} are already normalized.", objectIdStr);
            return;
        }

        MCRMetadataManager.update(object);
        LOGGER.info("Normalized the namespace declarations of {}.", objectIdStr);
    }

    private static List<String> toCommands(List<String> objectIds) {
        return objectIds.stream()
            .map(objectId -> "normalize mods namespaces of object " + objectId)
            .collect(Collectors.toList());
    }
}
