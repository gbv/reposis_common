package de.gbv.reposis.user.shibboleth;

import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.user2.MCRRealm;
import org.mycore.user2.MCRRealmFactory;
import org.mycore.user2.MCRUser;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Collections;
import java.util.List;

/**
 * The default user mapper which maps the following attributes:
 * <ul>
 *     <li>mail</li>
 *     <li>displayName</li>
 * </ul>
 *
 * The userid is set to the remote user from the request.
 *
 *
 * @author Sebastian Hofmann
 */
public class MCRDefaultConfigurableShibbolethUserMapper implements MCRShibbolethUserMapper {

    public MCRDefaultConfigurableShibbolethUserMapper() {
        super();
        setDefaultRoles(Collections.emptyList());
    }

    private List<String> defaultRoles;

    @Override
    public MCRUser mapAttributes(String remoteUser, MCRRequestAttributeResolver requestAttributeResolver) {
        String[] realmParts = remoteUser.split("@", 2);
        if (realmParts.length != 2) {
            throw new IllegalArgumentException("id must be in the form of 'username@realm'");
        }

        String userId = normalizeString(realmParts[0]);
        String realmId = normalizeString(realmParts[1]);

        MCRRealm realm = MCRRealmFactory.getRealm(realmId);
        MCRUser user = new MCRUser(userId, realm);

        assignAttributes(user, requestAttributeResolver);

        return user;
    }

    protected void assignAttributes(MCRUser user, MCRRequestAttributeResolver requestAttributeResolver) {
        user.setRealName(normalizeString(requireString(requestAttributeResolver, "displayName")));
        user.setEMail(normalizeString(requireString(requestAttributeResolver, "mail")));

        getDefaultRoles().forEach(user::assignRole);
    }

    public String requireString(MCRRequestAttributeResolver MCRRequestAttributeResolver, String key) {
        Object value = MCRRequestAttributeResolver.resolve(key);
        return requireString(key, value);
    }

    public String requireString(String name, Object attr) {
        if (attr == null) {
            throw new IllegalArgumentException("Attribute " + name + " is null");
        }

        if (!(attr instanceof String)) {
            throw new IllegalArgumentException("Attribute " + name + " is not a String");
        }

        return (String) attr;
    }

    public String normalizeString(String value) {
        if (value == null) {
            return null;
        }
        String normalizedString = new String(value.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
        normalizedString = Normalizer.normalize(normalizedString, Normalizer.Form.NFC);
        return normalizedString;
    }

    @MCRProperty(name = "DefaultRoles", required = false)
    public void setDefaultRoles(String defaultRoles) {
        this.defaultRoles = MCRConfiguration2.splitValue(defaultRoles).toList();
    }

    public void setDefaultRoles(List<String> defaultRoles) {
        this.defaultRoles = defaultRoles;
    }

    public List<String> getDefaultRoles() {
        return defaultRoles;
    }
}
