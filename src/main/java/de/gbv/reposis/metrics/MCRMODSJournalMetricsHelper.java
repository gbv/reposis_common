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

package de.gbv.reposis.metrics;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.crypt.MCRCipher;
import org.mycore.crypt.MCRCipherManager;
import org.mycore.crypt.MCRCryptKeyFileNotFoundException;
import org.mycore.crypt.MCRCryptKeyNoPermissionException;
import org.mycore.mods.MCRMODSWrapper;

/**
 * Helper Class to read and write metrics to mycore objects.
 * @author Sebastian Hofmann
 */
public class MCRMODSJournalMetricsHelper {

    public static final String METRICS_EXTENSION_TYPE = "metrics";
    // The key is type of the metric element and the value is the key which is used to encrypt
    public static final Map<String, String> ENCRYPTED_METRIC_TYPES_KEY_MAP = Collections
        .unmodifiableMap(buildEncryptedMetricMap());
    public static final String VALUE_ELEMENT_NAME = "value";
    public static final String TYPE_ATTRIBUTE_NAME = "type";
    public static final String YEAR_ATTRIBUTE_NAME = "year";
    public static final String MODS_EXTENSION_ELEMENT_NAME = "extension";
    public static final String JOURNAL_METRICS_ELEMENT_NAME = "journalMetrics";
    public static final String METRIC_ELEMENT = "metric";
    public static final String METRICS_PROVIDER_PROPERTY_PREFIX = "MCR.MODS.Metrics.Provider.";
    private static final Logger LOGGER = LogManager.getLogger();

    private static Map<String, String> buildEncryptedMetricMap() {
        HashMap<String, String> map = new HashMap<>();
        map.put("JCR", "jcr_intern");
        return map;
    }

    /**
     * Reads metrics from an object.
     *
     * @param object to read the metrics from
     * @return the Journal metrics or null if no extension element is present.
     * @throws MCRCryptKeyFileNotFoundException if the key for the metric type is not found
     *                                          see {@link #ENCRYPTED_METRIC_TYPES_KEY_MAP}
     * @throws MCRCryptKeyNoPermissionException if the permission to encrypt with the key is not present in this context
     */
    public static MCRJournalMetrics getMetrics(MCRMODSWrapper object)
        throws MCRCryptKeyNoPermissionException, MCRCryptKeyFileNotFoundException {
        Objects.requireNonNull(object);
        Element mods = object.getMODS();
        if (!hasMetricsExtensionElement(mods)) {
            return null;
        }

        MCRJournalMetrics journalMetrics = new MCRJournalMetrics();
        Element metricsExtensionElement = getMetricsExtensionElement(mods);
        Element journalMetricsElement = metricsExtensionElement.getChild(JOURNAL_METRICS_ELEMENT_NAME);
        for (Element metricElement : journalMetricsElement.getChildren(METRIC_ELEMENT)) {
            readMetricElement(journalMetrics, metricElement);
        }

        return journalMetrics;
    }

    /**
     * Writes metrics to an object overwriting all existing data. Better use {@link #getMetrics(MCRMODSWrapper)} before.
     *
     * @param object         the mods wrapper with the object
     * @param journalMetrics object which contains the information about the metric or null if the mods extension
     *                       element should be deleted
     * @throws MCRCryptKeyFileNotFoundException if the key for the metric type is not found
     *                                          see {@link #ENCRYPTED_METRIC_TYPES_KEY_MAP}
     * @throws MCRCryptKeyNoPermissionException if the permission to encrypt with the key is not present in this context
     */
    public static void setMetrics(MCRMODSWrapper object, MCRJournalMetrics journalMetrics)
        throws MCRCryptKeyFileNotFoundException, MCRCryptKeyNoPermissionException {
        Objects.requireNonNull(object);
        Element metricsExtensionElement = getMetricsExtensionElement(object.getMODS());
        metricsExtensionElement.removeContent();
        if (journalMetrics == null) {
            metricsExtensionElement.getParentElement().removeContent(metricsExtensionElement);
            return;
        }

        Element metrics = new Element(JOURNAL_METRICS_ELEMENT_NAME);

        Optional.ofNullable(convertYearValueMap("SNIP", journalMetrics.getSnip()))
                .ifPresent(metrics::addContent);

        Optional.ofNullable(convertYearValueMap("SJR", journalMetrics.getSJR()))
                .ifPresent(metrics::addContent);

        Optional.ofNullable(convertYearValueMap("JCR", journalMetrics.getJCR()))
            .ifPresent(metrics::addContent);

        metricsExtensionElement.addContent(metrics);
    }

