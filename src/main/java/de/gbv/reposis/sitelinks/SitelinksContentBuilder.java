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
import java.util.List;

import org.mycore.common.content.MCRContent;

/**
 * Interface for building HTML content for sitelinks pages.
 */
public interface SitelinksContentBuilder {

    /**
     * Creates the sitelinks HTML page root.
     *
     * @param years the list of years to include in the page
     * @return {@link MCRContent} representing the HTML content of the page
     * @throws IOException if an error occurs during content generation
     */
    MCRContent createRootPage(List<Integer> years) throws IOException;


    /**
     * Creates a sitelinks HTML page for a specific year containing object links.
     *
     * @param year the year for this page
     * @param page the current page number
     * @param totalCount the total number of objects for the given year
     * @param objectIds the list of object IDs to include
     * @return {@link MCRContent} representing the HTML content of the page
     * @throws IOException if an error occurs during content generation
     */
    MCRContent createPage(int year, int page, long totalCount, List<String> objectIds) throws IOException;

}
