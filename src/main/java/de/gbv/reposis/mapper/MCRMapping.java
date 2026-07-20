package de.gbv.reposis.mapper;

import java.util.Optional;

import de.gbv.reposis.mapper.source.MCRValueSource;

/**
 * Maps raw attributes to a single result, if applicable.
 *
 * @param <V> the type of the raw attribute values
 * @param <R> the type of the mapping result
 */
@FunctionalInterface
public interface MCRMapping<V, R> {

    /**
     * Applies this mapping to the given attribute source.
     *
     * @param source the source providing the raw attribute values
     * @return the mapping result, or an empty {@link Optional} if this mapping does not apply
     */
    Optional<R> apply(MCRValueSource<V> source);
}
