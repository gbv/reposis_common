package de.gbv.reposis.user.mapper.role;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.Test;

import de.gbv.reposis.mapper.source.MCRMapValueSource;
import de.gbv.reposis.mapper.source.MCRValueSource;

public class MCRRoleMapperTest {

    private static final MCRValueSource<String> EMPTY_SOURCE = new MCRMapValueSource<>(Map.of());

    @Test
    public void constructorRejectsNullMappings() {
        assertThrows(NullPointerException.class, () -> new MCRRoleMapper(null));
    }

    @Test
    public void mapCombinesRolesOfAllMappings() {
        MCRRoleMapper mapper = new MCRRoleMapper(List.of(fixedRole("editor"), fixedRole("reviewer")));

        Set<String> roles = mapper.map(EMPTY_SOURCE);

        assertEquals(Set.of("editor", "reviewer"), roles);
    }

    @Test
    public void mapSkipsMappingsThatDoNotApply() {
        MCRRoleMapper mapper = new MCRRoleMapper(List.of(noRole(), fixedRole("editor")));

        Set<String> roles = mapper.map(EMPTY_SOURCE);

        assertEquals(Set.of("editor"), roles);
    }

    @Test
    public void mapReturnsEmptySetIfNoMappingsAreConfigured() {
        MCRRoleMapper mapper = new MCRRoleMapper(List.of());

        Set<String> roles = mapper.map(EMPTY_SOURCE);

        assertTrue(roles.isEmpty());
    }

    @Test
    public void mapReturnsImmutableSet() {
        MCRRoleMapper mapper = new MCRRoleMapper(List.of(fixedRole("editor")));
        Set<String> roles = mapper.map(EMPTY_SOURCE);

        assertThrows(UnsupportedOperationException.class, () -> roles.add("x"));
    }

    private static MCRRoleMapping fixedRole(String role) {
        return source -> Optional.of(role);
    }

    private static MCRRoleMapping noRole() {
        return source -> Optional.empty();
    }
}
