package de.gbv.reposis.user.mapper.attribute;

import java.util.Map;
import java.util.Objects;

import de.gbv.reposis.mapper.MCRMapping;

/**
 * A single mapping that derives a name/value pair from raw attributes.
 */
public interface MCRUserMapping extends MCRMapping<String, Map.Entry<String, String>> {

    /**
     * Wraps a mapping's result together with a fixed target key into a {@link MCRUserMapping}.
     *
     * @param mapping the mapping producing the value
     * @param targetName the target key the value should be mapped to
     * @return a user mapping producing an entry of {@code targetName} and the mapping's result
     */
    static MCRUserMapping withTarget(MCRMapping<String, String> mapping, String targetName) {
        Objects.requireNonNull(mapping, "mapping must not be null");
        Objects.requireNonNull(targetName, "targetName must not be null");
        return source -> mapping.apply(source).map(value -> Map.entry(targetName, value));
    }
}
