package de.gbv.reposis.mapper.matcher;

import java.util.function.Predicate;

/**
 * A specialization of {@link Predicate} used throughout the mapping pipeline to express
 * that a given value matches some condition.
 *
 * @param <V> the type of the value to test
 */
@FunctionalInterface
public interface MCRValueMatcher<V> extends Predicate<V> {
}
