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

package de.gbv.reposis.sitelinks;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.FacetParams;
import org.mycore.common.MCRException;
import org.mycore.solr.MCRSolrClientFactory;

/**
 * Service for retrieving object-related metadata from Solr.
 * Provides methods to fetch years, months, and object IDs for which objects have been issued.
 */
public class ObjectMetadataService {

    private static final String FIELD_ID = "id";

    private static final String FIELD_YEAR_ISSUED = "mods.yearIssued";

    private static final String FIELD_DATE_ISSUED = "mods.dateIssued";

    private static final String DEFAULT_SOLR_QUERY = "*:*";

    private final SolrClient solrClient;

    private final String basicFilterQuery;

    /**
     * Constructs an instance of {@link ObjectMetadataService} using the provided filter query.
     *
     *@param basicFilterQuery a Solr filter query applied to all queries
     */
    public ObjectMetadataService (String basicFilterQuery) {
        this(MCRSolrClientFactory.getMainSolrClient(), basicFilterQuery);
    }

    /**
     * Constructs a new {@code ObjectMetadataService}.
     *
     * @param solrClient the Solr client used to execute queries
     * @param basicFilterQuery a Solr filter query applied to all queries
     */
    public ObjectMetadataService(SolrClient solrClient, String basicFilterQuery) {
        this.solrClient = solrClient;
        this.basicFilterQuery = basicFilterQuery;
    }

    // TODO easy cachable
    /**
     * Retrieves all years for which objects exist with an issued date.
     *
     * @return a list of years as, sorted in descending order
     * @throws MCRException if a Solr query or I/O error occurs
     */
    public List<Integer> getYearsWithObjects() {
        final SolrQuery query = new SolrQuery(DEFAULT_SOLR_QUERY);
        query.setRows(0);
        query.addFilterQuery(basicFilterQuery);
        query.setFacet(true);
        query.addFacetField(FIELD_YEAR_ISSUED);
        query.setFacetSort(FacetParams.FACET_SORT_INDEX);
        query.setFacetLimit(-1);
        try {
            return solrClient.query(query).getFacetField(FIELD_YEAR_ISSUED).getValues()
                .stream().map(FacetField.Count::getName).map(Integer::parseInt).sorted(Comparator.reverseOrder())
                .toList();
        } catch (SolrServerException | IOException e) {
            throw new MCRException(e);
        }
    }

    /**
     * Retrieves object IDs for objects issued in a specific year, with support for pagination.
     *
     * @param year the year of the issued objects (e.g., 2021)
     * @param offset the offset from where to start fetching results (for pagination)
     * @param limit the maximum number of results to fetch (for pagination)
     * @return an {@link ObjectIdsWithCount} object containing a list of object IDs and total count
     * @throws MCRException if a Solr query or I/O error occurs, or if the query execution fails
     */
    public ObjectIdsWithCount getObjectIdsByDate(int year, int offset, int limit) {
        final SolrQuery query = new SolrQuery(DEFAULT_SOLR_QUERY);
        query.addFilterQuery(basicFilterQuery);
        query.addFilterQuery(String.format(Locale.ROOT, FIELD_DATE_ISSUED + ":%s-*", year));
        query.setFields(FIELD_ID);
        query.setStart(offset);
        query.setRows(limit);
        try {
            final QueryResponse response = solrClient.query(query);
            long totalCount = response.getResults().getNumFound();
            final List<String> objectIds =
                response.getResults().stream().map((document) -> (String) document.getFieldValue(FIELD_ID)).toList();
            return new ObjectIdsWithCount(objectIds, totalCount);
        } catch (SolrServerException | IOException e) {
            throw new MCRException(e);
        }
    }

    /**
     * A simple record to store a list of object IDs along with the total count of matching objects.
     *
     * @param objectIds the list of object IDs that were issued in the specified year and month
     * @param totalCount the total number of objects that match the query criteria (not limited by pagination)
     */
    public record ObjectIdsWithCount(List<String> objectIds, long totalCount) {}
}
