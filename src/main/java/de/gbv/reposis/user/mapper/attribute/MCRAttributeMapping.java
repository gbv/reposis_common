package de.gbv.reposis.user.mapper.attribute;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A single mapping that derives a name/value pair from raw attributes.
 */
public interface MCRAttributeMapping {

    /**
     * Applies this mapping to the given raw attributes.
     *
     * @param attributes the raw attributes, keyed by attribute name, with each value being the
     *                    (possibly multi-valued) list of values for that attribute
     * @return the result of this mapping, or {@link Optional#empty()} if this mapping does not
     *         apply to the given attributes
     */
    Optional<Result> apply(Map<String, List<String>> attributes);

    /**
     * The result of a single applied mapping.
     *
     * @param key the resulting attribute name
     * @param value the resulting attribute value
     */
    record Result(String key, String value) {
        public Result {
            Objects.requireNonNull(key, "key must not be null");
            Objects.requireNonNull(value, "value must not be null");
        }
    }
}
