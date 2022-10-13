package de.gbv.reposis.metrics;

import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.access.MCRAccessException;
import org.mycore.crypt.MCRCryptKeyFileNotFoundException;
import org.mycore.crypt.MCRCryptKeyNoPermissionException;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.mycore.mods.MCRMODSWrapper;

@MCRCommandGroup(name = "Mods Journal Metrics Commands")
public class MCRMODSJournalMetricCommands {

    private static final Logger LOGGER = LogManager.getLogger();

    @MCRCommand(syntax = "update journal metrics for object {0} for years {1}",
        help = "",
        order = 30)
    public static void updateMetricsForObject(String objidStr, String commaYears)
        throws MCRCryptKeyNoPermissionException, MCRCryptKeyFileNotFoundException, MCRAccessException {
        if (!MCRObjectID.isValid(objidStr)) {
            LOGGER.error("The String {} is not a valid object id!", objidStr);
            return;
        }

        MCRObjectID objId = MCRObjectID.getInstance(objidStr);
        if (!MCRMetadataManager.exists(objId)) {
            LOGGER.error("Object {} does not exist!", objidStr);
            return;
        }

        Set<Integer> years = Stream.of(commaYears.split(","))
            .filter(Predicate.not(String::isBlank))
            .map(Integer::parseInt)
            .collect(Collectors.toSet());

        MCRObject mcrObject = MCRMetadataManager.retrieveMCRObject(objId);
        MCRMODSWrapper mw = new MCRMODSWrapper(mcrObject);

        if (MCRMODSJournalMetricsHelper.updateMetrics(MCRMODSJournalMetricsHelper.getIssn(mw), mw, years)) {
            LOGGER.info("The Metrics of {} changed. Update the Object", objidStr);
            MCRMetadataManager.update(mcrObject);
        } else {
            LOGGER.info("The Metrics of {} did not change.", objidStr);
        }
    }

    @MCRCommand(syntax = "update journal metrics for object {0}",
        help = "",
        order = 40)
    public static void updateMetricsForObject(String objidStr)
        throws MCRCryptKeyNoPermissionException, MCRCryptKeyFileNotFoundException, MCRAccessException {
        updateMetricsForObject(objidStr, "");
    }

}
