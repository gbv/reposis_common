package de.gbv.reposis.user.postprocessor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class MCRPrefixStringPostProcessorTest {

    @Test
    public void shouldPrependPrefix() {
        MCRPrefixStringPostProcessor processor = new MCRPrefixStringPostProcessor("xxx:");
        String result = processor.process("fg2");
        assertEquals("xxx:fg2", result);
    }

    @Test
    public void shouldHandleEmptyValue() {
        MCRPrefixStringPostProcessor processor = new MCRPrefixStringPostProcessor("xxx:");
        String result = processor.process("");
        assertEquals("xxx:", result);
    }

    @Test
    public void shouldRejectNullPrefix() {
        assertThrows(NullPointerException.class,
            () -> new MCRPrefixStringPostProcessor(null));
    }
}
