package de.gbv.reposis.codemeta;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.JSONLDSettings;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.eclipse.rdf4j.rio.jsonld.JSONLDParser;
import org.eclipse.rdf4j.rio.rdfxml.RDFXMLWriter;
import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.jdom2.transform.JDOMSource;
import org.mycore.common.MCRClassTools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jsonldjava.core.DocumentLoader;

/**
 * {@link URIResolver} that converts CodeMeta JSON-LD content into RDF/XML.
 */
public class CodeMetaRDFURIResolver implements URIResolver {

    private static final String JSONLD_PATH = "jsonld/codemeta.jsonld";

    private static final String JSONLD_URL = "https://doi.org/10.5063/schema/codemeta-2.0";

    private static final String JSONLD_DOC;

    static {
        try (InputStream is = MCRClassTools.getClassLoader().getResourceAsStream(JSONLD_PATH)) {
            JSONLD_DOC = new String(Objects.requireNonNull(is).readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    /**
     * Converts CodeMeta JSON-LD content to RDF/XML.
     * <p>URI Syntax:
     * <pre>
     *   &lt;scheme&gt;:&lt;baseURI&gt;:&lt;json&gt;
     * </pre>
     * <p>Example request:
     * <pre>
     *   codemeta2rdf::{"@context": "https://doi.org/10.5063/schema/codemeta-2.0", "name": "Example"}
     * </pre>
     * <p>Example response:
     * <pre>{@code
     *   <rdf:RDF>
     *     ...
     *   </rdf:RDF>
     * }</pre>
     *
     * @param href the URI in the syntax above, consisting of the scheme, an optional base URI
     *             used to resolve relative references in the JSON-LD document, and the
     *             CodeMeta JSON-LD content itself
     * @param base the base URI of the calling stylesheet (unused)
     * @return a {@link JDOMSource} wrapping the converted RDF/XML document
     * @throws TransformerException if the given URI does not contain the expected parts,
     *                              or if the JSON-LD content could not be converted to RDF
     */
    @Override
    public Source resolve(final String href, final String base) throws TransformerException {
        String[] hrefParts = href.split(":", 3);
        if (hrefParts.length != 3) {
            throw new TransformerException("Invalid format of uri for retrieval of json2rdf: " + href);
        }
        String baseURI = hrefParts[1];
        String json = URLDecoder.decode(hrefParts[2].replace("+", "%2B"), StandardCharsets.UTF_8);
        try {
            requireExpectedContext(json);
            Document result = convertToRDF(json, baseURI);
            return new JDOMSource(result);
        } catch (Exception e) {
            throw new TransformerException("Unable to convert to rdf", e);
        }
    }

    private void requireExpectedContext(String json) throws TransformerException, IOException {
        JsonNode root = JSON_MAPPER.readTree(json);
        JsonNode context = root.get("@context");
        if (context == null || !context.isTextual() || !JSONLD_URL.equals(context.asText())) {
            throw new TransformerException(
                "Unexpected or missing @context, refusing to process (must be exactly '" + JSONLD_URL + "')");
        }
    }

    // TODO: JSONLDSettings.DOCUMENT_LOADER has been deprecated since rdf4j 4.3.0,
    // but its replacement (org.eclipse.rdf4j.rio.jsonld.JSONLDSettings) only
    // exists starting from rdf4j 5.2.0. Update the import once upgraded to >=5.2.0.
    @SuppressWarnings("deprecation")
    private Document convertToRDF(String json, String baseURI) throws Exception {
        try (InputStream input = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
            ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            DocumentLoader docLoader = new DocumentLoader();
            docLoader.addInjectedDoc(JSONLD_URL, JSONLD_DOC);

            JSONLDParser parser = new JSONLDParser();
            parser.getParserConfig().set(JSONLDSettings.DOCUMENT_LOADER, docLoader);
            Model model = new LinkedHashModel();
            parser.setRDFHandler(new StatementCollector(model));
            parser.parse(input, baseURI.isEmpty() ? null : baseURI);

            Rio.write(model, new RDFXMLWriter(out));
            return new SAXBuilder().build(new ByteArrayInputStream(out.toByteArray()));
        }
    }
}
