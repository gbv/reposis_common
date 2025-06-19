package de.gbv.reposis.user.shibboleth;

import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.mcr.cronjob.MCRCronjob;
import org.mycore.user2.MCRRealmFactory;
import org.mycore.user2.MCRUserManager;

/**
 * The realm updater takes the dfn aai metadata and takes all the scopes and creates a realm for each scope.
 *
 * @author Sebastian Hofmann
 */
public class MCRSAMLEntitiesRealmFileUpdater extends MCRCronjob {

    public static final Namespace NAMESPACE_REMD = Namespace.getNamespace("remd", "http://refeds.org/metadata");
    public static final Namespace NAMESPACE_SAML
        = Namespace.getNamespace("saml", "urn:oasis:names:tc:SAML:2.0:assertion");
    public static final Namespace NAMESPACE_SAML_1_MD
        = Namespace.getNamespace("saml1md", "urn:mace:shibboleth:metadata:1.0");
    public static final Namespace NAMESPACE_MDRPI
        = Namespace.getNamespace("mdrpi", "urn:oasis:names:tc:SAML:metadata:rpi");
    public static final Namespace NAMESPACE_MDATTR
        = Namespace.getNamespace("mdattr", "urn:oasis:names:tc:SAML:metadata:attribute");
    public static final Namespace NAMEPSACE_MDUI
        = Namespace.getNamespace("mdui", "urn:oasis:names:tc:SAML:metadata:ui");
    public static final Namespace NAMESPACE_DS = Namespace.getNamespace("ds", "http://www.w3.org/2000/09/xmldsig#");
    public static final Namespace NAMESPACE_MD = Namespace.getNamespace("md", "urn:oasis:names:tc:SAML:2.0:metadata");
    public static final List<Namespace> SAML_NAMESPACES = Stream.of(
        NAMESPACE_MD,
        NAMESPACE_DS,
        NAMEPSACE_MDUI,
        NAMESPACE_MDATTR,
        NAMESPACE_MDRPI,
        NAMESPACE_SAML_1_MD,
        NAMESPACE_SAML,
        NAMESPACE_REMD)
        .collect(Collectors.toList());
    public static final XPathExpression<Element> ENTITY_DESCRIPTOR_XPATH = XPathFactory.instance()
        .compile("//md:EntitiesDescriptor/md:EntityDescriptor", Filters.element(), null, SAML_NAMESPACES);

    public static final String DATA_DIR_PROPERTY = "MCR.datadir";

    private static final Logger LOGGER = LogManager.getLogger();
    private URL url;
    private Set<String> filterEntities;
    private Path realmsFilePath;
    private boolean preserveRealmsWithUsers;
    private Set<String> preserveRealms;

    public MCRSAMLEntitiesRealmFileUpdater() {
        super();
        setFilterEntities(new HashSet<>());
        setPreserveRealmsWithUsers(true);
        this.preserveRealms = new HashSet<>();
        setPreserveRealms("local,shibboleth");
        setRealmsFilePath(Paths.get(MCRConfiguration2.getStringOrThrow(DATA_DIR_PROPERTY), "realms.xml"));
    }

    public Path getRealmsFilePath() {
        return realmsFilePath;
    }

    public void setRealmsFilePath(Path realmsFilePath) {
        this.realmsFilePath = realmsFilePath;
    }

    @MCRProperty(name = "RealmsFilePath", required = false)
    public void setRealmsFilePath(String realmsFilePath) {
        this.realmsFilePath = Paths.get(realmsFilePath);
    }

    public Set<String> getPreserveRealms() {
        return preserveRealms;
    }

