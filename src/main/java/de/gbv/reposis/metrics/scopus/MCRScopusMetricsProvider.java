package de.gbv.reposis.metrics.scopus;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration2;

import de.gbv.reposis.metrics.MCRJournalMetrics;
import de.gbv.reposis.metrics.MCRMetricsProvider;

public class MCRScopusMetricsProvider extends MCRMetricsProvider {

    private static final String ScopusApiUrl = MCRConfiguration2.getString("MIR.Scopus.API.URL").orElse(null);
    private static final String ScopusApiKey = MCRConfiguration2.getString("MIR.Scopus.API.Key").orElse(null);

    private static final Logger LOGGER = LogManager.getLogger();

    public static void applyValues(Element MetricList, MCRJournalMetrics metrics, String type,
        Set<Integer> yearFilter) {
        Map<Integer, Double> map = switch (type) {
        case "SNIP" -> metrics.getSnip();
        case "SJR" -> metrics.getSJR();
        default -> throw new MCRException("Unknown type " + type + "!");
        };

        for (Element metric : MetricList.getChildren()) {
            String valueStr = metric.getText();
            String yearStr = metric.getAttributeValue("year");
            int year = Integer.parseInt(yearStr);
            double value = Double.parseDouble(valueStr);
            if (value > 0) {
                if (yearFilter.isEmpty() || yearFilter.contains(year)) {
                    if (map.containsKey(year) && map.get(year) != value && map.get(year) > 0) {
                        LOGGER.warn("{} existing data for {}, replace {} with {}!",
                            type, year, map.get(year), value);
                    }
                    map.put(year, value);
                }
            }
        }
    }

    @Override
    public MCRJournalMetrics getMetrics(Map<String, List<String>> metadata, Set<Integer> years) {
        if (ScopusApiUrl == null || ScopusApiKey == null) {
            return null;
        }
        List<String> issnList = metadata.get("issn");
        if (issnList == null) {
            return null;
        }

        Integer lowestYear = years.stream().min(Integer::compareTo).orElse(1);
        Integer highestYear = years.stream().min(Integer::compareTo)
            .orElse(Calendar.getInstance(TimeZone.getDefault(), Locale.ROOT).get(Calendar.YEAR));

        final MCRJournalMetrics metrics = new MCRJournalMetrics();
        for (String issn : issnList) {
            LOGGER.info("SCOPUS: Requesting metrics for issn {} and years {}-{}", issn, lowestYear, highestYear);
            String requestURI = ScopusApiUrl + "serial/title/issn/" + issn
                + "?field=SJR,SNIP&view=STANDARD&apiKey=" + ScopusApiKey
                + "&httpAccept=text/xml&date=" + lowestYear + "-" + highestYear;
            HttpClient client = HttpClient.newHttpClient();
            try {
                HttpRequest request = HttpRequest.newBuilder().GET().uri(new URI(requestURI)).build();
                HttpResponse<InputStream> resp = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
                int response = resp.statusCode();
                if (response == 200) {
                    try (InputStream is = resp.body()) {
                        SAXBuilder saxBuilder = new SAXBuilder();
                        Document document = saxBuilder.build(is);
                        Element root = document.getRootElement();
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("received xml from source: " + new XMLOutputter().outputString(root));
                        }

                        if (root.getChild("error") != null) {
                            LOGGER.error("Error from Scopus API:" + root.getChild("error").getText());
                            continue;
                        }
                        Element entry = root.getChild("entry");
                        if (entry.getChild("SNIPList") == null && entry.getChild("SJRList") == null) {
                            LOGGER.info("No SNIP or SJR Metrics for " + issn);
                            continue;
                        }

                        Optional.ofNullable(entry.getChild("SNIPList"))
                            .ifPresent(snipList -> applyValues(snipList, metrics, "SNIP", years));

                        Optional.ofNullable(entry.getChild("SJRList"))
                            .ifPresent(snipList -> applyValues(snipList, metrics, "SJR", years));
                    } catch (JDOMException e) {
                        LOGGER.error("Error while updating scopus metrics!", e);
                    }
                } else if (response == 401 || response == 403) {
                    LOGGER.error("Access denied to the Scopus API - didn't add Scopus Metrics");
                } else {
                    LOGGER.info("Unknown '{}' Response from ScopusAPI - didn't add Scopus Metrics", resp.statusCode());
                }
            } catch (URISyntaxException e) {
                LOGGER.error("MalformedURLException - didn't add Scopus Metrics", e);
            } catch (IOException | InterruptedException e) {
                LOGGER.error("IOException - didn't add Scopus Metrics", e);
            }
        }

        return metrics;
    }
}
