package de.gbv.reposis.mapper;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import de.gbv.reposis.mapper.extractor.MCRExtractor;
import de.gbv.reposis.mapper.matcher.MCRValueMatcher;
import de.gbv.reposis.mapper.postprocessor.MCRPostProcessor;
import de.gbv.reposis.mapper.source.MCRValueSource;

/**
 * A generic {@link MCRMapping} that composes three steps: first, a value is extracted from the
 * raw attribute values found under a configured attribute name; then it is checked whether that
 * value matches a given condition; if so, it is post-processed into the final result.
 * <p>
 * If either the source yields no value, the extraction fails, or the matcher does not match, the
 * mapping produces no result.
 *
 * @param <V> the type of the intermediate, extracted value
 */
public class MCRPipelineMapping<V> implements MCRMapping<V, V> {

    private final String attributeName;
    private final MCRExtractor<V> extractor;
    private final MCRValueMatcher<V> matcher;
    private final MCRPostProcessor<V> postProcessor;

    /**
     * Creates a new {@code MCRPipelineMapping}.
     *
     * @param attributeName the name of the attribute to read the raw value(s) from
     * @param extractor extracts the intermediate value from a raw value, or {@code null} if the
     *                  raw value should be used as-is
     * @param matcher decides whether the extracted value matches, or {@code null} if every
     *                extracted value should be considered a match
     * @param postProcessor transforms a matching value further, or {@code null} if no further
     *                      processing is needed
     */
    public MCRPipelineMapping(String attributeName, MCRExtractor<V> extractor,
        MCRValueMatcher<V> matcher, MCRPostProcessor<V> postProcessor) {
        this.attributeName = Objects.requireNonNull(attributeName, "attributeName must not be null");
        this.extractor = extractor != null ? extractor : Optional::of;
        this.matcher = matcher != null ? matcher : value -> true;
        this.postProcessor = postProcessor != null ? postProcessor : value -> value;
    }

    /**
     * Applies this pipeline to the given attribute source.
     *
     * @param source the source providing the raw attribute values
     * @return the pipeline's result, or an empty {@link Optional} if the source yields no
     *         value, or no extracted value matches
     */
    @Override
    public Optional<V> apply(MCRValueSource<V> source) {
        return source.getValues(attributeName)
            .stream()
            .flatMap(List::stream)
            .map(extractor::extract)
            .flatMap(Optional::stream)
            .filter(matcher::test)
            .findFirst()
            .map(postProcessor::apply);
    }
}
