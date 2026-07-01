package de.gbv.reposis.user.mapper.attribute;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstanceList;

/**
 * Maps raw attributes to a flat set of name/value pairs based on a configured list of
 * {@link MCRAttributeMapping} instances.
 * <p>
 * Each configured mapping is applied independently to the given raw attributes.
 * Mappings that do not produce a result (i.e. return {@link Optional#empty()}) are skipped.
 * The results of all mappings are combined into a single {@link MappingsResult}.
 */
@MCRConfigurationProxy(proxyClass = MCRAttributeMapper.Factory.class)
public class MCRAttributeMapper {

    private final List<MCRAttributeMapping> attributeMappings;

    /**
     * Creates a new {@code MCRAttributeMapper}.
     *
     * @param attributeMappings the list of attribute mappings to apply, in order
     */
    public MCRAttributeMapper(List<MCRAttributeMapping> attributeMappings) {
        this.attributeMappings = Objects.requireNonNull(attributeMappings, "attributeMappings must not be null");
    }

    /**
     * Applies all configured attribute mappings to the given raw attributes and aggregates
     * the results.
     * <p>
     * If two or more mappings produce the same result key, the value from the first matching
     * mapping (in configuration order) is used; subsequent values for that key are ignored.
     *
     * @param attributes the raw attributes, keyed by attribute name, with each value being the
     *                   (possibly multi-valued) list of values for that attribute
     * @return the aggregated result of all applied mappings
     */
    public MappingsResult map(Map<String, List<String>> attributes) {
        Map<String, String> userAttributes = attributeMappings.stream()
            .map(m -> m.apply(attributes))
            .flatMap(Optional::stream)
            .collect(Collectors.toMap(
                MCRAttributeMapping.Result::key,
                MCRAttributeMapping.Result::value,
                (first, second) -> first
            ));
        return new MappingsResult(userAttributes);
    }

    /**
     * Factory for creating {@link MCRAttributeMapper} instances from configuration properties.
     */
    public static class Factory implements Supplier<MCRAttributeMapper> {

        @MCRInstanceList(name = "Attributes", valueClass = MCRAttributeMapping.class, required = false)
        public List<MCRAttributeMapping> attributes;

        @Override
        public MCRAttributeMapper get() {
            return new MCRAttributeMapper(attributes != null ? attributes : List.of());
        }
    }

    /**
     * The aggregated result of applying all configured attribute mappings.
     *
     * @param userAttributes the resulting attributes, keyed by attribute name
     */
    public record MappingsResult(Map<String, String> userAttributes) {
        public MappingsResult {
            userAttributes = Map.copyOf(userAttributes);
        }
    }
}
