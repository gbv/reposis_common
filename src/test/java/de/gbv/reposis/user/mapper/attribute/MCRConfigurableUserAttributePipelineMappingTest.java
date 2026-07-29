package de.gbv.reposis.user.mapper.attribute;

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
import de.gbv.reposis.mapper.postprocessor.MCRStringPrefixPostProcessor;
import de.gbv.reposis.mapper.source.MCRMapValueSource;

public class MCRConfigurableUserAttributePipelineMappingTest {

    @Test
    public void constructorRejectsNullProvider() {
        assertThrows(NullPointerException.class,
            () -> new MCRConfigurableUserAttributePipelineMapping(null, null, null, null, "eMail"));
    }

    @Test
    public void constructorRejectsNullTarget() {
        assertThrows(NullPointerException.class,
            () -> new MCRConfigurableUserAttributePipelineMapping(
                "mail", null, null, null, null));
    }

    @Test
    public void applyReturnsResultWithFirstValueIfSourceAttributeIsPresent() {
        MCRConfigurableUserAttributePipelineMapping mapping = new MCRConfigurableUserAttributePipelineMapping(
            "mail", null, null, null, "eMail");
        Map<String, List<String>> attributes = Map.of("mail", List.of("test@example.com", "other@example.com"));

        Optional<Map.Entry<String, String>> result = mapping.apply(new MCRMapValueSource<>(attributes));

        assertTrue(result.isPresent());
        assertEquals("eMail", result.get().getKey());
        assertEquals("test@example.com", result.get().getValue());
    }

    @Test
    public void applyReturnsEmptyIfSourceAttributeIsAbsent() {
        MCRConfigurableUserAttributePipelineMapping mapping = new MCRConfigurableUserAttributePipelineMapping(
            "mail", null, null, null, "eMail");
        Map<String, List<String>> attributes = Map.of("displayName", List.of("Jane Doe"));

        assertTrue(mapping.apply(new MCRMapValueSource<>(attributes)).isEmpty());
    }

    @Test
    public void applyReturnsEmptyIfSourceAttributeIsEmptyList() {
        MCRConfigurableUserAttributePipelineMapping mapping = new MCRConfigurableUserAttributePipelineMapping(
            "mail", null, null, null, "eMail");
        Map<String, List<String>> attributes = Map.of("mail", List.of());

        assertTrue(mapping.apply(new MCRMapValueSource<>(attributes)).isEmpty());
    }

    @Test
    public void applyUsesExtractorIfConfigured() {
        MCRConfigurableUserAttributePipelineMapping mapping = new MCRConfigurableUserAttributePipelineMapping(
            "description", new MCRStringRegexExtractor(Pattern.compile(".*fg=(\\w+).*"), 1),
            null, null, "fg");
        Map<String, List<String>> attributes = Map.of("description", List.of("abc fg=fg2 xyz"));

        Optional<Map.Entry<String, String>> result = mapping.apply(new MCRMapValueSource<>(attributes));

        assertTrue(result.isPresent());
        assertEquals("fg", result.get().getKey());
        assertEquals("fg2", result.get().getValue());
    }

    @Test
    public void applyReturnsEmptyIfMatcherDoesNotMatch() {
        MCRConfigurableUserAttributePipelineMapping mapping = new MCRConfigurableUserAttributePipelineMapping(
            "role", null, new MCRStringEqualsMatcher("admin"), null, "role");
        Map<String, List<String>> attributes = Map.of("role", List.of("editor"));

        assertTrue(mapping.apply(new MCRMapValueSource<>(attributes)).isEmpty());
    }

    @Test
    public void applyReturnsResultIfMatcherMatches() {
        MCRConfigurableUserAttributePipelineMapping mapping = new MCRConfigurableUserAttributePipelineMapping(
            "role", null, new MCRStringEqualsMatcher("admin"), null, "role");
        Map<String, List<String>> attributes = Map.of("role", List.of("admin"));

        Optional<Map.Entry<String, String>> result = mapping.apply(new MCRMapValueSource<>(attributes));

        assertTrue(result.isPresent());
        assertEquals("admin", result.get().getValue());
    }

    @Test
    public void applyAppliesPostProcessorToMatchingValue() {
        MCRConfigurableUserAttributePipelineMapping mapping = new MCRConfigurableUserAttributePipelineMapping(
            "mail", null, null, new MCRStringPrefixPostProcessor("xxx:"), "eMail");
        Map<String, List<String>> attributes = Map.of("mail", List.of("test@example.com"));

        Optional<Map.Entry<String, String>> result = mapping.apply(new MCRMapValueSource<>(attributes));

        assertTrue(result.isPresent());
        assertEquals("xxx:test@example.com", result.get().getValue());
    }
}
