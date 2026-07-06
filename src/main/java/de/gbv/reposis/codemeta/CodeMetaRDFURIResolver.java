package de.gbv.reposis.codemeta;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.apache.commons.io.IOUtils;
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

import com.github.jsonldjava.core.DocumentLoader;

/**
 * {@link URIResolver} that converts CodeMeta JSON-LD content into RDF/XML.
 */
public class CodeMetaRDFURIResolver implements URIResolver {

    private static final String CODEMETA_JSONLD_PATH = "/jsonld/codemeta.jsonld";

    private static final String CODEMETA_JSONLD_URL = "https://doi.org/10.5063/schema/codemeta-2.0";

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
        String json = URLDecoder.decode(hrefParts[2], StandardCharsets.UTF_8);
        try {
            Document result = convertToRDF(json, baseURI);
            return new JDOMSource(result);
        } catch (Exception e) {
            throw new TransformerException("Unable to convert to rdf", e);
        }
    }

    // TODO: JSONLDSettings.DOCUMENT_LOADER has been deprecated since rdf4j 4.3.0,
    // but its replacement (org.eclipse.rdf4j.rio.jsonld.JSONLDSettings) only
    // exists starting from rdf4j 5.2.0. Update the import once upgraded to >=5.2.0.
    @SuppressWarnings("deprecation")
    private Document convertToRDF(String json, String baseURI) throws Exception {
        try (InputStream input = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
            ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            String codemetaDoc = IOUtils.resourceToString(CODEMETA_JSONLD_PATH, StandardCharsets.UTF_8);
            DocumentLoader docLoader = new DocumentLoader();
            docLoader.addInjectedDoc(CODEMETA_JSONLD_URL, codemetaDoc);

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
