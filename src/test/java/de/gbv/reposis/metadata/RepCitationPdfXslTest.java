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
import java.util.Map;

import javax.xml.transform.Source;
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

    @Test
    public void testReadablePdf() throws TransformerException {
        String result = transform(Map.of("a_derivate_00000001", "article.pdf"), "a_derivate_00000001");
        assertContains(result, "<citation_meta><meta content=\"" + BASE_URL
            + "a_derivate_00000001/article.pdf\" name=\"citation_pdf_url\"/></citation_meta>");
    }

    @Test
    public void testAllReadablePdfsAreListed() throws TransformerException {
        String result = transform(Map.of("a_derivate_00000001", "article.pdf", "a_derivate_00000002", "appendix.PDF"),
            "a_derivate_00000001", "a_derivate_00000002");
        assertContains(result, BASE_URL + "a_derivate_00000001/article.pdf");
        assertContains(result, BASE_URL + "a_derivate_00000002/appendix.PDF");
    }

    @Test
    public void testPathIsEncoded() throws TransformerException {
        String result = transform(Map.of("a_derivate_00000001", "/sub dir/my article.pdf"), "a_derivate_00000001");
        assertContains(result, BASE_URL + "a_derivate_00000001/sub%20dir/my%20article.pdf");
    }

    @Test
    public void testOtherFileTypeIsIgnored() throws TransformerException {
        Assert.assertFalse(transform(Map.of("a_derivate_00000001", "image.tif"), "a_derivate_00000001")
            .contains("citation_pdf_url"));
    }

    @Test
    public void testMissingMaindocIsIgnored() throws TransformerException {
        Assert.assertFalse(transform(Map.of("a_derivate_00000001", ""), "a_derivate_00000001")
            .contains("citation_pdf_url"));
    }

    @Test
    public void testUnreadableDerivateIsIgnored() throws TransformerException {
        Assert.assertFalse(transform(Map.of("a_derivate_00000001", "article.pdf")).contains("citation_pdf_url"));
    }

    @Test
    public void testNextStylesheetIsApplied() throws TransformerException {
        assertContains(transform(Map.of("a_derivate_00000001", "article.pdf"), "a_derivate_00000001"), "<next/>");
    }

    private static void assertContains(String result, String expected) {
        Assert.assertTrue("Missing " + expected + " in " + result, result.contains(expected));
    }

    private static String transform(Map<String, String> maindocs, String... readableDerivates)
        throws TransformerException {
        TransformerFactory factory = TransformerFactory.newInstance(XALAN, null);
        factory.setURIResolver((href, base) -> new StreamSource(new StringReader(NEXT_STYLESHEET)));
        Source stylesheet = new StreamSource(
            RepCitationPdfXslTest.class.getClassLoader().getResource("xsl/metadata/rep-citation-pdf.xsl")
                .toExternalForm());
        Transformer transformer = factory.newTransformer(stylesheet);
        transformer.setParameter("ServletsBaseURL", "https://example.org/servlets/");
        transformer.setURIResolver((href, base) -> new StreamSource(
            new StringReader(derivate(maindocs.get(href.substring("mcrobject:".length()))))));
        StringWriter result = new StringWriter();
        transformer.transform(new StreamSource(new StringReader(object(maindocs, readableDerivates))),
            new StreamResult(result));
        return result.toString();
    }

    private static String derivate(String maindoc) {
        return "<mycorederivate><derivate><internals><internal maindoc=\"" + maindoc
            + "\" /></internals></derivate></mycorederivate>";
    }

    private static String object(Map<String, String> maindocs, String... readableDerivates) {
        StringBuilder derivates = new StringBuilder();
        maindocs.keySet().forEach(id -> derivates.append("<derobject xlink:href=\"").append(id).append("\" />"));
        StringBuilder rights = new StringBuilder();
        for (String id : readableDerivates) {
            rights.append("<right id=\"").append(id).append("\" read=\"\" />");
        }
        return "<mycoreobject xmlns:xlink=\"http://www.w3.org/1999/xlink\"><structure><derobjects>" + derivates
            + "</derobjects></structure><rights>" + rights + "</rights></mycoreobject>";
    }
}
