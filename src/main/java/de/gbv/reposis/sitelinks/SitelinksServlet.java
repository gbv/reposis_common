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

import java.io.FileNotFoundException;
import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.content.MCRContent;
import org.mycore.frontend.servlets.MCRContentServlet;

/**
 * Servlet for managing "Sitelinks" and their associated data.
 * This class provides endpoints to display years, months, and publications
 * based on the provided parameters.
 */
public class SitelinksServlet extends MCRContentServlet {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String PROP_PREFIX = "SitelinksServlet.";
    private static final String PATH_PAGE = "page";

    private final SitelinksMetadataService metadataService;
    private final SitelinksContentBuilder contentBuilder;
    private final int objectsPerPage;

    /**
     * Constructor for {@code SitelinksServlet}.
     * Initializes the resource with an instance of {@link SitelinksMetadataService}
     * and the page size from the configuration.
     */
    public SitelinksServlet() {
        this(MCRConfiguration2.<SitelinksMetadataService>getSingleInstanceOf(PROP_PREFIX + "MetadataService.Class")
                .orElseThrow(),
            MCRConfiguration2.<SitelinksContentBuilder>getSingleInstanceOf(PROP_PREFIX + "ContentBuilder.Class")
                .orElseThrow(),
            MCRConfiguration2.getString("Sitelinks.ObjectsPerPage").map(Integer::valueOf)
                .orElseThrow(
                    () -> new MCRConfigurationException("Please specify property: 'Sitelinks.ObjectsPerPage'")));
    }

    /**
     * Constructs a new {@code SitelinksServlet} with the given metadata service,
     * page builder, and objects-per-page setting.
     *
     * @param metadataService the service responsible for managing object metadata
     * @param contentBuilder the builder used to generate pages for the servlet
     * @param objectsPerPage the maximum number of objects to display per page
     */
    public SitelinksServlet(SitelinksMetadataService metadataService,
        SitelinksContentBuilder contentBuilder, int objectsPerPage) {
        this.metadataService = metadataService;
        this.contentBuilder = contentBuilder;
        this.objectsPerPage = objectsPerPage;
    }

    @Override
    public MCRContent getContent(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String pathInfo = request.getPathInfo();
        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                // GET /sitelinks -> list years
                return handleRootPage();
            } else {
                pathInfo = pathInfo.replaceAll("/$", "");
                String[] pathParts = pathInfo.substring(1).split("/");
                int year = parseYear(pathParts[0]);
                if (pathParts.length == 1) {
                    // GET /sitelinks/{year} -> list publications for year (page 1)
                    return handlePage(year, 1);
                } else if (pathParts.length == 3 && PATH_PAGE.equals(pathParts[1])) {
                    // GET /sitelinks/{year}/page/{page} -> list publications for year and page
                    int page = parsePage(pathParts[2]);
                    return handlePage(year, page);
                }
            }
        } catch (MCRException e) {
            throw new FileNotFoundException("Invalid year or page number");
        } catch (IOException e) {
            LOGGER.error("Error processing Sitelinks request: pathInfo={}", pathInfo, e);
            throw new IOException("Internal server error during XML transformation");
        }
        throw new FileNotFoundException("Invalid path parameter: " + pathInfo);
    }

    private MCRContent handleRootPage() throws IOException {
        return contentBuilder.createRootPage(metadataService.getYearsWithObjects());
    }

    private MCRContent handlePage(int year, int page) throws IOException {
        SitelinksMetadataService.ObjectIdsWithCount data =
            metadataService.getObjectIdsByYear(year, (page - 1) * objectsPerPage, objectsPerPage);
        return contentBuilder.createPage(year, page, data.totalCount(), data.objectIds());
    }

    private int parseYear(String yearStr) throws IOException {
        try {
            return Integer.parseInt(yearStr);
        } catch (NumberFormatException e) {
            throw new MCRException("Invalid year: " + yearStr, e);
        }
    }

    private int parsePage(String pageStr) throws IOException {
        try {
            int page = Integer.parseInt(pageStr);
            if (page < 1) {
                throw new MCRException("Page number must be >= 1");
            }
            return page;
        } catch (NumberFormatException e) {
            throw new MCRException("Invalid page: " + pageStr, e);
        }
    }

}
