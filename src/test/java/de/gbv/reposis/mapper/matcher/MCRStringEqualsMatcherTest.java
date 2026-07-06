package de.gbv.reposis.mapper.matcher;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MCRStringEqualsMatcherTest {

    @Test
    public void constructorRejectsNullExpectedValue() {
        assertThrows(NullPointerException.class, () -> new MCRStringEqualsMatcher(null));
    }

    @Test
    public void matchesReturnsTrueIfValueEqualsExpectedValue() {
        MCRStringEqualsMatcher matcher = new MCRStringEqualsMatcher("foo");
        assertTrue(matcher.matches("foo"));
    }

    @Test
    public void matchesReturnsFalseIfValueDiffersFromExpectedValue() {
        MCRStringEqualsMatcher matcher = new MCRStringEqualsMatcher("foo");
        assertFalse(matcher.matches("bar"));
    }

    @Test
    public void matchesIsCaseSensitive() {
        MCRStringEqualsMatcher matcher = new MCRStringEqualsMatcher("foo");
        assertFalse(matcher.matches("FOO"));
    }

    @Test
    public void matchesReturnsFalseIfActualValueIsNull() {
        MCRStringEqualsMatcher matcher = new MCRStringEqualsMatcher("foo");
        assertFalse(matcher.matches(null));
    }
}
