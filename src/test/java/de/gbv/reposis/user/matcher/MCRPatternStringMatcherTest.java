package de.gbv.reposis.user.matcher;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.regex.PatternSyntaxException;

import org.junit.Test;

public class MCRPatternStringMatcherTest {

    @Test
    public void constructorRejectsNullRegex() {
        assertThrows(NullPointerException.class, () -> new MCRPatternStringMatcher(null));
    }

    @Test
    public void constructorRejectsInvalidRegex() {
        assertThrows(PatternSyntaxException.class, () -> new MCRPatternStringMatcher("["));
    }

    @Test
    public void matchesReturnsTrueIfValueFullyMatchesPattern() {
        MCRPatternStringMatcher matcher = new MCRPatternStringMatcher("foo.*");
        assertTrue(matcher.matches("foobar"));
    }

    @Test
    public void matchesReturnsFalseIfOnlyPartOfValueMatchesPattern() {
        MCRPatternStringMatcher matcher = new MCRPatternStringMatcher("bar");
        assertFalse(matcher.matches("foobarbaz"));
    }

    @Test
    public void matchesReturnsFalseIfValueDoesNotMatchPattern() {
        MCRPatternStringMatcher matcher = new MCRPatternStringMatcher("foo");
        assertFalse(matcher.matches("bar"));
    }

    @Test
    public void matchesReturnsFalseIfActualValueIsNull() {
        MCRPatternStringMatcher matcher = new MCRPatternStringMatcher("foo");
        assertFalse(matcher.matches(null));
    }
}
