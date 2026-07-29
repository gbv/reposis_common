package de.gbv.reposis.user.mapper.role;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.junit.Test;

import de.gbv.reposis.mapper.extractor.MCRStringRegexExtractor;
import de.gbv.reposis.mapper.matcher.MCRStringEqualsMatcher;
import de.gbv.reposis.mapper.matcher.MCRValueMatcher;
import de.gbv.reposis.mapper.postprocessor.MCRStringFixedValuePostProcessor;
import de.gbv.reposis.mapper.source.MCRMapValueSource;

public class MCRConfigurableRolePipelineMappingTest {

    private static final MCRValueMatcher<String> ALWAYS_MATCHES = value -> true;
    private static final MCRValueMatcher<String> NEVER_MATCHES = value -> false;

    @Test
    public void constructorRejectsNullProvider() {
        assertThrows(NullPointerException.class,
            () -> new MCRConfigurableRolePipelineMapping(null, null, ALWAYS_MATCHES, null));
    }

    @Test
    public void applyReturnsFirstValueIfMatcherMatchesAndNoPostProcessorConfigured() {
        MCRConfigurableRolePipelineMapping mapping = new MCRConfigurableRolePipelineMapping(
            "memberOf", null, ALWAYS_MATCHES, null);
        Map<String, List<String>> attributes = Map.of("memberOf", List.of("group1", "group2"));

        Optional<String> result = mapping.apply(new MCRMapValueSource<>(attributes));

        assertTrue(result.isPresent());
        assertEquals("group1", result.get());
    }

    @Test
    public void applyReturnsFixedRoleIfPostProcessorConfigured() {
        MCRConfigurableRolePipelineMapping mapping =
            new MCRConfigurableRolePipelineMapping("memberOf", null, ALWAYS_MATCHES,
                new MCRStringFixedValuePostProcessor("admin"));
        Map<String, List<String>> attributes = Map.of("memberOf", List.of("group1", "group2"));

        Optional<String> result = mapping.apply(new MCRMapValueSource<>(attributes));

        assertTrue(result.isPresent());
        assertEquals("admin", result.get());
    }

    @Test
    public void applyReturnsEmptyIfNoValueMatches() {
        MCRConfigurableRolePipelineMapping mapping = new MCRConfigurableRolePipelineMapping(
            "memberOf", null, NEVER_MATCHES, new MCRStringFixedValuePostProcessor("admin"));
        Map<String, List<String>> attributes = Map.of("memberOf", List.of("group1", "group2"));

        assertTrue(mapping.apply(new MCRMapValueSource<>(attributes)).isEmpty());
    }

    @Test
    public void applyReturnsEmptyIfSourceAttributeIsAbsent() {
        MCRConfigurableRolePipelineMapping mapping = new MCRConfigurableRolePipelineMapping(
            "memberOf", null, ALWAYS_MATCHES, new MCRStringFixedValuePostProcessor("admin"));
        Map<String, List<String>> attributes = Map.of("displayName", List.of("Jane Doe"));

        assertTrue(mapping.apply(new MCRMapValueSource<>(attributes)).isEmpty());
    }

    @Test
    public void applyOnlyChecksValuesOfConfiguredSourceAttribute() {
        MCRConfigurableRolePipelineMapping mapping =
            new MCRConfigurableRolePipelineMapping("memberOf", null, value -> value.equals("admins"),
                new MCRStringFixedValuePostProcessor("admin"));
        Map<String, List<String>> attributes =
            Map.of("memberOf", List.of("users"), "otherAttribute", List.of("admins"));

        assertTrue(mapping.apply(new MCRMapValueSource<>(attributes)).isEmpty());
    }

    @Test
    public void applyUsesExtractorIfConfigured() {
        MCRConfigurableRolePipelineMapping mapping = new MCRConfigurableRolePipelineMapping("description",
            new MCRStringRegexExtractor(Pattern.compile(".*fg=(\\w+).*"), 1),
            new MCRStringEqualsMatcher("fg2"), null);
        Map<String, List<String>> attributes = Map.of("description", List.of("abc fg=fg2 xyz"));

        Optional<String> result = mapping.apply(new MCRMapValueSource<>(attributes));

        assertTrue(result.isPresent());
        assertEquals("fg2", result.get());
    }
}
