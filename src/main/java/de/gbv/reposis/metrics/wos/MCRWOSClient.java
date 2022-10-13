/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.gbv.reposis.metrics.wos;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Locale;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;

import com.google.common.util.concurrent.RateLimiter;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class MCRWOSClient {

    public static final String HEADER_X_API_KEY = "x-ApiKey";
    private static final String API_URL_PROPERTY = "MIR.WebOfScience.API.URL";
    private static final String API_KEY_PROPERTY = "MIR.WebOfScience.API.Key";
    private static final Logger LOGGER = LogManager.getLogger();
    private static final RateLimiter RATE_LIMITER = RateLimiter.create(2.0);
    private final static String JOURNAL_URL_TEMPLATE = "%swos-journals/v1/journals/%s";
    private final static String JOURNAL_REPORT_YEAR_URL_TEMPLATE = "%swos-journals/v1/journals/%s/reports/year/%s";
    private final static String JOURNAL_SEARCH_QUERY_URL_TEMPLATE = "%swos-journals/v1/journals/?q=%s";
    private String key;
    private String baseURL;

    public MCRWOSClient() {
        this(MCRConfiguration2.getStringOrThrow(API_KEY_PROPERTY),
            MCRConfiguration2.getStringOrThrow(API_URL_PROPERTY));
    }

    public MCRWOSClient(String key, String baseURL) {
        this.key = key;
        this.baseURL = baseURL;
    }

    public JsonObject journalsSearchQuery(String query) throws IOException, InterruptedException {
        Objects.requireNonNull(key, "API Key must be not null!");
        Objects.requireNonNull(query, "Query must be not null!");

        LOGGER.info("WOS: Search journal with issn '{}'", query);

        String requestUriString
            = String.format(Locale.ROOT, JOURNAL_SEARCH_QUERY_URL_TEMPLATE, getBaseURL(), query);

        return executeGetRequest(requestUriString).getAsJsonObject();
    }

    public JsonObject journalsReportYear(String journalId, int year)
        throws IOException, InterruptedException {
        Objects.requireNonNull(key, "API Key must be not null!");
        Objects.requireNonNull(journalId, "JournalId must be not null!");

        LOGGER.info("WOS: Request metrics report for journal '{}' in year '{}'", journalId, year);

        String requestUriString
            = String.format(Locale.ROOT, JOURNAL_REPORT_YEAR_URL_TEMPLATE, getBaseURL(), journalId, year);
        JsonElement jsonElement = executeGetRequest(requestUriString);

        return jsonElement.getAsJsonObject();
    }

    public JsonObject journal(String id) throws IOException, InterruptedException {
        Objects.requireNonNull(key, "API Key must be not null!");
        Objects.requireNonNull(id, "Id must be not null!");

        LOGGER.info("WOS: Request journal information for journal '{}'", id);
        String requestUriString = String.format(Locale.ROOT, JOURNAL_URL_TEMPLATE, getBaseURL(), id);

        return executeGetRequest(requestUriString).getAsJsonObject();
    }

    protected JsonElement executeGetRequest(String requestUriString) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest httpRequest;
        try {
            RATE_LIMITER.acquire();
            httpRequest = HttpRequest.newBuilder()
                .GET()
                .uri(new URI(requestUriString))
                .header(HEADER_X_API_KEY, key).build();
        } catch (URISyntaxException e) {
            throw new MCRConfigurationException("URL seems to be invalid " + requestUriString, e);
        }
        HttpResponse<String> stringHttpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        return JsonParser.parseString(stringHttpResponse.body());
    }

    public String getBaseURL() {
        return baseURL;
    }

    public void setBaseURL(String baseURL) {
        this.baseURL = baseURL;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
