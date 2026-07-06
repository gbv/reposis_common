package de.gbv.reposis.mapper.postprocessor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class MCRStringPrefixPostProcessorTest {

    @Test
    public void shouldPrependPrefix() {
        MCRStringPrefixPostProcessor processor = new MCRStringPrefixPostProcessor("xxx:");
        String result = processor.process("fg2");
        assertEquals("xxx:fg2", result);
    }

    @Test
    public void shouldHandleEmptyValue() {
        MCRStringPrefixPostProcessor processor = new MCRStringPrefixPostProcessor("xxx:");
        String result = processor.process("");
        assertEquals("xxx:", result);
    }

    @Test
    public void shouldRejectNullPrefix() {
        assertThrows(NullPointerException.class,
            () -> new MCRStringPrefixPostProcessor(null));
    }
}
