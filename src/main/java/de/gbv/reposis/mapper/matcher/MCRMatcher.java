package de.gbv.reposis.mapper.matcher;

/**
 * Decides whether an extracted value matches a given condition.
 *
 * @param <V> the type of the value to check
 */
@FunctionalInterface
public interface MCRMatcher<V> {

    /**
     * Checks whether the given value matches.
     *
     * @param value the value to check
     * @return {@code true} if the value matches, {@code false} otherwise
     */
    boolean matches(V value);
}
