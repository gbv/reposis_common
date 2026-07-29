package de.gbv.reposis.geo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.xml.transform.Source;

import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;
import org.junit.jupiter.api.Test;

public class WKTURIResolverTest {

    private final WKTURIResolver resolver = new WKTURIResolver();

    @Test
    public void shouldSplitAtUnencodedCommasOnly() {
        // polygon "0 0, 1 0, 1 1, 0 1" and point "10 20", encoded like encode-for-uri in repgeoutils.xsl
        String href = "normalizeWKT:0%200%2C%201%200%2C%201%201%2C%200%201,10%2020";
        assertEquals("GeometryCollection (POLYGON ((0.0 0.0, 1.0 0.0, 1.0 1.0, 0.0 1.0)), POINT (10.0 20.0))",
            resolve(href));
    }

    @Test
    public void shouldReturnEmptyStringWithoutCoordinates() {
        assertEquals("", resolve("normalizeWKT:"));
    }

    @Test
    public void shouldIgnoreEmptyCoordinates() {
        assertEquals("", resolve("normalizeWKT:,"));
        assertEquals("GeometryCollection (POINT (10.0 20.0))", resolve("normalizeWKT:,10%2020,"));
    }

    private String resolve(String href) {
        Source source = resolver.resolve(href, null);
        Element result = (Element) ((JDOMSource) source).getNodes().get(0);
        assertEquals("string", result.getName());
        return result.getText();
    }
}
