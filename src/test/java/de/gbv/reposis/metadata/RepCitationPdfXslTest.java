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

package de.gbv.reposis.metadata;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.regex.Pattern;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.junit.Assert;
import org.junit.Test;
import org.mycore.common.MCRTestCase;

public class RepCitationPdfXslTest extends MCRTestCase {

    private static final String XALAN = "org.apache.xalan.processor.TransformerFactoryImpl";

    private static final String BASE_URL = "https://example.org/servlets/MCRFileNodeServlet/";

    private static final String NEXT_STYLESHEET = "<xsl:stylesheet version=\"1.0\""
        + " xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"><xsl:template match=\"/\"><next />"
        + "</xsl:template></xsl:stylesheet>";

    private static final Pattern READABLE_DERIVATE = Pattern.compile("xlink:href=\"([^\"]+)\" readable=\"true\"");

    @Test
    public void testContentDerivateWithPdf() throws TransformerException {
        assertTag(BASE_URL + "a_derivate_00000001/article.pdf",
            transform(derivate("a_derivate_00000001", "content", "article.pdf", true)));
    }

    @Test
    public void testOtherDerivateTypesAreIgnored() throws TransformerException {
        assertTag(BASE_URL + "a_derivate_00000002/article.pdf",
            transform(derivate("a_derivate_00000001", "thumbnail", "cover.pdf", true)
                + derivate("a_derivate_00000002", "content", "article.pdf", true)));
    }

    @Test
    public void testOnlyFirstContentDerivateIsUsed() throws TransformerException {
        String result = transform(derivate("a_derivate_00000001", "content", "article.PDF", true)
            + derivate("a_derivate_00000002", "content", "appendix.pdf", true));
        assertTag(BASE_URL + "a_derivate_00000001/article.PDF", result);
        Assert.assertFalse(result.contains("appendix.pdf"));
    }

    @Test
    public void testFirstContentDerivateWithoutPdfIsNotSkipped() throws TransformerException {
        Assert.assertFalse(transform(derivate("a_derivate_00000001", "content", "index.xml", true)
            + derivate("a_derivate_00000002", "content", "article.pdf", true)).contains("citation_pdf_url"));
    }

    @Test
    public void testUnreadableDerivateIsSkipped() throws TransformerException {
        String result = transform(derivate("a_derivate_00000001", "content", "secret.pdf", false)
            + derivate("a_derivate_00000002", "content", "article.pdf", true));
        assertTag(BASE_URL + "a_derivate_00000002/article.pdf", result);
        Assert.assertFalse(result.contains("secret.pdf"));
    }

    @Test
    public void testPathIsEncoded() throws TransformerException {
        assertTag(BASE_URL + "a_derivate_00000001/sub%20dir/my%20article.pdf",
            transform(derivate("a_derivate_00000001", "content", "/sub dir/my article.pdf", true)));
    }

    @Test
    public void testMissingMaindocIsIgnored() throws TransformerException {
        Assert.assertFalse(transform(derivate("a_derivate_00000001", "content", "", true))
            .contains("citation_pdf_url"));
    }

    @Test
    public void testNextStylesheetIsApplied() throws TransformerException {
        Assert.assertTrue(transform("").contains("<next/>"));
    }

    private static void assertTag(String url, String result) {
        String expected = "<citation_meta><meta content=\"" + url + "\" name=\"citation_pdf_url\"/></citation_meta>";
        Assert.assertTrue("Missing " + expected + " in " + result, result.contains(expected));
    }

    private static String transform(String derivates) throws TransformerException {
        TransformerFactory factory = TransformerFactory.newInstance(XALAN, null);
        factory.setURIResolver((href, base) -> new StreamSource(new StringReader(NEXT_STYLESHEET)));
        Transformer transformer = factory.newTransformer(new StreamSource(
            RepCitationPdfXslTest.class.getClassLoader().getResource("xsl/metadata/rep-citation-pdf.xsl")
                .toExternalForm()));
        transformer.setParameter("ServletsBaseURL", "https://example.org/servlets/");
        StringWriter result = new StringWriter();
        transformer.transform(new StreamSource(new StringReader(object(derivates))), new StreamResult(result));
        return result.toString();
    }

    private static String derivate(String id, String type, String maindoc, boolean readable) {
        return "<derobject xlink:href=\"" + id + "\" readable=\"" + readable + "\"><maindoc>" + maindoc + "</maindoc>"
            + "<classification classid=\"derivate_types\" categid=\"" + type + "\" /></derobject>";
    }

    private static String object(String derivates) {
        StringBuilder rights = new StringBuilder();
        READABLE_DERIVATE.matcher(derivates).results()
            .forEach(match -> rights.append("<right id=\"").append(match.group(1)).append("\" read=\"\" />"));
        return "<mycoreobject xmlns:xlink=\"http://www.w3.org/1999/xlink\"><structure><derobjects>" + derivates
            + "</derobjects></structure><rights>" + rights + "</rights></mycoreobject>";
    }
}
