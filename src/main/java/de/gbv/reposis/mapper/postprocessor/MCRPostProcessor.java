package de.gbv.reposis.mapper.postprocessor;

/**
 * Transforms a matching value further into its final form.
 *
 * @param <V> the type of the value to process
 */
@FunctionalInterface
public interface MCRPostProcessor<V> {

    /**
     * Processes the given value.
     *
     * @param value the value to process
     * @return the processed value
     */
    V process(V value);
}
