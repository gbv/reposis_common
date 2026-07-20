package de.gbv.reposis.user.merger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.mycore.common.MCRJPATestCase;
import org.mycore.user2.MCRUser;

public class MCRUserAttributeGapFillMergerTest extends MCRJPATestCase {

    private final MCRUserAttributeGapFillMerger merger = new MCRUserAttributeGapFillMerger();

    @Test
    public void shouldMergeAttributes() {
        MCRUser target = new MCRUser("foo");
        target.setUserAttribute("department", "IT");
        target.setUserAttribute("phone", "1234");

        MCRUser source = new MCRUser("foo");
        source.setUserAttribute("department", "HR");
        source.setUserAttribute("email", "test@example.org");

        boolean changed = merger.merge(source, target);

        assertTrue(changed);

        assertEquals("IT", target.getUserAttribute("department"));
        assertEquals("1234", target.getUserAttribute("phone"));
        assertEquals("test@example.org", target.getUserAttribute("email"));
    }

    @Test
    public void shouldNotOverwriteAttributeValue() {
        MCRUser target = new MCRUser("foo");
        target.setUserAttribute("department", "IT");

        MCRUser source = new MCRUser("foo");
        source.setUserAttribute("department", "HR");

        boolean changed = merger.merge(source, target);

        assertFalse(changed);
        assertEquals("IT", target.getUserAttribute("department"));
    }
}
