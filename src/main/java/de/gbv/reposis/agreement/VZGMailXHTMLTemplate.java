/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.gbv.reposis.agreement;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.Parent;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.common.MCRClassTools;

public class VZGMailXHTMLTemplate {

    private static final XPathFactory XPATH_FACTORY = XPathFactory.instance();

    public static Namespace PLACE_HOLDER_NAMESPACE = Namespace.getNamespace("plc", "https://gbv.de/mail-placeholder");

    public static Namespace XHTML_NAMESPACE = Namespace.getNamespace("xhtml", "http://www.w3.org/1999/xhtml");

    final Document doc;

    public VZGMailXHTMLTemplate(InputStream is) throws IOException, JDOMException {
        doc = new SAXBuilder().build(is);
    }

    public VZGMailXHTMLTemplate(String resourceName) throws IOException, JDOMException {
        this(MCRClassTools.getClassLoader().getResourceAsStream(resourceName));
    }

    public void replace(String placeHolderName, Content content) {
        final XPathExpression<Content> xpath = XPATH_FACTORY.compile(".//plc:" + placeHolderName,
            Filters.content(),
            Collections.emptyMap(),
                PLACE_HOLDER_NAMESPACE, XHTML_NAMESPACE);

        final List<Content> contentsToReplace = new ArrayList<>(xpath.evaluate(doc.getRootElement()));

        for (Content toReplace : contentsToReplace) {
            final Parent parent = toReplace.getParent();
            final int i = parent.indexOf(toReplace);
            parent.addContent(i, content);
            parent.removeContent(toReplace);
        }
    }

    public Document getDoc() {
        return doc;
    }
}
