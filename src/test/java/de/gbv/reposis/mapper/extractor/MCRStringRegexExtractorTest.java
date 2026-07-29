package de.gbv.reposis.mapper.extractor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Optional;
import java.util.regex.Pattern;

import org.junit.Test;

public class MCRStringRegexExtractorTest {

    @Test
    public void shouldExtractCaptureGroup() {
        MCRStringRegexExtractor extractor = new MCRStringRegexExtractor(
            Pattern.compile(".*fg=(\\w+).*"), 1);
        assertEquals(Optional.of("fg2"), extractor.extract("abc fg=fg2 xyz"));
    }

    @Test
    public void shouldExtractWholeMatchForGroupZero() {
        MCRStringRegexExtractor extractor = new MCRStringRegexExtractor(
            Pattern.compile("fg=(\\w+)"), 0);
        assertEquals(Optional.empty(),
            extractor.extract("abc fg=fg2 xyz"));
    }

    @Test
    public void shouldReturnEmptyIfPatternDoesNotMatch() {
        MCRStringRegexExtractor extractor = new MCRStringRegexExtractor(
            Pattern.compile(".*fg=(\\w+).*"), 1);
        assertTrue(extractor.extract("no matching value").isEmpty());
    }

    @Test
    public void shouldRejectNullPattern() {
        assertThrows(NullPointerException.class,
            () -> new MCRStringRegexExtractor(null, 1));
    }

    @Test
    public void shouldRejectNegativeGroup() {
        assertThrows(IllegalArgumentException.class,
            () -> new MCRStringRegexExtractor(Pattern.compile(".*fg=(\\w+).*"), -1));
    }

    @Test
    public void shouldRejectGroupExceedingPatternGroupCount() {
        assertThrows(IllegalArgumentException.class,
            () -> new MCRStringRegexExtractor(Pattern.compile(".*fg=(\\w+).*"), 2));
    }
}
