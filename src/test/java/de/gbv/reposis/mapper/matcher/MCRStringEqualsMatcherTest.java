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
    public void testReturnsTrueIfValueEqualsExpectedValue() {
        MCRStringEqualsMatcher matcher = new MCRStringEqualsMatcher("foo");
        assertTrue(matcher.test("foo"));
    }

    @Test
    public void testReturnsFalseIfValueDiffersFromExpectedValue() {
        MCRStringEqualsMatcher matcher = new MCRStringEqualsMatcher("foo");
        assertFalse(matcher.test("bar"));
    }

    @Test
    public void testIsCaseSensitive() {
        MCRStringEqualsMatcher matcher = new MCRStringEqualsMatcher("foo");
        assertFalse(matcher.test("FOO"));
    }

    @Test
    public void testReturnsFalseIfActualValueIsNull() {
        MCRStringEqualsMatcher matcher = new MCRStringEqualsMatcher("foo");
        assertFalse(matcher.test(null));
    }
}
