package de.gbv.reposis.mapper.source;

import java.util.List;
import java.util.Optional;

/**
 * Provides raw attribute values by name, regardless of how or when the underlying system
 * actually retrieves them.
 *
 * @param <V> the type of the raw attribute values
 */
@FunctionalInterface
public interface MCRValueSource<V> {

    /**
     * Returns the raw values for the given attribute name.
     *
     * @param name the attribute name
     * @return the raw values, or an empty {@link Optional} if no values are available for the
     *         given name
     */
    Optional<List<V>> getValues(String name);
}
