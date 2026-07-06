package de.gbv.reposis.mapper.matcher;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MCRStringContainsMatcherTest {

    @Test
    public void constructorRejectsNullSubstring() {
        assertThrows(NullPointerException.class, () -> new MCRStringContainsMatcher(null));
    }

    @Test
    public void matchesReturnsTrueIfValueContainsSubstring() {
        MCRStringContainsMatcher
            matcher = new MCRStringContainsMatcher("bar");
        assertTrue(matcher.matches("foobarbaz"));
    }

    @Test
    public void matchesReturnsTrueIfValueEqualsSubstring() {
        MCRStringContainsMatcher
            matcher = new MCRStringContainsMatcher("bar");
        assertTrue(matcher.matches("bar"));
    }

    @Test
    public void matchesReturnsFalseIfValueDoesNotContainSubstring() {
        MCRStringContainsMatcher
            matcher = new MCRStringContainsMatcher("bar");
        assertFalse(matcher.matches("foobaz"));
    }

    @Test
    public void matchesReturnsFalseIfActualValueIsNull() {
        MCRStringContainsMatcher
            matcher = new MCRStringContainsMatcher("bar");
        assertFalse(matcher.matches(null));
    }

    @Test
    public void matchesWithEmptySubstringMatchesAnyNonNullValue() {
        MCRStringContainsMatcher
            matcher = new MCRStringContainsMatcher("");
        assertTrue(matcher.matches("anything"));
    }

    @Test
    public void matchesIsCaseSensitive() {
        MCRStringContainsMatcher
            matcher = new MCRStringContainsMatcher("Bar");
        assertFalse(matcher.matches("foobarbaz"));
    }
}
