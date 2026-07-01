package de.gbv.reposis.user.matcher;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MCREqualsStringMatcherTest {

    @Test
    public void constructorRejectsNullExpectedValue() {
        assertThrows(NullPointerException.class, () -> new MCREqualsStringMatcher(null));
    }

    @Test
    public void matchesReturnsTrueIfValueEqualsExpectedValue() {
        MCREqualsStringMatcher matcher = new MCREqualsStringMatcher("foo");
        assertTrue(matcher.matches("foo"));
    }

    @Test
    public void matchesReturnsFalseIfValueDiffersFromExpectedValue() {
        MCREqualsStringMatcher matcher = new MCREqualsStringMatcher("foo");
        assertFalse(matcher.matches("bar"));
    }

    @Test
    public void matchesIsCaseSensitive() {
        MCREqualsStringMatcher matcher = new MCREqualsStringMatcher("foo");
        assertFalse(matcher.matches("FOO"));
    }

    @Test
    public void matchesReturnsFalseIfActualValueIsNull() {
        MCREqualsStringMatcher matcher = new MCREqualsStringMatcher("foo");
        assertFalse(matcher.matches(null));
    }
}