    private static void readMetricElement(MCRJournalMetrics metrics, Element metricElement)
        throws MCRCryptKeyFileNotFoundException, MCRCryptKeyNoPermissionException {
        String type = metricElement.getAttributeValue(TYPE_ATTRIBUTE_NAME);

        Map<Integer, Double> targetMap = switch (type) {
            case "JCR" -> metrics.getJCR();
            case "SJR" -> metrics.getSJR();
            case "SNIP" -> metrics.getSnip();
            default -> throw new MCRException("Unknown/Unsupported metric found " + type);
        };

        for (Element valueElement : metricElement.getChildren(VALUE_ELEMENT_NAME)) {
            String yearString = valueElement.getAttributeValue(YEAR_ATTRIBUTE_NAME);
            String valueString = valueElement.getTextNormalize();

            int year = Integer.parseInt(yearString);
            double value;

            if (ENCRYPTED_METRIC_TYPES_KEY_MAP.containsKey(type)) {
                String cipherName = ENCRYPTED_METRIC_TYPES_KEY_MAP.get(type);
                String decryptedString = MCRCipherManager.getCipher(cipherName).decrypt(valueString);
                value = Double.parseDouble(decryptedString);
            } else {
                value = Double.parseDouble(valueString);
            }

            targetMap.put(year, value);
        }
    }

    private static Element convertYearValueMap(String type, Map<Integer, Double> yearValueMap)
        throws MCRCryptKeyFileNotFoundException, MCRCryptKeyNoPermissionException {
        if (yearValueMap.size() > 0) {
            Element metricElement = new Element(METRIC_ELEMENT);
            metricElement.setAttribute(TYPE_ATTRIBUTE_NAME, type);
            Collection<Map.Entry<Integer, Double>> entries = yearValueMap.entrySet();
            entries = entries.stream().sorted(Map.Entry.comparingByKey()).collect(Collectors.toList());
            for (Map.Entry<Integer, Double> entry : entries) {
                Integer year = entry.getKey();
                Double value = entry.getValue();
                Element valueElement = new Element(VALUE_ELEMENT_NAME);
                valueElement.setAttribute(YEAR_ATTRIBUTE_NAME, year.toString());
                if (ENCRYPTED_METRIC_TYPES_KEY_MAP.containsKey(type)) {
                    String cipherKeyName = ENCRYPTED_METRIC_TYPES_KEY_MAP.get(type);
                    MCRCipher cipher = MCRCipherManager.getCipher(cipherKeyName);
                    valueElement.setText(cipher.encrypt(value.toString()));
                } else {
                    valueElement.setText(value.toString());
                }
                metricElement.addContent(valueElement);
            }
            return metricElement;
        }
        return null;
    }

    private static Boolean hasMetricsExtensionElement(Element mods) {
        return mods.getChildren(MODS_EXTENSION_ELEMENT_NAME, MCRConstants.MODS_NAMESPACE)
            .stream()
            .anyMatch(extension -> METRICS_EXTENSION_TYPE.equals(extension.getAttributeValue(TYPE_ATTRIBUTE_NAME)));
    }

    private static Element getMetricsExtensionElement(Element mods) {
        List<Element> extensionElements = mods.getChildren(MODS_EXTENSION_ELEMENT_NAME, MCRConstants.MODS_NAMESPACE);
        List<Element> metricsExtensionElements = extensionElements.stream()
            .filter(extension -> METRICS_EXTENSION_TYPE.equals(extension.getAttributeValue(TYPE_ATTRIBUTE_NAME)))
            .collect(Collectors.toList());
        switch (metricsExtensionElements.size()) {
        case 1:
            return metricsExtensionElements.get(0);
        case 0:
            Element extensionElement = new Element(MODS_EXTENSION_ELEMENT_NAME, MCRConstants.MODS_NAMESPACE);
            extensionElement.setAttribute(TYPE_ATTRIBUTE_NAME, METRICS_EXTENSION_TYPE);
            mods.addContent(extensionElement);
            return extensionElement;
        default:
            throw new MCRException("Multiple Extensions with type='metrics' found!");
        }
    }

    /**
     * Reads the issn(s) with the mods wrapper
     * @param mw a mods wrapper with a mods document
     * @return a list of issn of the document
     */
    public static List<String> getIssn(MCRMODSWrapper mw) {
        Element mods = mw.getMODS();
        return mods.getChildren("identifier", MCRConstants.MODS_NAMESPACE).stream()
            .filter(el -> "issn".equals(el.getAttributeValue("type")))
            .map(Element::getTextNormalize)
            .collect(Collectors.toList());
    }

