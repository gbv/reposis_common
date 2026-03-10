package de.gbv.reposis.user.ldap.mapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A single mapping that derives a value from raw LDAP attributes.
 */
public interface MCRLDAPMapping {

    /**
     * Applies this mapping to the given LDAP attributes.
     *
     * @param ldapAttributes raw attributes returned by the LDAP server
     * @return mapping result, or empty if this mapping does not apply
     */
    Optional<Result> apply(Map<String, List<String>> ldapAttributes);

    record Result(String key, String value) {}
}
