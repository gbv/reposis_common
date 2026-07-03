package de.gbv.reposis.user.mapper.role;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstance;
import org.mycore.common.config.annotation.MCRProperty;

import de.gbv.reposis.user.postprocessor.MCRStringPostProcessor;

/**
 * A {@link MCRRoleMapping} that extracts a role from the value of a configured
 * source attribute using a regular expression.
 * <p>
 * The configured pattern must match the entire attribute value. The extracted
 * capture group is returned as the mapped role and may optionally be transformed
 * by a configured {@link MCRStringPostProcessor} before being returned.
 */
@MCRConfigurationProxy(proxyClass = MCRExtractRoleMapping.Factory.class)
public class MCRExtractRoleMapping implements MCRRoleMapping {

    private final String source;
    private final Pattern pattern;
    private final int group;
    private final MCRStringPostProcessor postProcessor;

    /**
     * Creates a new {@code MCRExtractRoleMapping}.
     *
     * @param source the name of the raw attribute whose values are searched
     * @param pattern the regular expression used to extract the role value
     * @param group the capture group containing the extracted value
     * @param postProcessor an optional post processor used to transform the extracted
     *                      value before it is returned as the mapped role; may be {@code null}
     */
    public MCRExtractRoleMapping(String source, String pattern, int group, MCRStringPostProcessor postProcessor) {
        this.source = Objects.requireNonNull(source, "source must not be null");
        this.pattern = Pattern.compile(Objects.requireNonNull(pattern, "pattern must not be null"));

        if (group < 0) {
            throw new IllegalArgumentException("group must be greater than or equal to 0");
        }

        this.group = group;
        this.postProcessor = postProcessor;
    }

    @Override
    public Optional<String> apply(Map<String, List<String>> attributes) {
        return attributes.getOrDefault(source, List.of())
            .stream()
            .map(this::extract)
            .flatMap(Optional::stream)
            .map(this::postProcess)
            .findFirst();
    }

    private Optional<String> extract(String value) {
        Matcher matcher = pattern.matcher(value);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        return Optional.of(matcher.group(group));
    }

    private String postProcess(String value) {
        return postProcessor != null ? postProcessor.process(value) : value;
    }

    /**
     * Factory for creating {@link MCRExtractRoleMapping} instances from configuration properties.
     */
    public static class Factory implements Supplier<MCRExtractRoleMapping> {

        @MCRProperty(name = "Source")
        public String source;

        @MCRProperty(name = "Pattern")
        public String pattern;

        @MCRProperty(name = "Group", required = false)
        public String groupString;

        @MCRInstance(name = "PostProcessor", valueClass = MCRStringPostProcessor.class, required = false)
        public MCRStringPostProcessor postProcessor;

        @Override
        public MCRExtractRoleMapping get() {
            int group = Optional.ofNullable(groupString).map(Integer::parseInt).orElse(1);
            return new MCRExtractRoleMapping(source, pattern, group, postProcessor);
        }
    }
}
