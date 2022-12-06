package de.gbv.reposis.config;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.mycore.common.config.MCRComponent;
import org.mycore.common.config.MCRConfigurationDir;
import org.mycore.common.config.MCRConfigurationInputStream;
import org.mycore.common.config.MCRRuntimeComponentDetector;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRFileContent;
import org.mycore.common.content.MCRURLContent;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class PropertyHelper {

    private final LinkedHashMap<String, byte[]> configFileContents;

    public PropertyHelper() throws IOException {
        configFileContents = getConfigFileContents("mycore.properties");
    }

    public static LinkedHashMap<String, byte[]> getConfigFileContents(String filename) throws IOException {
        LinkedHashMap<String, byte[]> map = new LinkedHashMap<>();
        for (MCRComponent component : MCRRuntimeComponentDetector.getAllComponents()) {
            try (InputStream is = component.getConfigFileStream(filename)) {
                if (is != null) {
                    map.put(component.getName(), IOUtils.toByteArray(is));
                }
            }
        }
        // load config file from classpath
        try (InputStream configStream = getConfigFileStream(filename)) {
            if (configStream != null) {
                LogManager.getLogger().debug("Loaded config file from classpath: " + filename);
                map.put("classpath_" + filename, IOUtils.toByteArray(configStream));
            }
        }

        //load config file from app config dir
        File localConfigFile = MCRConfigurationDir.getConfigFile(filename);
        if (localConfigFile != null && localConfigFile.canRead()) {
            LogManager.getLogger().debug("Loaded config file from config dir: " + filename);
            try (FileInputStream fis = new FileInputStream(localConfigFile)) {
                map.put("configdir_" + filename, IOUtils.toByteArray(fis));
            }
        }
        return map;
    }

    private static InputStream getConfigFileStream(String filename) throws IOException {
        File cfgFile = new File(filename);
        MCRContent input = null;
        if (cfgFile.canRead()) {
            input = new MCRFileContent(cfgFile);
        } else {
            URL url = MCRConfigurationInputStream.class.getClassLoader().getResource(filename);
            if (url != null) {
                input = new MCRURLContent(url);
            }
        }
        return input == null ? null : input.getInputStream();
    }

    public Map<String, List<Property>> analyzeProperties() throws IOException {
        HashMap<String, String> currentProperties = new HashMap<>();

        LinkedHashMap<String, List<Property>> analyzedProperties = new LinkedHashMap<>();

        for (String component : configFileContents.keySet()) {
            byte[] value = configFileContents.get(component);
            Properties properties = new Properties();
            properties.load(new ByteArrayInputStream(value));
            properties.forEach((k, v) -> {
                String propertyName = (String) k;
                String propertyValue = interpolatePropertyValue(propertyName, (String) v, currentProperties);
                String oldValue = currentProperties.get(propertyName);
                analyzedProperties.computeIfAbsent(component, k1 -> new LinkedList<>())
                        .add(new Property(component, propertyName, oldValue, propertyValue));
                currentProperties.put(propertyName, propertyValue);
            });
        }
        return analyzedProperties;
    }

    private String interpolatePropertyValue(String key, String value, Map<String, String> currentProperties) {
        String oldValue = currentProperties.get(key);
        String newValue = oldValue == null ? value : value.replaceAll('%' + key + '%', oldValue);
        if (!newValue.equals(value) && newValue.startsWith(",")) {
            //replacement took place, but starts with 'empty' value
            newValue = newValue.substring(1);
        }
        return newValue;
    }

}
