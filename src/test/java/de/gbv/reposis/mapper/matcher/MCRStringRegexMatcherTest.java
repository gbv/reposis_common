package de.gbv.reposis.mapper.matcher;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.regex.Pattern;

import org.junit.Test;

public class MCRStringRegexMatcherTest {

    @Test
    public void constructorRejectsNullRegex() {
        assertThrows(NullPointerException.class, () -> new MCRStringRegexMatcher(null));
    }

    @Test
    public void matchesReturnsTrueIfValueFullyMatchesPattern() {
        MCRStringRegexMatcher matcher = new MCRStringRegexMatcher(Pattern.compile("foo.*"));
        assertTrue(matcher.matches("foobar"));
    }

    @Test
    public void matchesReturnsFalseIfOnlyPartOfValueMatchesPattern() {
        MCRStringRegexMatcher matcher = new MCRStringRegexMatcher(Pattern.compile("bar"));
        assertFalse(matcher.matches("foobarbaz"));
    }

    @Test
    public void matchesReturnsFalseIfValueDoesNotMatchPattern() {
        MCRStringRegexMatcher matcher = new MCRStringRegexMatcher(Pattern.compile("foo"));
        assertFalse(matcher.matches("bar"));
    }

    @Test
    public void matchesReturnsFalseIfActualValueIsNull() {
        MCRStringRegexMatcher matcher = new MCRStringRegexMatcher(Pattern.compile("foo"));
        assertFalse(matcher.matches(null));
    }
}
