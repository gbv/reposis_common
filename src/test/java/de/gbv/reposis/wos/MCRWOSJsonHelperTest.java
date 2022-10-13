package de.gbv.reposis.wos;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.gbv.reposis.metrics.wos.MCRWOSJsonHelper;
import org.junit.Assert;
import org.junit.Test;
import org.mycore.common.MCRClassTools;
import org.mycore.common.MCRTestCase;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MCRWOSJsonHelperTest extends MCRTestCase {

    private JsonObject jsonObj;

    @Test
    public void getIDsFromSearchResponse() throws IOException {
        jsonObj = loadJson("wos/journals.search.response.json");
        List<String> ids = MCRWOSJsonHelper.getIDsFromSearchResponse(jsonObj);

        Assert.assertEquals(1, ids.size());
        Assert.assertEquals("FOO_TWO", ids.get(0));
    }

    @Test
    public void getReportYearsFromJournal() throws IOException {
        jsonObj = loadJson("wos/journals.response.json");
        List<Integer> actualReportYears = MCRWOSJsonHelper.getReportYearsFromJournal(jsonObj);
        List<Integer> expectedReportYears = IntStream.range(2009, 2021).boxed().collect(Collectors.toList());

        for (Integer expectedReportYear : expectedReportYears) {
            Assert.assertTrue(actualReportYears.stream().map(Object::toString).collect(Collectors.joining())
                + " should contain " + expectedReportYear, actualReportYears.contains(expectedReportYear));
        }
    }

    @Test
    public void getJiffFromJournalYearReport() throws IOException {
        jsonObj = loadJson("wos/journals.report.year.response.json");
        Double jiffFromJournalYearReport = MCRWOSJsonHelper.getJiffFromJournalYearReport(jsonObj);
        Assert.assertEquals(Double.valueOf(7.123), jiffFromJournalYearReport);
    }

    private JsonObject loadJson(String url) throws IOException {
        try (InputStream is = MCRClassTools.getClassLoader().getResourceAsStream(url);
            InputStreamReader isr = new InputStreamReader(is)) {
            return JsonParser.parseReader(isr).getAsJsonObject();
        }
    }
}
