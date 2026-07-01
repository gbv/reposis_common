package de.gbv.reposis.user.mapper.attribute;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;

public class MCRGenericAttributeMappingTest {

    @Test
    public void constructorRejectsNullSource() {
        assertThrows(NullPointerException.class, () -> new MCRGenericAttributeMapping(null, "target"));
    }

    @Test
    public void constructorRejectsNullTarget() {
        assertThrows(NullPointerException.class, () -> new MCRGenericAttributeMapping("source", null));
    }

    @Test
    public void applyReturnsResultWithFirstValueIfSourceAttributeIsPresent() {
        MCRGenericAttributeMapping mapping = new MCRGenericAttributeMapping("mail", "eMail");
        Map<String, List<String>> attributes = Map.of("mail", List.of("test@example.com", "other@example.com"));

        Optional<MCRAttributeMapping.Result> result = mapping.apply(attributes);

        assertTrue(result.isPresent());
        assertEquals("eMail", result.get().key());
        assertEquals("test@example.com", result.get().value());
    }

    @Test
    public void applyReturnsEmptyIfSourceAttributeIsAbsent() {
        MCRGenericAttributeMapping mapping = new MCRGenericAttributeMapping("mail", "eMail");
        Map<String, List<String>> attributes = Map.of("displayName", List.of("Jane Doe"));

        Optional<MCRAttributeMapping.Result> result = mapping.apply(attributes);

        assertTrue(result.isEmpty());
    }

    @Test
    public void applyReturnsEmptyIfSourceAttributeIsEmptyList() {
        MCRGenericAttributeMapping mapping = new MCRGenericAttributeMapping("mail", "eMail");
        Map<String, List<String>> attributes = Map.of("mail", List.of());

        Optional<MCRAttributeMapping.Result> result = mapping.apply(attributes);

        assertTrue(result.isEmpty());
    }
}
