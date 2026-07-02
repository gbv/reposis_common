package de.gbv.reposis.user;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Holds user information to be used for creating or updating an {@code MCRUser}.
 *
 * @param username the unique identifier of the user within its realm
 * @param realmId the realm id
 * @param roles the roles assigned to the user, to be mapped onto roles
 * @param attributes additional user attributes (e.g. real name, email), keyed by attribute name
 */
public record MCRUserData(String username, String realmId, Set<String> roles, Map<String, String> attributes) {

    /**
     * Creates an immutable user data instance.
     * <p>
     * The username and realm ID must not be {@code null}. Missing roles or
     * attributes are replaced with empty collections. Provided collections are
     * defensively copied.
     */
    public MCRUserData {
        username = Objects.requireNonNull(username, "username must not be null");
        realmId = Objects.requireNonNull(realmId, "realmId must not be null");
        roles = roles == null ? Set.of() : Set.copyOf(roles);
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    /**
     * Returns the fully qualified user ID, composed of {@code username} and {@code realmId}.
     *
     * @return the user ID in the form {@code username@realmId}
     */
    public String userId() {
        return username + "@" + realmId;
    }
}
