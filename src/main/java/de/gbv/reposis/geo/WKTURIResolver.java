package de.gbv.reposis.geo;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import javax.xml.transform.Source;
import javax.xml.transform.URIResolver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.xsl.uriresolver.MCRURIResolverResponse;

/**
 * {@link URIResolver} that normalizes geographic coordinates.
 */
public class WKTURIResolver implements URIResolver {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Normalizes geographic coordinates and returns the resulting WKT geometry as a string.
     * <p>URI Syntax:
     * <pre>
     *   &lt;scheme&gt;:{coordinates}
     * </pre>
     * <p>
     * The coordinates are expected to be URI encoded and separated by commas.
     * Each coordinate value is decoded before being passed to the WKT normalization
     * logic.
     * </p>
     * <p>Example request:
     * <pre>
     *   normalizeWKT:E%2072%C2%B0--E%20148%C2%B0%2FN%2013%C2%B0--N%2018%C2%B0
     * </pre>
     * <p>Example response:
     * <pre>{@code
     * POLYGON((72 13,148 13,148 18,72 18,72 13))
     * }</pre>
     *
     * @param href the URI to resolve
     * @param base the base URI of the calling stylesheet (unused)
     * @return a {@link Source} wrapping the result element
     */
    @Override
    public Source resolve(String href, String base) {
        String key = href.substring(href.indexOf(':') + 1);
        LOGGER.debug("Building wkt string from {}", key);

        if (key.isBlank()) {
            return MCRURIResolverResponse.ofString("");
        }

        List<String> coords =
            Arrays.stream(key.split(",")).map(s -> URLDecoder.decode(s, StandardCharsets.UTF_8)).toList();
        String result = GeoFunctions.getNormalizedWKTString(coords);
        return MCRURIResolverResponse.ofString(result);
    }
}
