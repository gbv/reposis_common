package de.gbv.reposis.user.mapper.role;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstance;
import org.mycore.common.config.annotation.MCRProperty;

import de.gbv.reposis.mapper.MCRMapping;
import de.gbv.reposis.mapper.MCRPipelineMapping;
import de.gbv.reposis.mapper.extractor.MCRExtractor;
import de.gbv.reposis.mapper.matcher.MCRMatcher;
import de.gbv.reposis.mapper.postprocessor.MCRPostProcessor;
import de.gbv.reposis.mapper.source.MCRValueSource;

/**
 * A configurable {@link MCRRoleMapping} that combines a source attribute, an optional
 * extractor, an optional matcher, and an optional post-processor into the resulting role.
 */
@MCRConfigurationProxy(proxyClass = MCRConfigurableRolePipelineMapping.Factory.class)
public class MCRConfigurableRolePipelineMapping implements MCRRoleMapping {

    private final MCRMapping<String, String> pipeline;

    /**
     * Creates a new {@code MCRConfigurableRolePipelineMapping}.
     *
     * @param attributeName the name of the attribute to read the raw value(s) from
     * @param extractor the extractor that derives a value from the raw values, or {@code null}
     *                  if the raw value should be used as-is
     * @param matcher the matcher that decides whether the extracted value matches, or
     *                {@code null} if every extracted value should be considered a match
     * @param postProcessor the post-processor that further transforms a matching value, or
     *                      {@code null} if no further processing is needed
     */
    public MCRConfigurableRolePipelineMapping(String attributeName, MCRExtractor<String> extractor,
        MCRMatcher<String> matcher, MCRPostProcessor<String> postProcessor) {
        Objects.requireNonNull(attributeName, "attributeName must not be null");
        this.pipeline = new MCRPipelineMapping<>(attributeName, extractor, matcher, postProcessor);
    }

    /**
     * Applies the pipeline to the given attribute source to derive the resulting role.
     *
     * @param source the source providing the raw attribute values
     * @return the resulting role, or an empty {@link Optional} if the pipeline produces no result
     */
    @Override
    public Optional<String> apply(MCRValueSource<String> source) {
        return pipeline.apply(source);
    }

    /**
     * Factory for creating {@link MCRConfigurableRolePipelineMapping} instances from configuration properties.
     */
    public static class Factory implements Supplier<MCRConfigurableRolePipelineMapping> {

        @MCRProperty(name = "Source")
        public String attributeName;

        @MCRInstance(name = "Extractor", valueClass = MCRExtractor.class, required = false)
        public MCRExtractor<String> extractor;

        @MCRInstance(name = "Matcher", valueClass = MCRMatcher.class, required = false)
        public MCRMatcher<String> matcher;

        @MCRInstance(name = "PostProcessor", valueClass = MCRPostProcessor.class, required = false)
        public MCRPostProcessor<String> postProcessor;

        @Override
        public MCRConfigurableRolePipelineMapping get() {
            return new MCRConfigurableRolePipelineMapping(attributeName, extractor, matcher, postProcessor);
        }
    }
}
