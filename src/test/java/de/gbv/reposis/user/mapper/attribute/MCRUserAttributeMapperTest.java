package de.gbv.reposis.user.mapper.attribute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import de.gbv.reposis.mapper.source.MCRMapValueSource;
import de.gbv.reposis.mapper.source.MCRValueSource;

public class MCRUserAttributeMapperTest {

    private static final MCRValueSource<String> EMPTY_SOURCE = new MCRMapValueSource<>(Map.of());

    @Test
    public void constructorRejectsNullMappings() {
        assertThrows(NullPointerException.class, () -> new MCRUserAttributeMapper(null));
    }

    @Test
    public void mapCombinesResultsOfAllMappings() {
        MCRUserAttributeMapper mapper = new MCRUserAttributeMapper(List.of(
            fixedMapping("realName", "Jane Doe"),
            fixedMapping("eMail", "jane@example.com")
        ));

        Map<String, String> result = mapper.map(EMPTY_SOURCE);

        assertEquals(Map.of("realName", "Jane Doe", "eMail", "jane@example.com"), result);
    }

    @Test
    public void mapSkipsMappingsThatDoNotApply() {
        MCRUserAttributeMapper mapper = new MCRUserAttributeMapper(List.of(
            emptyMapping(),
            fixedMapping("realName", "Jane Doe")
        ));

        Map<String, String> result = mapper.map(EMPTY_SOURCE);

        assertEquals(Map.of("realName", "Jane Doe"), result);
    }

    @Test
    public void mapReturnsEmptyResultIfNoMappingsAreConfigured() {
        MCRUserAttributeMapper mapper = new MCRUserAttributeMapper(List.of());

        Map<String, String> result = mapper.map(EMPTY_SOURCE);

        assertEquals(Map.of(), result);
    }

    @Test
    public void mapThrowsIfMultipleMappingsProduceSameKey() {
        MCRUserAttributeMapper mapper = new MCRUserAttributeMapper(List.of(
            fixedMapping("realName", "First Value"),
            fixedMapping("realName", "Second Value")
        ));

        assertThrows(IllegalStateException.class, () -> mapper.map(EMPTY_SOURCE));
    }

    @Test
    public void mappingsResultIsImmutable() {
        MCRUserAttributeMapper mapper = new MCRUserAttributeMapper(List.of(fixedMapping("realName", "Jane Doe")));
        Map<String, String> result = mapper.map(EMPTY_SOURCE);

        assertThrows(UnsupportedOperationException.class, () -> result.put("x", "y"));
    }

    private static MCRUserMapping fixedMapping(String key, String value) {
        return source -> Optional.of(Map.entry(key, value));
    }

    private static MCRUserMapping emptyMapping() {
        return source -> Optional.empty();
    }
}
