package de.gbv.reposis.user.mapper.role;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.Test;

public class MCRRoleMapperTest {

    @Test
    public void constructorRejectsNullMappings() {
        assertThrows(NullPointerException.class, () -> new MCRRoleMapper(null));
    }

    @Test
    public void mapCombinesRolesOfAllMappings() {
        MCRRoleMapper mapper = new MCRRoleMapper(List.of(fixedRole("editor"), fixedRole("reviewer")));

        Set<String> roles = mapper.map(Map.of());

        assertEquals(Set.of("editor", "reviewer"), roles);
    }

    @Test
    public void mapSkipsMappingsThatDoNotApply() {
        MCRRoleMapper mapper = new MCRRoleMapper(List.of(noRole(), fixedRole("editor")));

        Set<String> roles = mapper.map(Map.of());

        assertEquals(Set.of("editor"), roles);
    }

    @Test
    public void mapReturnsEmptySetIfNoMappingsAreConfigured() {
        MCRRoleMapper mapper = new MCRRoleMapper(List.of());

        Set<String> roles = mapper.map(Map.of());

        assertTrue(roles.isEmpty());
    }

    @Test
    public void mapReturnsImmutableSet() {
        MCRRoleMapper mapper = new MCRRoleMapper(List.of(fixedRole("editor")));
        Set<String> roles = mapper.map(Map.of());

        assertThrows(UnsupportedOperationException.class, () -> roles.add("x"));
    }

    private static MCRRoleMapping fixedRole(String role) {
        return attributes -> Optional.of(role);
    }

    private static MCRRoleMapping noRole() {
        return attributes -> Optional.empty();
    }
}
