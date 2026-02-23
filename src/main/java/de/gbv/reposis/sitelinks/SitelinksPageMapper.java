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

import org.mycore.common.content.MCRContent;

import de.gbv.reposis.sitelinks.dto.SitelinksRootPageDto;
import de.gbv.reposis.sitelinks.dto.SitelinksYearPageDto;

/**
 * Mapper interface for transforming sitelinks DTOs into content representations.
 * <p>
 * This interface defines methods for mapping different types of sitelinks pages
 * (root pages and year-specific pages) into {@link MCRContent} objects that can be
 * rendered or further processed.
 */
public interface SitelinksPageMapper {

    /**
     * Maps a sitelinks root page DTO to content.
     *
     * @param rootPage the root page to be mapped
     * @return the mapped content representation
     * @throws SitelinksMappingException if an error occurs during mapping
     */
    MCRContent map(SitelinksRootPageDto rootPage) throws SitelinksMappingException;

    /**
     * Maps a sitelinks year page DTO to content.
     *
     * @param yearPage the year-specific page to be mapped
     * @return the mapped content representation
     * @throws SitelinksMappingException if an error occurs during mapping
     */
    MCRContent map(SitelinksYearPageDto yearPage) throws SitelinksMappingException;
}
