package de.gbv.reposis.mapper.postprocessor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class MCRStringPrefixPostProcessorTest {

    @Test
    public void shouldPrependPrefix() {
        MCRStringPrefixPostProcessor processor = new MCRStringPrefixPostProcessor("xxx:");
        String result = processor.apply("fg2");
        assertEquals("xxx:fg2", result);
    }

    @Test
    public void shouldHandleEmptyValue() {
        MCRStringPrefixPostProcessor processor = new MCRStringPrefixPostProcessor("xxx:");
        String result = processor.apply("");
        assertEquals("xxx:", result);
    }

    @Test
    public void shouldRejectNullPrefix() {
        assertThrows(NullPointerException.class,
            () -> new MCRStringPrefixPostProcessor(null));
    }
}
