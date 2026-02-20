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
import java.util.Comparator;
import java.util.List;
import org.jdom2.Element;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.content.transformer.MCRContentTransformer;
import org.mycore.common.xml.MCRLayoutTransformerFactory;

/**
 * Default implementation of {@link SitelinksContentBuilder} that generates HTML/XML content pages.
 */
public class SitelinksContentBuilderImpl implements SitelinksContentBuilder {

    private static final String ROOT = "sitelinks-page";
    private static final String YEARS = "years";
    private static final String YEAR = "year";
    private static final String PAGE = "page";
    private static final String OBJECT_IDS = "object-ids";
    private static final String OBJECT_ID = "object-id";
    private static final String ATTR_NUMBER = "number";
    private static final String ATTR_TOTAL_COUNT = "total-count";
    private static final String ATTR_YEAR = "year";

    private final MCRContentTransformer transformer;

    /**
     * Constructs a new {@code SitelinksContentBuilder} with a transformer
     * for the root element "sitelinks-page".
     */
    public SitelinksContentBuilderImpl() {
        this.transformer = new MCRLayoutTransformerFactory().getTransformer(ROOT);
    }

    @Override
    public MCRContent createRootPage(List<Integer> years) throws IOException {
        Element root = new Element(ROOT);
        root.addContent(buildYearsElement(years));
        return transformer.transform(new MCRJDOMContent(root));
    }

    @Override
    public MCRContent createPage(int year, int page, long totalCount, List<String> objectIds) throws IOException {
        Element root = new Element(ROOT);
        root.addContent(buildPageElement(year, page, totalCount, objectIds));
        return transformer.transform(new MCRJDOMContent(root));
    }

    private static Element buildYearsElement(List<Integer> years) {
        Element yearsElement = new Element(YEARS);
        years.stream()
            .sorted(Comparator.reverseOrder())
            .forEach(y -> yearsElement.addContent(createElement(YEAR, String.valueOf(y))));
        return yearsElement;
    }

    private static Element buildPageElement(int year, int page, long totalCount, List<String> objectIds) {
        Element pageElement = new Element(PAGE);
        pageElement.setAttribute(ATTR_NUMBER, String.valueOf(page));
        pageElement.setAttribute(ATTR_TOTAL_COUNT, String.valueOf(totalCount));
        pageElement.setAttribute(ATTR_YEAR, String.valueOf(year));

        Element objectIdsElement = new Element(OBJECT_IDS);
        objectIds.forEach(id -> objectIdsElement.addContent(createElement(OBJECT_ID, id)));
        pageElement.addContent(objectIdsElement);

        return pageElement;
    }

    private static Element createElement(String name, String text) {
        Element element = new Element(name);
        element.setText(text);
        return element;
    }
}
