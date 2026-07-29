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
    public void testReturnsTrueIfValueContainsSubstring() {
        MCRStringContainsMatcher
            matcher = new MCRStringContainsMatcher("bar");
        assertTrue(matcher.test("foobarbaz"));
    }

    @Test
    public void testReturnsTrueIfValueEqualsSubstring() {
        MCRStringContainsMatcher
            matcher = new MCRStringContainsMatcher("bar");
        assertTrue(matcher.test("bar"));
    }

    @Test
    public void testReturnsFalseIfValueDoesNotContainSubstring() {
        MCRStringContainsMatcher
            matcher = new MCRStringContainsMatcher("bar");
        assertFalse(matcher.test("foobaz"));
    }

    @Test
    public void testReturnsFalseIfActualValueIsNull() {
        MCRStringContainsMatcher
            matcher = new MCRStringContainsMatcher("bar");
        assertFalse(matcher.test(null));
    }

    @Test
    public void matchesWithEmptySubstringTestAnyNonNullValue() {
        MCRStringContainsMatcher
            matcher = new MCRStringContainsMatcher("");
        assertTrue(matcher.test("anything"));
    }

    @Test
    public void testIsCaseSensitive() {
        MCRStringContainsMatcher
            matcher = new MCRStringContainsMatcher("Bar");
        assertFalse(matcher.test("foobarbaz"));
    }
}
