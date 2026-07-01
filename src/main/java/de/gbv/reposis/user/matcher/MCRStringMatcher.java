package de.gbv.reposis.user.matcher;

/**
 * A single condition that checks whether a given string value matches.
 */
public interface MCRStringMatcher {

    /**
     * Checks whether the given value matches this condition.
     *
     * @param actualValue the value to check
     * @return {@code true} if the value matches, {@code false} otherwise
     */
    boolean matches(String actualValue);
}
