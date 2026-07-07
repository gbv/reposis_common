package de.gbv.reposis.mapper.postprocessor;

import java.util.function.UnaryOperator;

/**
 * Transforms a matching value further into its final form.
 * <p>
 * A specialization of {@link UnaryOperator} used as the final step of the mapping pipeline.
 *
 * @param <V> the type of the value to process
 */
@FunctionalInterface
public interface MCRPostProcessor<V> extends UnaryOperator<V> {
}
