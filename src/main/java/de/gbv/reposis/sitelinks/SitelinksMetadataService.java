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

import java.util.List;

import org.mycore.common.MCRException;

import de.gbv.reposis.sitelinks.dto.SitelinksObjectDto;

/**
 * Service interface for retrieving object metadata used to generate sitelinks for search engine crawlers.
 * <p>
 * Provides methods to fetch years and paginated objects based on publication dates.
 * Implementations should ensure results are sorted by publication date (newest first) for optimal
 * crawler indexing.
 */
public interface SitelinksMetadataService {

    /**
     * Retrieves all years for which objects exist with an issued date.
     *
     * @return a list of years
     * @throws MCRException if a Solr query or I/O error occurs
     */
    List<Integer> getYearsWithObjects();

    /**
     * Retrieves objects issued in a specific year, with support for pagination.
     * Results are sorted primarily by issued date (descending), then by creation timestamp (descending).
     *
     * @param year   the year of the issued objects (e.g., 2021)
     * @param offset the offset from which to start fetching results (for pagination)
     * @param limit  the maximum number of results to fetch (for pagination)
     * @return an {@link ObjectsWithCount} object containing a list of objects and the total count
     * @throws MCRException if a query or I/O error occurs
     */
    ObjectsWithCount getObjectsByYear(int year, int offset, int limit);


    /**
     * Container for paginated object results.
     *
     * @param objects    the list of objects for the requested page
     * @param totalCount the total number of objects matching the query (across all pages)
     */
    record ObjectsWithCount(List<SitelinksObjectDto> objects, long totalCount) {
    }

}