    /**
     * Updates the metrics of a mods document using the {@link MCRMetricsProvider} in MCR.MODS.Metrics.Provider.
     * @param issnList a list of issn
     * @param mw a mods wrapper with a mods document
     * @return true if the metrics have changed and the document needs to be saved
     * @throws MCRCryptKeyNoPermissionException if EnableACL=false is not set
     * @throws MCRCryptKeyFileNotFoundException if the crypt key used to modify objects is not
     */
    public static boolean updateMetrics(List<String> issnList, MCRMODSWrapper mw)
        throws MCRCryptKeyNoPermissionException, MCRCryptKeyFileNotFoundException {
        return updateMetrics(issnList, mw, Collections.emptySet());
    }

    /**
     * Updates the metrics of a mods document using the {@link MCRMetricsProvider} in MCR.MODS.Metrics.Provider.
     * @param issnList a list of issn
     * @param mw a mods wrapper with a mods document
     * @param years a set of years for which the metrics should be updated.
     *              If empty then all possible years will be updated.
     * @return true if the metrics have changed and the document needs to be saved
     * @throws MCRCryptKeyNoPermissionException if EnableACL=false is not set
     * @throws MCRCryptKeyFileNotFoundException if the crypt key used to modify objects is not
     */
    public static boolean updateMetrics(List<String> issnList, MCRMODSWrapper mw, Set<Integer> years)
        throws MCRCryptKeyNoPermissionException, MCRCryptKeyFileNotFoundException {
        Map<String, List<String>> metadata = new HashMap<>();
        metadata.put("issn", issnList);
        Map<String, Callable<MCRMetricsProvider>> metricsProviders
            = MCRConfiguration2.getInstances(METRICS_PROVIDER_PROPERTY_PREFIX);

        List<MCRJournalMetrics> newMetrics = metricsProviders.entrySet().stream().map(entry -> {
            String metadataAsString = getMetadataAsString(metadata);

            LOGGER.info("Loading metrics for {} with provider {}", metadataAsString, entry.getKey());
            try {
                MCRMetricsProvider metricsProvider = entry.getValue().call();
                MCRJournalMetrics metrics = metricsProvider.getMetrics(metadata, years);
                LOGGER.info("Got these metrics: {}", metrics.toString());
                return metrics;
            } catch (Exception e) {
                throw new MCRException(e);
            }
        }).toList();

        boolean changed = false;
        MCRJournalMetrics currentMetrics = Optional.ofNullable(getMetrics(mw))
            .orElseGet(MCRJournalMetrics::new);

        for (MCRJournalMetrics journalMetrics : newMetrics) {
            changed |= mergeMetrics(currentMetrics, journalMetrics);
        }

        setMetrics(mw, currentMetrics);

        return changed;
    }

    private static String getMetadataAsString(Map<String, List<String>> metadata) {
        return metadata.entrySet()
            .stream()
            .map(e -> e.getKey() + ":" + String.join(";", e.getValue()))
            .collect(Collectors.joining(";"));
    }

    private static boolean mergeMetrics(MCRJournalMetrics metrics, MCRJournalMetrics additionalMetrics) {
        boolean changed;
        changed = mergeMetricsMap(metrics.getJCR(), additionalMetrics.getJCR());
        changed |= mergeMetricsMap(metrics.getSJR(), additionalMetrics.getSJR());
        changed |= mergeMetricsMap(metrics.getSnip(), additionalMetrics.getSnip());
        return changed;
    }

    private static boolean mergeMetricsMap(Map<Integer, Double> metricsMap, Map<Integer, Double> additionalMetricsMap) {
        boolean changed = false;
        for (Map.Entry<Integer, Double> entry : additionalMetricsMap.entrySet()) {
            // if the value is 0 or below then the current value will be deleted
            if (entry.getValue() <= 0
                && (!metricsMap.containsKey(entry.getKey()) || metricsMap.get(entry.getKey()) <= 0)) {
                metricsMap.remove(entry.getKey());
                changed = true;
                continue;
            }

            // if the values already match, then nothing needs to be done
            if (metricsMap.containsKey(entry.getKey()) && metricsMap.get(entry.getKey()).equals(entry.getValue())) {
                continue;
            }

            // the value changed, so it needs to be updated
            changed = true;
            metricsMap.put(entry.getKey(), entry.getValue());
        }

        return changed;
    }
}
