package de.gbv.reposis.mapper.matcher;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

public class MCRStringRegexMatcherTest {

    @Test
    public void constructorRejectsNullRegex() {
        assertThrows(NullPointerException.class, () -> new MCRStringRegexMatcher(null));
    }

    @Test
    public void matchesReturnsTrueIfValueFullyTestPattern() {
        MCRStringRegexMatcher matcher = new MCRStringRegexMatcher(Pattern.compile("foo.*"));
        assertTrue(matcher.test("foobar"));
    }

    @Test
    public void matchesReturnsFalseIfOnlyPartOfValueTestPattern() {
        MCRStringRegexMatcher matcher = new MCRStringRegexMatcher(Pattern.compile("bar"));
        assertFalse(matcher.test("foobarbaz"));
    }

    @Test
    public void testReturnsFalseIfValueDoesNotMatchPattern() {
        MCRStringRegexMatcher matcher = new MCRStringRegexMatcher(Pattern.compile("foo"));
        assertFalse(matcher.test("bar"));
    }

    @Test
    public void testReturnsFalseIfActualValueIsNull() {
        MCRStringRegexMatcher matcher = new MCRStringRegexMatcher(Pattern.compile("foo"));
        assertFalse(matcher.test(null));
    }
}
