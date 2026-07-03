package de.gbv.reposis.user.postprocessor;

/**
 * A post processor that transforms a string value into another string value.
 * <p>
 * Post-processing is applied after a value has been successfully extracted or matched
 * and is used to derive the final representation (e.g. adding prefixes, suffixes,
 * formatting, or other transformations).
 * <p>
 * Implementations must be stateless or explicitly document any internal state.
 */
public interface MCRStringPostProcessor {

    /**
     * Transforms the given input value into a processed output value.
     *
     * @param value the input value to process; must not be {@code null}
     * @return the processed string value; must not be {@code null}
     */
    String process(String value);
}