    @MCRProperty(name = "PreserveRealms", required = false)
    public void setPreserveRealms(String preserveRealms) {
        this.preserveRealms = Arrays.stream(preserveRealms.split(","))
            .map(String::trim)
            .collect(Collectors.toSet());
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    @MCRProperty(name = "Url", required = true)
    public void setUrl(String url) throws MalformedURLException {
        setUrl(URI.create(url).toURL());
    }
    public boolean isPreserveRealmsWithUsers() {
        return preserveRealmsWithUsers;
    }

    public void setPreserveRealmsWithUsers(boolean preserveRealmsWithUsers) {
        this.preserveRealmsWithUsers = preserveRealmsWithUsers;
    }

    @MCRProperty(name = "PreserveRealmsWithUsers", required = false)
    public void setPreserveRealmsWithUsers(String preserveRealmsWithUsers) {
        this.preserveRealmsWithUsers = Boolean.parseBoolean(preserveRealmsWithUsers);
    }

    public Set<String> getFilterEntities() {
        return filterEntities;
    }

    public void setFilterEntities(Set<String> filterEntities) {
        this.filterEntities = filterEntities;
    }

    @MCRProperty(name = "FilterEntities", required = false)
    public void setFilterEntities(String filterEntities) {
        this.filterEntities = new HashSet<>(Arrays.asList(filterEntities.split(",")));
    }

    @Override
    public void runJob() {
        try {
            Document document = MCRRealmFactory.getRealmsDocument();

            Element realmsElement = document.getRootElement();
            Map<String, Element> realmMap = realmsElement.getChildren("realm")
                .stream()
                .collect(Collectors.toMap(realm -> realm.getAttributeValue("id"), realm -> realm));
            Map<String, RealmInfo> remoteRealms = getRemoteRealms();
            boolean changed = synchronizeRealmXML(remoteRealms, realmMap, realmsElement);
            if (changed) {
                XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
                try (OutputStream outputStream = Files.newOutputStream(getRealmsFilePath())) {
                    outputter.output(document, outputStream);
                } catch (IOException ex) {
                    LOGGER.error("Error while writing realms file", ex);
                }
            }

        } catch (Exception e) {
            LOGGER.error("Error while updating realms", e);
        }
    }

    public boolean synchronizeRealmXML(Map<String, RealmInfo> newRealmMap, Map<String, Element> oldRealmMap,
        Element realmsElement) {
        boolean changed = false;
        for (Map.Entry<String, Element> realmEntry : oldRealmMap.entrySet()) {
            String realmId = realmEntry.getKey();
            Element realmElement = realmEntry.getValue();
            if (!newRealmMap.containsKey(realmId)) {
                if (getPreserveRealms().contains(realmId)) {
                    LOGGER.info("Realm {} is preserved, because it is listed in PreserveRealms", realmId);
                } else if (isPreserveRealmsWithUsers() && MCRUserManager.countUsers("", realmId, "", "") > 0) {
                    LOGGER.info("Realm {} is preserved, because it contains users", realmId);
                } else {
                    LOGGER.info("Realm {} is removed, because it is not listed in remote metadata", realmId);
                    realmElement.detach();
                    changed = true;
                }
            }
        }
        for (Map.Entry<String, RealmInfo> newRealmInfoEntry : newRealmMap.entrySet()) {
            RealmInfo newRealmInfo = newRealmInfoEntry.getValue();
            Element newRealmInfoXML = newRealmInfo.asXML();
            if (oldRealmMap.containsKey(newRealmInfo.id)) {
                Element element = oldRealmMap.get(newRealmInfo.id);
                if (!MCRXMLHelper.deepEqual(newRealmInfoXML, element)) {
                    LOGGER.info("Realm {} is updated", newRealmInfo.id);
                    element.detach();
                    realmsElement.addContent(newRealmInfoXML);
                    changed = true;
                }
            } else {
                realmsElement.addContent(newRealmInfoXML);
                changed = true;
            }

        }
        return changed;
    }

    public Map<String, RealmInfo> getRemoteRealms() throws IOException, JDOMException {
        Document document = downloadIDPList();
        HashMap<String, RealmInfo> realms = new HashMap<>();

        List<Element> entities = ENTITY_DESCRIPTOR_XPATH.evaluate(document);

        for (Element entity : entities) {
            String entityID = entity.getAttributeValue("entityID");
            if (getFilterEntities().contains(entityID)) {
                LOGGER.info("Skipping entity " + entityID + " because it is in the filter list.");
                continue;
            }

            Element attributeAuthorityDescriptor = entity.getChild("AttributeAuthorityDescriptor", NAMESPACE_MD);
            if (attributeAuthorityDescriptor == null) {
                LOGGER.info("Skipping entity " + entityID + " because it has no AttributeAuthorityDescriptor.");
                continue;
            }

            Element extensions = attributeAuthorityDescriptor.getChild("Extensions", NAMESPACE_MD);
            if (extensions == null) {
                LOGGER.info(
                    "Skipping entity " + entityID + " because it has no AttributeAuthorityDescriptor/Extensions.");
                continue;
            }

            List<Element> scopeList = extensions.getChildren("Scope", NAMESPACE_SAML_1_MD);

            if (scopeList.isEmpty()) {
                LOGGER.info("Skipping entity " + entityID
                    + " because it has no AttributeAuthorityDescriptor/Extensions/Scope.");
                continue;
            }

            for (Element scope : scopeList) {
                String scopeID = scope.getValue();
                if (scope.getAttributeValue("regexp").equals("true")) {
                    LOGGER.info("Skipping Scope " + entityID + ":" + scopeID + " because it has a regexp scope.");
                    continue;
                }

                RealmInfo realm = new RealmInfo(scopeID);
                Element organization = entity.getChild("Organization", NAMESPACE_MD);
                if (organization != null) {
                    List<Element> organizationDisplayNames
                        = organization.getChildren("OrganizationDisplayName", NAMESPACE_MD);
                    for (Element organizationDisplayName : organizationDisplayNames) {
                        String language = organizationDisplayName.getAttributeValue("lang", Namespace.XML_NAMESPACE);
                        String displayName = organizationDisplayName.getValue();
                        displayName = Normalizer.normalize(displayName, Normalizer.Form.NFC);
                        displayName = displayName.trim();

                        realm.getLabels().put(language, displayName);
                    }
                }

                realms.put(realm.getId(), realm);
            }

        }

        return realms;
    }

    public Document downloadIDPList() throws IOException, JDOMException {
        return new SAXBuilder().build(url);
    }

    @Override
    public String getDescription() {
        return "Update Realms.xml file with SAML Entities ";
    }

    public static class RealmInfo {
        private String id;
        private Map<String, String> labels = new HashMap<>();
        private String passwordChangeURL;
        private String loginURL;
        private String createURL;
        private String redirectParameter;
        private String realmParameter;

        public RealmInfo(String scopeID) {
            this.id = scopeID;
            this.labels = new HashMap<>();
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Map<String, String> getLabels() {
            return labels;
        }

        public String getPasswordChangeURL() {
            return passwordChangeURL;
        }

        public void setPasswordChangeURL(String passwordChangeURL) {
            this.passwordChangeURL = passwordChangeURL;
        }

        public String getLoginURL() {
            return loginURL;
        }

        public void setLoginURL(String loginURL) {
            this.loginURL = loginURL;
        }

        public String getCreateURL() {
            return createURL;
        }

        public void setCreateURL(String createURL) {
            this.createURL = createURL;
        }

        public String getRedirectParameter() {
            return redirectParameter;
        }

        public void setRedirectParameter(String redirectParameter) {
            this.redirectParameter = redirectParameter;
        }

        public String getRealmParameter() {
            return realmParameter;
        }

        public void setRealmParameter(String realmParameter) {
            this.realmParameter = realmParameter;
        }

        public Element asXML() {
            Element realm = new Element("realm");
            realm.setAttribute("id", id);
            for (Map.Entry<String, String> label : labels.entrySet()) {
                Element labelElement = new Element("label");
                if (label.getKey() != null) {
                    labelElement.setAttribute("lang", label.getKey(), Namespace.XML_NAMESPACE);
                }
                labelElement.setText(label.getValue());
                realm.addContent(labelElement);
            }
            return realm;
        }
    }
}