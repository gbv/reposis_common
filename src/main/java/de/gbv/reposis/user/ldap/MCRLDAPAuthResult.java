package de.gbv.reposis.user.ldap;

import java.util.List;
import java.util.Map;

/**
 * Holds the raw LDAP attributes returned after a successful authentication bind.
 *
 * @param attributes map of LDAP attribute name to list of values
 */
 public record MCRLDAPAuthResult(Map<String, List<String>> attributes) {
}
