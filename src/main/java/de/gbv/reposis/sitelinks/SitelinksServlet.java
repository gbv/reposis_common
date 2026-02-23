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
import java.util.Optional;

import de.gbv.reposis.sitelinks.dto.SitelinksYearPageDto;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.content.MCRContent;
import org.mycore.frontend.servlets.MCRContentServlet;

/**
 * Servlet for managing "Sitelinks" and their associated data.
 * <p>
 * Provides endpoints to display years, months, and publications based on the
 * requested path parameters. Supports pagination and XML content mapping.
 */
public class SitelinksServlet extends MCRContentServlet {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String PROP_PREFIX = "SitelinksServlet.";
    private static final String PATH_PAGE = "page";

    private SitelinksService service;
    private SitelinksPageMapper mapper;
    private int pageSize;

    /**
     * Default constructor for Servlet container.
     * <p>
     * Initialization is done in {@link #init()}.
     */
    public SitelinksServlet() {
        // Leave empty, container will call init()
    }

    /**
     * Constructor for manual instantiation / unit tests.
     *
     * @param service the service responsible for managing object metadata
     * @param mapper the mapper used to transform pages into content representations
     * @param pageSize the maximum number of objects to display per page
     */
    protected SitelinksServlet(SitelinksService service, SitelinksPageMapper mapper, int pageSize) {
        this.service = service;
        this.mapper = mapper;
        this.pageSize = pageSize;
    }

    /**
     * Initializes the servlet from configuration.
     *
     * @throws ServletException if initialization fails
     */
    @Override
    public void init() throws ServletException {
        super.init();
        try {
            this.service = MCRConfiguration2.<SitelinksService>getSingleInstanceOf(PROP_PREFIX + "Service.Class")
                .orElseThrow(() -> new MCRConfigurationException("Missing SitelinksService configuration"));
            this.mapper = MCRConfiguration2.<SitelinksPageMapper>getSingleInstanceOf(PROP_PREFIX + "Mapper.Class")
                .orElseThrow(() -> new MCRConfigurationException("Missing SitelinksPageMapper configuration"));
            this.pageSize = MCRConfiguration2.getString("Sitelinks.PageSize")
                .map(Integer::valueOf)
                .orElseThrow(() -> new MCRConfigurationException("Please specify property: 'Sitelinks.PageSize'"));
        } catch (MCRConfigurationException e) {
            throw new ServletException("Failed to initialize SitelinksServlet", e);
        }
    }

    @Override
    public MCRContent getContent(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            // GET /sitelinks -> list years
            return mapper.map(service.getRootPage());
        }
        if (pathInfo.endsWith("/")) {
            pathInfo = pathInfo.substring(0, pathInfo.length() - 1);
        }
        String[] pathParts = pathInfo.substring(1).split("/");

        return parseYear(pathParts[0])
            .flatMap(year -> {
                if (pathParts.length == 1) {
                    // GET /sitelinks/{year} -> list for year (page 1)
                    return getYearPage(year, 1);
                } else if (pathParts.length == 3 && PATH_PAGE.equals(pathParts[1])) {
                    // GET /sitelinks/{year}/page/{page} -> list for year and page
                    return parsePage(pathParts[2]).flatMap(page -> getYearPage(year, page));
                }
                return Optional.empty();
            }).orElseGet(() -> {
                LOGGER.error(() -> "Invalid or missing content for path" + request.getPathInfo());
                return null;
            });
    }

    private Optional<Integer> parseYear(String yearStr) {
        try {
            return Optional.of(Integer.parseInt(yearStr));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private Optional<Integer> parsePage(String pageStr) {
        try {
            return Optional.of(Integer.parseInt(pageStr));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private Optional<MCRContent> getYearPage(int year, int page) {
        SitelinksYearPageDto resultPage = service.getYearPage(year, page, pageSize);
        long totalPages = (resultPage.totalCount() + pageSize - 1) / pageSize;
        if (resultPage.totalCount() == 0 || page > totalPages) {
            return Optional.empty();
        }
        try {
            return Optional.of(mapper.map(resultPage));
        } catch (SitelinksMappingException e) {
            LOGGER.error("Mapping failed for year {} page {}", year, page, e);
            return Optional.empty();
        }
    }
}
