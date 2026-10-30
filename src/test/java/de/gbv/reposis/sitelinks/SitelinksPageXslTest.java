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

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.junit.Assert;
import org.junit.Test;

public class SitelinksPageXslTest {

    private static final String YEAR_URL = "https://example.org/sitelinks/2026";

    @Test
    public void testFirstPage() throws TransformerException {
        String html = transformPage(1, 250);
        assertContains(html, "<link rel=\"canonical\" href=\"" + YEAR_URL + "\"");
        assertContains(html, "<link rel=\"next\" href=\"" + YEAR_URL + "/page/2\"");
        Assert.assertFalse(html.contains("rel=\"prev\""));
        assertContains(html, "<span>1</span>");
        assertPageLink(html, YEAR_URL + "/page/3", 3);
    }

    @Test
    public void testSecondPageLinksYearPageAsFirstPage() throws TransformerException {
        String html = transformPage(2, 250);
        assertContains(html, "<link rel=\"canonical\" href=\"" + YEAR_URL + "/page/2\"");
        assertContains(html, "<link rel=\"prev\" href=\"" + YEAR_URL + "\"");
        assertContains(html, "<a href=\"" + YEAR_URL + "\">All Pages of 2026</a>");
        Assert.assertFalse(html.contains("/page/1\""));
    }

    @Test
    public void testLastPage() throws TransformerException {
        String html = transformPage(3, 250);
        assertContains(html, "<link rel=\"canonical\" href=\"" + YEAR_URL + "/page/3\"");
        Assert.assertFalse(html.contains("rel=\"next\""));
        assertContains(html, "<span>Page 3</span>");
    }

    @Test
    public void testSinglePage() throws TransformerException {
        String html = transformPage(1, 50);
        assertContains(html, "<link rel=\"canonical\" href=\"" + YEAR_URL + "\"");
        assertContains(html, "<span>1</span>");
        Assert.assertFalse(html.contains("rel=\"prev\""));
        Assert.assertFalse(html.contains("rel=\"next\""));
        Assert.assertFalse(html.contains("/page/"));
    }

    @Test
    public void testTotalCountIsMultipleOfPageSize() throws TransformerException {
        String html = transformPage(2, 200);
        assertContains(html, "<span>Page 2</span>");
        Assert.assertFalse(html.contains("rel=\"next\""));
        Assert.assertFalse(html.contains("/page/3\""));
        Assert.assertFalse(transformPage(1, 200).contains("/page/3\""));
    }

    @Test
    public void testAllPagesAreLinked() throws TransformerException {
        String html = transformPage(1, 5000);
        for (int page = 2; page <= 50; page++) {
            assertPageLink(html, YEAR_URL + "/page/" + page, page);
        }
        Assert.assertFalse(html.contains("/page/51\""));
    }

    @Test
    public void testOnlyYearPageListsAllPages() throws TransformerException {
        String html = transformPage(25, 5000);
        assertContains(html, "<link rel=\"next\" href=\"" + YEAR_URL + "/page/26\"");
        Assert.assertFalse(html.contains("/page/2\""));
        Assert.assertFalse(html.contains("/page/50\""));
    }

    @Test
    public void testManyPagesAreLinkedInOrder() throws TransformerException {
        String html = transformPage(1, 1_000_000);
        assertPageLink(html, YEAR_URL + "/page/10000", 10_000);
        Assert.assertTrue(html.indexOf(">1999</a>") < html.indexOf(">2000</a>"));
    }

    @Test
    public void testObjectLinkUsesTitle() throws TransformerException {
        assertContains(transformPage("<object-id title=\"A &lt; B\">a_mods_00000001</object-id>"),
            "<a href=\"https://example.org/receive/a_mods_00000001\">A &lt; B</a>");
    }

    @Test
    public void testObjectLinkFallsBackToId() throws TransformerException {
        assertContains(transformPage("<object-id>a_mods_00000001</object-id>"),
            "<a href=\"https://example.org/receive/a_mods_00000001\">a_mods_00000001</a>");
    }

    private static void assertPageLink(String html, String url, int page) {
        assertContains(html, "<a href=\"" + url + "\">" + page + "</a>");
    }

    private static void assertContains(String html, String expected) {
        Assert.assertTrue("Missing " + expected + " in " + html, html.contains(expected));
    }

    private static String transformPage(int number, int totalCount) throws TransformerException {
        return transformPage(number, totalCount, "<object-id>a_mods_00000001</object-id>");
    }

    private static String transformPage(String objectId) throws TransformerException {
        return transformPage(1, 1, objectId);
    }

    private static String transformPage(int number, int totalCount, String objectId) throws TransformerException {
        String xml = "<sitelinks-page><page number=\"" + number + "\" total-count=\"" + totalCount
            + "\" year=\"2026\"><object-ids>" + objectId + "</object-ids></page></sitelinks-page>";
        StreamSource stylesheet = new StreamSource(
            SitelinksPageXslTest.class.getClassLoader().getResource("xsl/sitelinks-page.xsl").toExternalForm());
        Transformer transformer = TransformerFactory.newInstance().newTransformer(stylesheet);
        transformer.setParameter("WebApplicationBaseURL", "https://example.org/");
        transformer.setParameter("Sitelinks.PageSize", 100);
        StringWriter html = new StringWriter();
        transformer.transform(new StreamSource(new StringReader(xml)), new StreamResult(html));
        return html.toString();
    }
}
