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

package de.gbv.reposis.sitelinks.resources;

import java.io.InputStream;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.frontend.jersey.MCRJerseyUtil;

import de.gbv.reposis.sitelinks.ObjectMetadataService;

/**
 * REST resource for managing "Sitelinks" and their associated data.
 * This class provides endpoints to display years, months, and publications
 * based on the provided parameters.
 */
@Path("sitelinks")
public class SitelinksResource {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String PATH_PARAM_YEAR = "year";
    private static final String PATH_PARAM_MONTH = "month";
    private static final String PATH_PARAM_PAGE = "page";

    private static final String BASIC_FILTER_QUERY =
        MCRConfiguration2.getStringOrThrow("SitelinksResource.BasicFilterQuery");

    private static final int PAGE_SIZE =
        MCRConfiguration2.getString("Sitelinks.PageSize").map(Integer::valueOf).orElseThrow(
            () -> new MCRConfigurationException("Please specify property: 'Sitelinks.PageSize'"));

    private final ObjectMetadataService objectMetadataService;
    private final int pageSize;

    @Context
    private HttpServletRequest req;

    /**
     * Constructor for {@code SitelinksResource}.
     * Initializes the resource with an instance of {@link ObjectMetadataService}
     * and the page size from the configuration.
     */
    public SitelinksResource() {
        this(new ObjectMetadataService(BASIC_FILTER_QUERY), PAGE_SIZE);
    }

    /**
     * Constructor for {@code SitelinksResource} that allows passing
     * the {@link ObjectMetadataService} and page size.
     *
     * @param objectMetadataService The service to manage the object metadata
     * @param pageSize The maximum number of items per page
     */
    public SitelinksResource(ObjectMetadataService objectMetadataService, int pageSize) {
        this.objectMetadataService = objectMetadataService;
        this.pageSize = pageSize;
    }

    /**
     * Returns a list of years that contains objects (descending order).
     *
     * @return An HTML response containing a list of years
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response listYears() {
        return generateResponse(this::buildYearsElement);
    }

    /**
     * Returns a list of months for the given year (descending order).
     *
     * @param year The year for which to list the months
     * @return An HTML response containing a list of months for the specified year
     */
    @GET
    @Path("/{" + PATH_PARAM_YEAR + "}")
    @Produces(MediaType.TEXT_HTML)
    public Response listMonthsForYear(@PathParam(PATH_PARAM_YEAR) int year) {
        return generateResponse(() -> buildMonthsElement(year));
    }

    /**
     * Returns a list of publications for a specific month of the given year (descending order).
     * The default page (page 1) will be shown.
     *
     * @param year The year for which to list the publications
     * @param monthStr The month as a string for which to list the publications
     * @return An HTML response containing a list of publications for the specified month and year
     */
    @GET
    @Path("/{" + PATH_PARAM_YEAR + "}/{" + PATH_PARAM_MONTH + "}")
    @Produces(MediaType.TEXT_HTML)
    public Response listPublicationsForMonthPage(@PathParam(PATH_PARAM_YEAR) int year,
        @PathParam(PATH_PARAM_MONTH) String monthStr) {
        return listPublicationsForMonthPage(year, monthStr, 1);
    }

    /**
     * Returns a list of publications for a specific month and year for the given page (descending order).
     *
     * @param year The year for which to list the publications
     * @param monthStr The month as a string for which to list the publications
     * @param page The page number of the publications
     * @return An HTML response containing a list of publications for the specified page
     * @throws WebApplicationException if the page number is less than 1
     */
    @GET
    @Path("/{" + PATH_PARAM_YEAR + "}/{" + PATH_PARAM_MONTH + "}/page/{" + PATH_PARAM_PAGE + "}")
    @Produces(MediaType.TEXT_HTML)
    public Response listPublicationsForMonthPage(@PathParam(PATH_PARAM_YEAR) int year,
        @PathParam(PATH_PARAM_MONTH) String monthStr, @PathParam(PATH_PARAM_PAGE) int page) {
        if (page < 1) {
            throw new WebApplicationException("Page number must be >= 1", Response.Status.BAD_REQUEST);
        }
        final int month = parseMonth(monthStr);
        return generateResponse(() -> buildPageElement(year, month, page));
    }

    private int parseMonth(String monthStr) {
        try {
            final int month = Integer.parseInt(monthStr);
            if (month < 1 || month > 12) {
                throw new IllegalArgumentException();
            }
            return month;
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException("Month must be between 01 and 12", Response.Status.BAD_REQUEST);
        }
    }

    private Response generateResponse(ElementBuilder elementBuilder) {
        final Element root = new Element("sitelinks");
        final Element contentElement = elementBuilder.build();
        root.addContent(contentElement);
        try (InputStream transformedStream = MCRJerseyUtil.transform(new Document(root), req).getInputStream()) {
            return Response.ok(transformedStream).build();
        } catch (Exception e) {
            LOGGER.error("Error while transforming document", e);
            throw new WebApplicationException("Internal server error during XML transformation", e);
        }
    }

    private Element buildYearsElement() {
        final Element yearsElement = new Element("years");
        for (int year : objectMetadataService.getYearsWithObjects()) {
            yearsElement.addContent(createElement("year", String.valueOf(year)));
        }
        return yearsElement;
    }

    private Element buildMonthsElement(int year) {
        final Element monthsElement = new Element("months");
        monthsElement.setAttribute("year", String.valueOf(year));
        for (int month : objectMetadataService.getMonthsWithObjects(year)) {
            monthsElement.addContent(createElement("month", String.valueOf(month)));
        }
        return monthsElement;
    }

    private Element buildPageElement(int year, int month, int page) {
        final int offset = (page - 1) * pageSize;
        final ObjectMetadataService.ObjectIdsWithCount objectIdsWithCount =
            objectMetadataService.getObjectIdsByDate(year, month, offset, pageSize);

        final Element pageElement = new Element("page");
        pageElement.setAttribute("number", String.valueOf(page));
        pageElement.setAttribute("totalCount", String.valueOf(objectIdsWithCount.totalCount()));
        pageElement.setAttribute("month", String.valueOf(month));
        pageElement.setAttribute("year", String.valueOf(year));

        final Element objectIdsElement = new Element("objectIds");
        for (String objectId : objectIdsWithCount.objectIds()) {
            objectIdsElement.addContent(createElement("objectId", objectId));
        }
        pageElement.addContent(objectIdsElement);
        return pageElement;
    }

    private Element createElement(String name, String text) {
        final Element element = new Element(name);
        element.setText(text);
        return element;
    }

    private interface ElementBuilder {
        Element build();
    }

}
