package de.gbv.reposis.metrics;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathFactory;
import org.junit.Assert;
import org.junit.Test;
import org.mycore.common.MCRClassTools;
import org.mycore.common.MCRTestCase;
import org.mycore.crypt.MCRCryptKeyNoPermissionException;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.mods.MCRMODSWrapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Objects;

public class MCRMODSJournalMetricsHelperTest extends MCRTestCase {

    private static final Double TEST_SNIP_VALUE_2019 = 0.213;
    private static final Double TEST_SNIP_VALUE_2018 = 1.205;
    private static final Double TEST_SNIP_VALUE_2017 = 1.349;
    private static final Double TEST_SNIP_VALUE_2016 = 1.368;

    private static final Double TEST_SJR_VALUE_2019 = 0.321;
    private static final Double TEST_SJR_VALUE_2018 = 1.023;
    private static final Double TEST_SJR_VALUE_2017 = 0.99;
    private static final Double TEST_SJR_VALUE_2016 = 0.852;

    private static final Double TEST_JCR_VALUE_2019 = 0.123;
    private static final Double TEST_JCR_VALUE_2018 = 1.234;
    private static final Double TEST_JCR_VALUE_2017 = 2.421;
    private static final Double TEST_JCR_VALUE_2016 = 3.412;

    private static final String TEST_ENCRYPTED_JCR_VALUE_2019 = "YzXAmi//E79KzaNWfnEHAQ==";

    private static final String TEST_ENCRYPTED_JCR_VALUE_2018 = "O4VvdIpRkmn6wPSC4ckGCA==";

    @Test
    public void getMetrics() throws IOException, JDOMException, MCRCryptKeyNoPermissionException {
        try (InputStream is = MCRClassTools.getClassLoader().getResourceAsStream("metrics-test-document.xml")) {
            Document doc = new SAXBuilder().build(is);
            MCRObject mcrObject = new MCRObject(doc);
            MCRMODSWrapper wrapper = new MCRMODSWrapper(mcrObject);
            MCRJournalMetrics metrics = MCRMODSJournalMetricsHelper.getMetrics(wrapper);

            Assert.assertNotNull(metrics);

            Map<Integer, Double> snip = metrics.getSnip();
            Assert.assertEquals(TEST_SNIP_VALUE_2018, snip.get(2018));
            Assert.assertEquals(TEST_SNIP_VALUE_2017, snip.get(2017));
            Assert.assertEquals(TEST_SNIP_VALUE_2016, snip.get(2016));

            Map<Integer, Double> sjr = metrics.getSJR();
            Assert.assertEquals(TEST_SJR_VALUE_2018, sjr.get(2018));
            Assert.assertEquals(TEST_SJR_VALUE_2017, sjr.get(2017));
            Assert.assertEquals(TEST_SJR_VALUE_2016, sjr.get(2016));

            Map<Integer, Double> jcr = metrics.getJCR();

            Assert.assertEquals(TEST_JCR_VALUE_2016, jcr.get(2016));
            Assert.assertEquals(TEST_JCR_VALUE_2018, jcr.get(2018));
            Assert.assertEquals(TEST_JCR_VALUE_2017, jcr.get(2017));
        }
    }

    @Test
    public void setMetrics() throws IOException, JDOMException, MCRCryptKeyNoPermissionException {
        try (InputStream is = MCRClassTools.getClassLoader().getResourceAsStream("metrics-test-document.xml")) {
            Document doc = new SAXBuilder().build(is);
            MCRObject mcrObject = new MCRObject(doc);
            MCRMODSWrapper wrapper = new MCRMODSWrapper(mcrObject);
            MCRJournalMetrics metrics = MCRMODSJournalMetricsHelper.getMetrics(wrapper);

            metrics.getSJR().put(2019, TEST_SJR_VALUE_2019);
            metrics.getSnip().put(2019, TEST_SNIP_VALUE_2019);
            metrics.getJCR().put(2019, TEST_JCR_VALUE_2019);

            MCRMODSJournalMetricsHelper.setMetrics(wrapper, metrics);

            Element mods = wrapper.getMODS();
            Assert.assertEquals(TEST_SNIP_VALUE_2018.toString(), getMetricValueWithXpath(mods, "SNIP", 2018));
            Assert.assertEquals(TEST_SJR_VALUE_2018.toString(), getMetricValueWithXpath(mods, "SJR", 2018));
            Assert.assertEquals(TEST_ENCRYPTED_JCR_VALUE_2018, getMetricValueWithXpath(mods, "JCR", 2018));

            Assert.assertEquals(TEST_SNIP_VALUE_2019.toString(), getMetricValueWithXpath(mods, "SNIP", 2019));
            Assert.assertEquals(TEST_SJR_VALUE_2019.toString(), getMetricValueWithXpath(mods, "SJR", 2019));
            Assert.assertEquals(TEST_ENCRYPTED_JCR_VALUE_2019, getMetricValueWithXpath(mods, "JCR", 2019));
        }
    }

    private String getMetricValueWithXpath(Element mods, String type, int year) {
        return XPathFactory.instance()
            .compile(".//metric[@type='" + type + "']/value[@year='" + year + "']/text()", Filters.text())
            .evaluateFirst(mods).getValue();
    }

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> properties = super.getTestProperties();

        properties.put("MCR.Crypt.Cipher.jcr_intern.class", "org.mycore.crypt.MCRAESCipher");
        properties.put("MCR.Crypt.Cipher.jcr_intern.KeyFile", storeSecret());
        properties.put("MCR.Crypt.Cipher.jcr_intern.EnableACL", "false");

        return properties;
    }

    private String storeSecret() {
        Path file;

        try (InputStream is = MCRClassTools.getClassLoader().getResourceAsStream("test-secret.secret")) {
            file = Files.createTempFile("test-secret", "file");
            Files.copy(Objects.requireNonNull(is), file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return file.toAbsolutePath().toString();
    }
}
