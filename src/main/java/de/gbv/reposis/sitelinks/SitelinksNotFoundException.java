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

import java.io.Serial;
import java.util.Locale;

import org.mycore.common.MCRException;

/**
 * Exception thrown when a requested sitelinks service or resource does not exist.
 * <p>
 * This exception is typically thrown when attempting to access data for a year
 * that has no associated sitelinks, or when a requested page is out of bounds.
 */
public class SitelinksNotFoundException extends MCRException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message the detail message
     */
    public SitelinksNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception for a non-existent year.
     *
     * @param year the year that was not found
     * @return a new exception instance
     */
    public static SitelinksNotFoundException forYear(int year) {
        return new SitelinksNotFoundException("No sitelinks data found for year: " + year);
    }

    /**
     * Constructs a new exception for an invalid page.
     *
     * @param year the year
     * @param page the invalid page number
     * @param totalPages the total number of available pages
     * @return a new exception instance
     */
    public static SitelinksNotFoundException forPage(int year, int page, long totalPages) {
        return new SitelinksNotFoundException(
            String.format(Locale.ROOT,"Page %d does not exist for year %d (total pages: %d)", page, year, totalPages));
    }
}
