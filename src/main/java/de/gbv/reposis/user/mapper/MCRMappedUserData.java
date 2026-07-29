package de.gbv.reposis.user.mapper;

import java.util.Map;
import java.util.Set;

/**
 * The result of mapping raw attributes to user-related data, consisting of the derived roles
 * and the derived user attributes.
 *
 * @param roles the set of role names derived from the raw attributes
 * @param attributes the user attributes derived from the raw attributes, keyed by attribute name
 */
public record MCRMappedUserData(Set<String> roles, Map<String, String> attributes) {
}
