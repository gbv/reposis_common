package de.gbv.reposis.user.mapper.attribute;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstance;
import org.mycore.common.config.annotation.MCRProperty;

import de.gbv.reposis.mapper.MCRPipelineMapping;
import de.gbv.reposis.mapper.extractor.MCRExtractor;
import de.gbv.reposis.mapper.matcher.MCRValueMatcher;
import de.gbv.reposis.mapper.postprocessor.MCRPostProcessor;
import de.gbv.reposis.mapper.source.MCRValueSource;

/**
 * A configurable {@link MCRUserMapping} that combines a source attribute, an optional
 * extractor, an optional matcher, and an optional post-processor into an intermediate value,
 * which is then mapped to a fixed, configured target key.
 */
@MCRConfigurationProxy(proxyClass = MCRConfigurableUserAttributePipelineMapping.Factory.class)
public class MCRConfigurableUserAttributePipelineMapping implements MCRUserMapping {

    private final MCRUserMapping delegate;

    /**
     * Creates a new {@code MCRConfigurableUserAttributePipelineMapping}.
     *
     * @param attributeName the name of the attribute to read the raw value(s) from
     * @param extractor the extractor that derives a value from the raw values, or {@code null}
     *                  if the raw value should be used as-is
     * @param matcher the matcher that decides whether the extracted value matches, or
     *                {@code null} if every extracted value should be considered a match
     * @param postProcessor the post-processor that further transforms a matching value, or
     *                      {@code null} if no further processing is needed
     * @param targetName the target key the final value should be mapped to
     */
    public MCRConfigurableUserAttributePipelineMapping(String attributeName,
        MCRExtractor<String> extractor, MCRValueMatcher<String> matcher,
        MCRPostProcessor<String> postProcessor, String targetName) {
        MCRPipelineMapping<String> pipeline =
            new MCRPipelineMapping<>(attributeName, extractor, matcher, postProcessor);
        this.delegate = MCRUserMapping.withTarget(pipeline, targetName);
    }

    /**
     * Applies the pipeline to the given attribute source and maps its result to the configured
     * target key.
     *
     * @param source the source providing the raw attribute values
     * @return an entry consisting of the configured target key and the pipeline's result, or
     *         an empty {@link Optional} if the pipeline produces no result
     */
    @Override
    public Optional<Map.Entry<String, String>> apply(MCRValueSource<String> source) {
        return delegate.apply(source);
    }

    /**
     * Factory for creating {@link MCRConfigurableUserAttributePipelineMapping} instances from configuration properties.
     */
    public static class Factory implements Supplier<MCRConfigurableUserAttributePipelineMapping> {

        @MCRProperty(name = "Source")
        public String attributeName;

        @MCRInstance(name = "Extractor", valueClass = MCRExtractor.class, required = false)
        public MCRExtractor<String> extractor;

        @MCRInstance(name = "Matcher", valueClass = MCRValueMatcher.class, required = false)
        public MCRValueMatcher<String> matcher;

        @MCRInstance(name = "PostProcessor", valueClass = MCRPostProcessor.class, required = false)
        public MCRPostProcessor<String> postProcessor;

        @MCRProperty(name = "Target")
        public String targetName;

        @Override
        public MCRConfigurableUserAttributePipelineMapping get() {
            return new MCRConfigurableUserAttributePipelineMapping(attributeName, extractor, matcher, postProcessor,
                targetName);
        }
    }
}
