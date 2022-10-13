package de.gbv.reposis.metrics.wos;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;

import com.google.gson.JsonObject;

import de.gbv.reposis.metrics.MCRJournalMetrics;
import de.gbv.reposis.metrics.MCRMetricsProvider;

public class MCRWOSMetricsProvider extends MCRMetricsProvider {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public MCRJournalMetrics getMetrics(Map<String, List<String>> metadata, final Set<Integer> years) {
        MCRJournalMetrics journalMetrics = new MCRJournalMetrics();
        MCRWOSClient client = new MCRWOSClient();

        for (String issn : metadata.get("issn")) {
            try {
                JsonObject searchQueryResult = client.journalsSearchQuery(issn);
                List<String> wosIDs = MCRWOSJsonHelper.getIDsFromSearchResponse(searchQueryResult);
                if (wosIDs.size() == 0) {
                    LOGGER.info("No Results from WebOfScience for issn {}", issn);
                    continue;
                } else if (wosIDs.size() > 1) {
                    LOGGER.warn("No distinct result for issn {} found following WOS ids {}", issn,
                        String.join(",", wosIDs));
                    continue;
                }

                String wosId = wosIDs.get(0);
                JsonObject journalResult = client.journal(wosId);
                List<Integer> wosReportYears = MCRWOSJsonHelper.getReportYearsFromJournal(journalResult);

                Set<Integer> actualRequestYears = new HashSet<>();
                if(years == null || years.size()==0){
                    actualRequestYears = new HashSet<>(wosReportYears);
                } else {
                    actualRequestYears = years.stream().filter(o -> {
                        boolean yearPresent = wosReportYears.contains(o);
                        if(!yearPresent){
                            LOGGER.warn("The requested year {} is not present for WOS id {}", o, wosId);
                        }
                        return yearPresent;
                    }).collect(Collectors.toSet());
                }

                for (Integer requestYear : actualRequestYears) {
                    JsonObject reportYearResponse = client.journalsReportYear(wosId, requestYear);
                    Double jcr = MCRWOSJsonHelper.getJiffFromJournalYearReport(reportYearResponse);
                    journalMetrics.getJCR().put(requestYear, jcr);
                }

            } catch (IOException | InterruptedException e) {
                throw new MCRException("Error while receiving Metrics from WOS!", e);
            }

        }

        return journalMetrics;
    }
}
