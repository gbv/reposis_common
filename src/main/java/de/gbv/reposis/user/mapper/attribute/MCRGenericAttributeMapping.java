package de.gbv.reposis.user.mapper.attribute;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;

/**
 * A generic {@link MCRAttributeMapping} that maps a single source attribute to a target name.
 * <p>
 * If the source attribute is present and has at least one value, the first value is used as the
 * result value. Otherwise, this mapping does not apply.
 */
@MCRConfigurationProxy(proxyClass = MCRGenericAttributeMapping.Factory.class)
public class MCRGenericAttributeMapping implements MCRAttributeMapping {

    private final String source;
    private final String target;

    /**
     * Creates a new {@code MCRGenericAttributeMapping}.
     *
     * @param source the name of the raw attribute to read the value from
     * @param target the resulting attribute name
     */
    public MCRGenericAttributeMapping(String source, String target) {
        this.source = Objects.requireNonNull(source, "source must not be null");
        this.target = Objects.requireNonNull(target, "target must not be null");
    }

    @Override
    public Optional<Result> apply(Map<String, List<String>> attributes) {
        return Optional.ofNullable(attributes.get(source)).filter(v -> !v.isEmpty())
            .map(v -> new Result(target, v.get(0)));
    }

    /**
     * Factory for creating {@link MCRGenericAttributeMapping} instances from configuration properties.
     */
    public static class Factory implements Supplier<MCRGenericAttributeMapping> {

        @MCRProperty(name = "Source")
        public String source;

        @MCRProperty(name = "Target")
        public String target;

        @Override
        public MCRGenericAttributeMapping get() {
            return new MCRGenericAttributeMapping(source, target);
        }
    }
}
