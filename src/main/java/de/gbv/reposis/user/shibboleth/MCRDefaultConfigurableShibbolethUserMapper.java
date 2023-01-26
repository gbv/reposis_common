package de.gbv.reposis.user.shibboleth;

import org.mycore.user2.MCRRealm;
import org.mycore.user2.MCRRealmFactory;
import org.mycore.user2.MCRUser;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;

/**
 * The default user mapper which maps the following attributes:
 * <ul>
 *     <li>mail</li>
 *     <li>displayName</li>
 * </ul>
 *
 * The userid is set to the remote user from the request.
 *
 * @author Sebastian Hofmann
 */
public class MCRDefaultConfigurableShibbolethUserMapper implements MCRShibbolethUserMapper {

    public MCRDefaultConfigurableShibbolethUserMapper() {
        super();
    }

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
}
