package de.gbv.reposis.user.mapper.attribute;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstanceList;

import de.gbv.reposis.mapper.source.MCRValueSource;

/**
 * Maps raw attributes to a flat set of name/value pairs based on a configured list of
 * {@link MCRUserMapping} instances.
 *
 * @see MCRUserMapping
 */
@MCRConfigurationProxy(proxyClass = MCRUserAttributeMapper.Factory.class)
public class MCRUserAttributeMapper {

    private final List<MCRUserMapping> attributeMappings;

    /**
     * Creates a new {@code MCRUserAttributeMapper}.
     *
     * @param attributeMappings the list of attribute mappings to apply, in order
     */
    public MCRUserAttributeMapper(List<MCRUserMapping> attributeMappings) {
        this.attributeMappings = Objects.requireNonNull(attributeMappings, "attributeMappings must not be null");
    }

    /**
     * Applies all configured attribute mappings to the given attribute source and aggregates
     * the results.
     *
     * @param source the source providing the raw attribute values
     * @return an immutable map of user attributes derived from the given source
     * @throws IllegalStateException if multiple attribute mappings produce the same target key
     */
    public Map<String, String> map(MCRValueSource<String> source) {
        return attributeMappings.stream()
            .map(m -> m.apply(source))
            .flatMap(Optional::stream)
            .collect(Collectors.toUnmodifiableMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (first, second) -> {
                    throw new IllegalStateException(
                        "Multiple attribute mappings produced the same target key");
                }
            ));
    }

    /**
     * Factory for creating {@link MCRUserAttributeMapper} instances from configuration properties.
     */
    public static class Factory implements Supplier<MCRUserAttributeMapper> {

        @MCRInstanceList(name = "Attributes", valueClass = MCRUserMapping.class, required = false)
        public List<MCRUserMapping> attributes;

        @Override
        public MCRUserAttributeMapper get() {
            return new MCRUserAttributeMapper(Optional.ofNullable(attributes).orElse(List.of()));
        }
    }
}
