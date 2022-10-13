package de.gbv.reposis.metrics.wos;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class MCRWOSJsonHelper {

    public static List<String> getIDsFromSearchResponse(JsonObject response) {
        JsonArray hits = response.get("hits").getAsJsonArray();
        return StreamSupport.stream(hits.spliterator(), false)
                .map(obj->obj.getAsJsonObject().get("id").getAsString())
                .collect(Collectors.toList());
    }


    public static List<Integer> getReportYearsFromJournal(JsonObject response) {
        JsonArray journalCitationReports = response.get("journalCitationReports").getAsJsonArray();

        return StreamSupport.stream(journalCitationReports.spliterator(), false)
                .map(obj -> obj.getAsJsonObject().get("year").getAsInt())
                .collect(Collectors.toList());
    }


    public static Double getJiffFromJournalYearReport(JsonObject response) {
        Optional<String> optJif = Optional.ofNullable(response.get("metrics"))
                .map(JsonElement::getAsJsonObject)
                .map(obj -> obj.get("impactMetrics"))
                .map(JsonElement::getAsJsonObject)
                .map(obj -> obj.get("jif"))
                .map(JsonElement::getAsString);

        return optJif.map(Double::parseDouble).orElse(-1.0);


    }

}
