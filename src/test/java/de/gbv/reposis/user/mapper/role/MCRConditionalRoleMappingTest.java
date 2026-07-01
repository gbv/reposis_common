package de.gbv.reposis.user.mapper.role;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;

import de.gbv.reposis.user.matcher.MCRStringMatcher;

public class MCRConditionalRoleMappingTest {

    private static final MCRStringMatcher ALWAYS_MATCHES = actualValue -> true;
    private static final MCRStringMatcher NEVER_MATCHES = actualValue -> false;

    @Test
    public void constructorRejectsNullSource() {
        assertThrows(NullPointerException.class,
            () -> new MCRConditionalRoleMapping(null, ALWAYS_MATCHES, "role"));
    }

    @Test
    public void constructorRejectsNullMatcher() {
        assertThrows(NullPointerException.class,
            () -> new MCRConditionalRoleMapping("source", null, "role"));
    }

    @Test
    public void constructorRejectsNullRole() {
        assertThrows(NullPointerException.class,
            () -> new MCRConditionalRoleMapping("source", ALWAYS_MATCHES, null));
    }

    @Test
    public void applyReturnsRoleIfAnyValueMatches() {
        MCRConditionalRoleMapping mapping = new MCRConditionalRoleMapping("memberOf", ALWAYS_MATCHES, "admin");
        Map<String, List<String>> attributes = Map.of("memberOf", List.of("group1", "group2"));

        Optional<String> result = mapping.apply(attributes);

        assertTrue(result.isPresent());
        assertEquals("admin", result.get());
    }

    @Test
    public void applyReturnsEmptyIfNoValueMatches() {
        MCRConditionalRoleMapping mapping = new MCRConditionalRoleMapping("memberOf", NEVER_MATCHES, "admin");
        Map<String, List<String>> attributes = Map.of("memberOf", List.of("group1", "group2"));

        Optional<String> result = mapping.apply(attributes);

        assertTrue(result.isEmpty());
    }

    @Test
    public void applyReturnsEmptyIfSourceAttributeIsAbsent() {
        MCRConditionalRoleMapping mapping = new MCRConditionalRoleMapping("memberOf", ALWAYS_MATCHES, "admin");
        Map<String, List<String>> attributes = Map.of("displayName", List.of("Jane Doe"));

        Optional<String> result = mapping.apply(attributes);

        assertTrue(result.isEmpty());
    }

    @Test
    public void applyOnlyChecksValuesOfConfiguredSourceAttribute() {
        MCRConditionalRoleMapping mapping =
            new MCRConditionalRoleMapping("memberOf", value -> value.equals("admins"), "admin");
        Map<String, List<String>> attributes =
            Map.of("memberOf", List.of("users"), "otherAttribute", List.of("admins"));

        Optional<String> result = mapping.apply(attributes);

        assertTrue(result.isEmpty());
    }
}
