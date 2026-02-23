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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRException;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstance;

import de.gbv.reposis.sitelinks.dto.SitelinksRootPageDto;
import de.gbv.reposis.sitelinks.dto.SitelinksYearPageDto;

/**
 * Application service for generating sitelinks page data transfer objects.
 * <p>
 * This service provides methods to generate root pages listing all available years
 * and paginated year-specific pages containing object IDs. Year data is cached to
 * optimize performance.
 */
@MCRConfigurationProxy(proxyClass = SitelinksService.Factory.class)
public class SitelinksService {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String CACHE_KEY = "available_years";
    private static final long CACHE_TTL_MS = 5 * 60 * 1000;

    private final SitelinksMetadataService metadataService;
    private final MCRCache<String, Set<Integer>> yearCache;

    /**
     * Constructs a new SitelinksService with the specified metadata service.
     *
     * @param metadataService the service responsible for retrieving object metadata from the domain layer
     */
    public SitelinksService(SitelinksMetadataService metadataService) {
        this.metadataService = metadataService;
        this.yearCache = new MCRCache<>(1, "SitelinksYears");
    }

    /**
     * Retrieves the root page DTO containing all years for which sitelinks data is available.
     *
     * @return a {@link SitelinksRootPageDto} containing all available years
     */
    public SitelinksRootPageDto getRootPage() {
        Set<Integer> years = getAvailableYears();
        List<Integer> sortedYears = years.stream()
            .sorted(Comparator.reverseOrder())
            .toList();
        return new SitelinksRootPageDto(sortedYears);
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
     * @throws SitelinksNotFoundException if no data exists for the specified year
     */
    public SitelinksYearPageDto getYearPage(int year, int page, int pageSize) {
        if (page < 1) {
            throw new MCRException("page must be greater than or equal to 1");
        }
        if (pageSize < 1) {
            throw new MCRException("page size must be greater than or equal to 1");
        }
        if (!hasYearPage(year)) {
            throw SitelinksNotFoundException.forYear(year);
        }

        SitelinksMetadataService.ObjectIdsWithCount data =
            metadataService.getObjectIdsByYear(year, (page - 1) * pageSize, pageSize);
        long totalPages = (data.totalCount() + pageSize - 1) / pageSize;
        if (data.totalCount() == 0 || page > totalPages) {
            throw SitelinksNotFoundException.forPage(year, page, totalPages);
        }

        return new SitelinksYearPageDto(year, page, data.totalCount(), data.objectIds());
    }

    /**
     * Checks if a year has any sitelinks data available.
     *
     * @param year the year to check
     * @return true if the year has sitelinks data, false otherwise
     */
    public boolean hasYearPage(int year) {
        return getAvailableYears().contains(year);
    }

    /**
     * Clears the year cache, forcing a fresh fetch on the next access.
     */
    public void invalidateCache() {
        LOGGER.info("Invalidating sitelinks year cache");
        yearCache.clear();
    }

    private Set<Integer> getAvailableYears() {
        long currentTime = System.currentTimeMillis();
        Set<Integer> cached = yearCache.getIfUpToDate(CACHE_KEY, currentTime - CACHE_TTL_MS);

        if (LOGGER.isDebugEnabled()) {
            if (cached != null) {
                LOGGER.debug("Using cached years data");
                return cached;
            }
            LOGGER.debug("Fetching fresh years data from metadata service");
        }

        Set<Integer> years = new HashSet<>(metadataService.getYearsWithObjects());
        Set<Integer> immutableYears = Set.copyOf(years);
        yearCache.put(CACHE_KEY, immutableYears, currentTime);

        return immutableYears;
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
