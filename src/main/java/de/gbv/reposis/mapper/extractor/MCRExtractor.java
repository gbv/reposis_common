package de.gbv.reposis.mapper.extractor;

import java.util.Optional;

/**
 * Extracts an intermediate value from a raw attribute value.
 *
 * @param <V> the type of the raw and extracted value
 */
@FunctionalInterface
public interface MCRExtractor<V> {

    /**
     * Extracts a value from the given raw value.
     *
     * @param rawValue the raw value to extract from
     * @return the extracted value, or an empty {@link Optional} if extraction failed
     */
    Optional<V> extract(V rawValue);
}
