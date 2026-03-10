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

package de.gbv.reposis.sitelinks.dto;

import java.util.List;

/**
 * Data transfer object representing a paginated page of sitelinks for a specific year.
 *
 * @param year the year for which this page contains sitelinks
 * @param page the current page number
 * @param totalCount the total number of objects available for this year across all pages
 * @param objectIds the list of object IDs on this specific page
 */
public record SitelinksYearPageDto(int year, int page, long totalCount, List<String> objectIds) {
}
