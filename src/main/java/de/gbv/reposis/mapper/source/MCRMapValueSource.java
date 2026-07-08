package de.gbv.reposis.mapper.source;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A {@link MCRValueSource} backed by an already fully-collected {@link Map}, e.g. the
 * attributes returned from an LDAP search. The given map and its value lists are copied
 * defensively, so subsequent changes to the original data have no effect on this source.
 *
 * @param <V> the type of the raw attribute values
 */
public class MCRMapValueSource<V> implements MCRValueSource<V> {

    private final Map<String, List<V>> attributes;

    /**
     * Creates a new {@code MCRMapValueSource}.
     *
     * @param attributes the raw attributes, keyed by attribute name, with each value being the
     *                   (possibly multivalued) list of values for that attribute
     */
    public MCRMapValueSource(Map<String, List<V>> attributes) {
        Objects.requireNonNull(attributes, "attributes must not be null");
        this.attributes = attributes.entrySet().stream()
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> List.copyOf(entry.getValue())));
    }

    /**
     * Returns the values for the given attribute name from the underlying map.
     *
     * @param name the attribute name
     * @return the raw values, or an empty {@link Optional} if the attribute is absent or has
     *         no values
     */
    @Override
    public Optional<List<V>> getValues(String name) {
        return Optional.ofNullable(attributes.get(name)).filter(v -> !v.isEmpty());
    }
}
