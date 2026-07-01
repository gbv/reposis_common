package de.gbv.reposis.user.mapper.role;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A single mapping that derives a role from raw attributes.
 */
public interface MCRRoleMapping {

    /**
     * Applies this mapping to the given raw attributes.
     *
     * @param attributes the raw attributes, keyed by attribute name, with each value being the
     *                   (possibly multi-valued) list of values for that attribute
     * @return the role derived from the given attributes, or {@link Optional#empty()} if this
     *         mapping does not apply to the given attributes
     */
    Optional<String> apply(Map<String, List<String>> attributes);
}
