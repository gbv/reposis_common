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

import java.util.Comparator;
import java.util.function.Supplier;

import org.mycore.common.MCRException;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstance;

import de.gbv.reposis.sitelinks.dto.SitelinksRootPageDto;
import de.gbv.reposis.sitelinks.dto.SitelinksYearPageDto;

/**
 * Service for generating sitelinks page DTOs with metadata about objects organized by year.
 * <p>
 * This service provides methods to generate root pages listing all available years
 * and paginated year-specific pages containing object IDs.
 */
@MCRConfigurationProxy(proxyClass = SitelinksService.Factory.class)
public class SitelinksService {

    private final SitelinksMetadataService metadataService;

    /**
     * Constructs a new SitelinksService with the specified metadata service.
     *
     * @param metadataService the service responsible for retrieving object metadata from the domain layer
     */
    public SitelinksService(SitelinksMetadataService metadataService) {
        this.metadataService = metadataService;
    }

    // TODO add cache
    /**
     * Retrieves the root page DTO containing all years for which sitelinks data is available.
     *
     * @return a {@link SitelinksRootPageDto} containing all available years
     */
    public SitelinksRootPageDto getRootPage() {
        return new SitelinksRootPageDto(
            metadataService.getYearsWithObjects().stream().sorted(Comparator.reverseOrder()).toList());
    }

    /**
     * Retrieves a paginated page of object IDs for a specific year.
     *
     * @param year the year for which to retrieve object IDs (e.g., 2024)
     * @param page the page number to retrieve (1-indexed, must be ≥ 1)
     * @param pageSize the maximum number of object IDs to include per page (must be &gt; 0)
     * @return a {@link SitelinksYearPageDto} containing the requested page of object IDs,
     *         pagination metadata, and the total count of objects for the year
     * @throws MCRException if {@code page < 1} or {@code pageSize < 1}
     */
    public SitelinksYearPageDto getYearPage(int year, int page, int pageSize) {
        if (page < 1) {
            throw new MCRException("page must be greater than or equal to 1");
        }
        if (pageSize < 1) {
            throw new MCRException("page size must be greater than or equal to 1");
        }
        SitelinksMetadataService.ObjectIdsWithCount data =
            metadataService.getObjectIdsByYear(year, (page - 1) * pageSize, pageSize);
        return new SitelinksYearPageDto(year, page, data.totalCount(), data.objectIds());
    }

    /**
     * Factory class for creating {@link SitelinksService} instances via configuration.
     * <p>
     * This factory is used by the {@link MCRConfigurationProxy} annotation to automatically
     * instantiate the service with configuration values from properties.
     */
    public static class Factory implements Supplier<SitelinksService> {

        @MCRInstance(name = "MetadataService", valueClass = SitelinksMetadataService.class)
        public SitelinksMetadataService metadataService;

        @Override
        public SitelinksService get() {
            return new SitelinksService(metadataService);
        }
    }
}
