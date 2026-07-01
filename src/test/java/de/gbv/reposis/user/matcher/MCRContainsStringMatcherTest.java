package de.gbv.reposis.user.matcher;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MCRContainsStringMatcherTest {

    @Test
    public void constructorRejectsNullSubstring() {
        assertThrows(NullPointerException.class, () -> new MCRContainsStringMatcher(null));
    }

    @Test
    public void matchesReturnsTrueIfValueContainsSubstring() {
        MCRContainsStringMatcher matcher = new MCRContainsStringMatcher("bar");
        assertTrue(matcher.matches("foobarbaz"));
    }

    @Test
    public void matchesReturnsTrueIfValueEqualsSubstring() {
        MCRContainsStringMatcher matcher = new MCRContainsStringMatcher("bar");
        assertTrue(matcher.matches("bar"));
    }

    @Test
    public void matchesReturnsFalseIfValueDoesNotContainSubstring() {
        MCRContainsStringMatcher matcher = new MCRContainsStringMatcher("bar");
        assertFalse(matcher.matches("foobaz"));
    }

    @Test
    public void matchesReturnsFalseIfActualValueIsNull() {
        MCRContainsStringMatcher matcher = new MCRContainsStringMatcher("bar");
        assertFalse(matcher.matches(null));
    }

    @Test
    public void matchesWithEmptySubstringMatchesAnyNonNullValue() {
        MCRContainsStringMatcher matcher = new MCRContainsStringMatcher("");
        assertTrue(matcher.matches("anything"));
    }

    @Test
    public void matchesIsCaseSensitive() {
        MCRContainsStringMatcher matcher = new MCRContainsStringMatcher("Bar");
        assertFalse(matcher.matches("foobarbaz"));
    }
}
