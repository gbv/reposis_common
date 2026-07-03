package de.gbv.reposis.user.mapper.role;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;

import de.gbv.reposis.user.postprocessor.MCRPrefixStringPostProcessor;

public class MCRExtractRoleMappingTest {

    @Test
    public void shouldExtractRole() {
        MCRExtractRoleMapping mapping = new MCRExtractRoleMapping(
            "description",
            ".*fg=(\\w+).*",
            1,
            null);
        Map<String, List<String>> attributes = Map.of("description", List.of("abc fg=fg2 xyz"));
        assertEquals(Optional.of("fg2"), mapping.apply(attributes));
    }

    @Test
    public void shouldApplyPostProcessor() {
        MCRExtractRoleMapping mapping = new MCRExtractRoleMapping(
            "description",
            ".*fg=(\\w+).*",
            1,
            new MCRPrefixStringPostProcessor("xxx:"));
        Map<String, List<String>> attributes = Map.of("description", List.of("abc fg=fg2 xyz"));
        assertEquals(Optional.of("xxx:fg2"), mapping.apply(attributes));
    }

    @Test
    public void shouldReturnEmptyIfPatternDoesNotMatch() {
        MCRExtractRoleMapping mapping = new MCRExtractRoleMapping(
            "description",
            ".*fg=(\\w+).*",
            1,
            null);
        Map<String, List<String>> attributes = Map.of("description", List.of("no matching value"));
        assertTrue(mapping.apply(attributes).isEmpty());
    }

    @Test
    public void shouldReturnEmptyIfSourceAttributeIsMissing() {
        MCRExtractRoleMapping mapping = new MCRExtractRoleMapping(
            "description",
            ".*fg=(\\w+).*",
            1,
            null);
        assertTrue(mapping.apply(Map.of()).isEmpty());
    }

    @Test
    public void shouldReturnFirstMatchingRole() {
        MCRExtractRoleMapping mapping = new MCRExtractRoleMapping(
            "description",
            ".*fg=(\\w+).*",
            1,
            null);
        Map<String, List<String>> attributes = Map.of("description", List.of("foo", "fg=fg2", "fg=fg3"));
        assertEquals(Optional.of("fg2"), mapping.apply(attributes));
    }

    @Test
    public void shouldRejectNullSource() {
        assertThrows(NullPointerException.class,
            () -> new MCRExtractRoleMapping(
                null,
                ".*fg=(\\w+).*",
                1,
                null));
    }

    @Test
    public void shouldRejectNullPattern() {
        assertThrows(NullPointerException.class,
            () -> new MCRExtractRoleMapping(
                "description",
                null,
                1,
                null));
    }

    @Test
    public void shouldRejectNegativeGroup() {
        assertThrows(IllegalArgumentException.class,
            () -> new MCRExtractRoleMapping(
                "description",
                ".*fg=(\\w+).*",
                -1,
                null));
    }
}
