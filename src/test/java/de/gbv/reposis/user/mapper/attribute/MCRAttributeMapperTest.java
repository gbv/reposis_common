package de.gbv.reposis.user.mapper.attribute;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;

public class MCRAttributeMapperTest {

    @Test
    public void constructorRejectsNullMappings() {
        assertThrows(NullPointerException.class, () -> new MCRAttributeMapper(null));
    }

    @Test
    public void mapCombinesResultsOfAllMappings() {
        MCRAttributeMapper mapper = new MCRAttributeMapper(List.of(
            fixedMapping("realName", "Jane Doe"),
            fixedMapping("eMail", "jane@example.com")
        ));

        Map<String, String> result = mapper.map(Map.of());

        assertEquals(Map.of("realName", "Jane Doe", "eMail", "jane@example.com"), result);
    }

    @Test
    public void mapSkipsMappingsThatDoNotApply() {
        MCRAttributeMapper mapper = new MCRAttributeMapper(List.of(
            emptyMapping(),
            fixedMapping("realName", "Jane Doe")
        ));

        Map<String, String> result = mapper.map(Map.of());

        assertEquals(Map.of("realName", "Jane Doe"), result);
    }

    @Test
    public void mapReturnsEmptyResultIfNoMappingsAreConfigured() {
        MCRAttributeMapper mapper = new MCRAttributeMapper(List.of());

        Map<String, String> result = mapper.map(Map.of());

        assertTrue(result.isEmpty());
    }

    @Test
    public void mapUsesFirstValueIfMultipleMappingsProduceSameKey() {
        MCRAttributeMapper mapper = new MCRAttributeMapper(List.of(
            fixedMapping("realName", "First Value"),
            fixedMapping("realName", "Second Value")
        ));

        Map<String, String> result = mapper.map(Map.of());

        assertEquals("First Value", result.get("realName"));
    }

    @Test
    public void mappingsResultIsImmutable() {
        MCRAttributeMapper mapper = new MCRAttributeMapper(List.of(fixedMapping("realName", "Jane Doe")));
        Map<String, String> result = mapper.map(Map.of());

        assertThrows(UnsupportedOperationException.class, () -> result.put("x", "y"));
    }

    private static MCRAttributeMapping fixedMapping(String key, String value) {
        return attributes -> Optional.of(new MCRAttributeMapping.Result(key, value));
    }

    private static MCRAttributeMapping emptyMapping() {
        return attributes -> Optional.empty();
    }
}
