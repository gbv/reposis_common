package de.gbv.reposis.user.merger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.test.MCRJPAExtension;
import org.mycore.test.MyCoReTest;
import org.mycore.user2.MCRUser;

@MyCoReTest
@ExtendWith(MCRJPAExtension.class)
public class MCRUserAttributeGapFillMergerTest {

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
