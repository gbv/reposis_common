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

package de.gbv.reposis.mods;

import java.io.IOException;
import java.io.StringReader;

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.filter.ElementFilter;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.Assert;
import org.junit.Test;

public class MCRMODSNamespaceNormalizerTest {

    private static final String MODS_URI = "http://www.loc.gov/mods/v3";

    private static final String XLINK_URI = "http://www.w3.org/1999/xlink";

    @Test
    public void declaresTheDefaultNamespaceOfANameElementAtTheModsElement() throws JDOMException, IOException {
        Element mods = parse("""
            <mods:mods xmlns:mods="http://www.loc.gov/mods/v3">
              <name xmlns="http://www.loc.gov/mods/v3" type="personal">
                <mods:displayForm>Allen, Caitilyn</mods:displayForm>
                <namePart type="family">Allen</namePart>
              </name>
            </mods:mods>""");

        Assert.assertTrue(MCRMODSNamespaceNormalizer.normalize(mods));

        Assert.assertTrue(declares(mods, "", MODS_URI));
        Element name = mods.getChildren().get(0);
        Assert.assertTrue(name.getAdditionalNamespaces().isEmpty());
        Assert.assertEquals(MODS_URI, name.getNamespaceURI());
        Assert.assertEquals(MODS_URI, name.getChildren().get(1).getNamespaceURI());
        Assert.assertTrue(toXML(mods).contains("<name type=\"personal\">"));
    }

    @Test
    public void declaresTheNamespaceOfAnAttributeAtTheModsElement() throws JDOMException, IOException {
        Element mods = parse("""
            <mods:mods xmlns:mods="http://www.loc.gov/mods/v3">
              <mods:name xmlns:xlink="http://www.w3.org/1999/xlink" xlink:type="simple" />
            </mods:mods>""");

        Assert.assertTrue(MCRMODSNamespaceNormalizer.normalize(mods));

        Assert.assertTrue(declares(mods, "xlink", XLINK_URI));
        Assert.assertTrue(mods.getChildren().get(0).getAdditionalNamespaces().isEmpty());
    }

    @Test
    public void removesTheUnusedDeclarationsOfFormerTransformations() throws JDOMException, IOException {
        Element mods = parse("""
            <mods:mods xmlns:mods="http://www.loc.gov/mods/v3">
              <mods:extension xmlns:math="http://exslt.org/math"
                              xmlns:mcrxml="xalan://org.mycore.common.xml.MCRXMLFunctions" type="metrics">
                <value year="2002">0.532</value>
              </mods:extension>
            </mods:mods>""");

        Assert.assertTrue(MCRMODSNamespaceNormalizer.normalize(mods));

        Assert.assertTrue(mods.getAdditionalNamespaces().isEmpty());
        Assert.assertTrue(mods.getChildren().get(0).getAdditionalNamespaces().isEmpty());
        Assert.assertFalse(toXML(mods).contains("xalan://"));
        Assert.assertEquals("0.532", mods.getChildren().get(0).getChildren().get(0).getText());
    }

    @Test
    public void keepsAUsedDeclarationOfAConflictingPrefix() throws JDOMException, IOException {
        Element mods = parse("""
            <mods:mods xmlns:mods="http://www.loc.gov/mods/v3">
              <mods:extension xmlns:foo="http://example.org/one">
                <foo:one />
                <mods:note xmlns:foo="http://example.org/two">
                  <foo:two />
                </mods:note>
              </mods:extension>
            </mods:mods>""");

        Assert.assertTrue(MCRMODSNamespaceNormalizer.normalize(mods));

        Assert.assertTrue(declares(mods, "foo", "http://example.org/one"));
        Element extension = mods.getChildren().get(0);
        Assert.assertTrue(extension.getAdditionalNamespaces().isEmpty());
        Element note = extension.getChildren().get(1);
        Assert.assertTrue(declares(note, "foo", "http://example.org/two"));
    }

    @Test
    public void keepsNamespacesThatAreInheritedFromTheMycoreobject() throws JDOMException, IOException {
        Element mycoreobject = parse("""
            <mycoreobject xmlns:xlink="http://www.w3.org/1999/xlink" ID="openagrar_mods_00000001">
              <metadata>
                <def.modsContainer>
                  <modsContainer>
                    <mods:mods xmlns:mods="http://www.loc.gov/mods/v3">
                      <mods:name xlink:type="simple" />
                    </mods:mods>
                  </modsContainer>
                </def.modsContainer>
              </metadata>
            </mycoreobject>""");
        Element mods = mycoreobject.getDescendants(new ElementFilter("mods", Namespace.getNamespace(MODS_URI)))
            .iterator().next();

        Assert.assertFalse(MCRMODSNamespaceNormalizer.normalize(mods));
        Assert.assertTrue(mods.getAdditionalNamespaces().isEmpty());
    }

    @Test
    public void reportsNoChangeForAnAlreadyNormalizedDocument() throws JDOMException, IOException {
        Element mods = parse("""
            <mods:mods xmlns:mods="http://www.loc.gov/mods/v3" xmlns="http://www.loc.gov/mods/v3">
              <name type="personal">
                <namePart type="family">Allen</namePart>
              </name>
            </mods:mods>""");
        String before = toXML(mods);

        Assert.assertFalse(MCRMODSNamespaceNormalizer.normalize(mods));
        Assert.assertEquals(before, toXML(mods));
    }

    @Test
    public void isIdempotent() throws JDOMException, IOException {
        Element mods = parse("""
            <mods:mods xmlns:mods="http://www.loc.gov/mods/v3">
              <name xmlns="http://www.loc.gov/mods/v3">
                <namePart type="family">Allen</namePart>
              </name>
            </mods:mods>""");

        Assert.assertTrue(MCRMODSNamespaceNormalizer.normalize(mods));
        String normalized = toXML(mods);
        Assert.assertFalse(MCRMODSNamespaceNormalizer.normalize(mods));
        Assert.assertEquals(normalized, toXML(mods));
    }

    private static boolean declares(Element element, String prefix, String uri) {
        return element.getAdditionalNamespaces().stream()
            .anyMatch(namespace -> namespace.getPrefix().equals(prefix) && namespace.getURI().equals(uri));
    }

    private static Element parse(String xml) throws JDOMException, IOException {
        return new SAXBuilder().build(new StringReader(xml)).getRootElement();
    }

    private static String toXML(Element element) {
        return new XMLOutputter(Format.getRawFormat()).outputString(element);
    }
}
