package de.gbv.reposis.solr;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class MCRSolrFileSizeStrategyTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void constructorRejectsNullDelegate() {
        assertThrows(NullPointerException.class, () -> new MCRSolrFileSizeStrategy(10, null));
    }

    @Test
    public void constructorRejectsNegativeMaxSize() {
        assertThrows(IllegalArgumentException.class, () -> new MCRSolrFileSizeStrategy(-1, (f, a) -> true));
    }

    @Test
    public void checkReturnsFalseIfFileExceedsMaxSize() throws IOException {
        Path file = createFile(11);
        MCRSolrFileSizeStrategy strategy = new MCRSolrFileSizeStrategy(10, (f, a) -> true);
        assertFalse(strategy.check(file, attributes(file)));
    }

    @Test
    public void checkDelegatesIfFileHasMaxSize() throws IOException {
        Path file = createFile(10);
        assertTrue(new MCRSolrFileSizeStrategy(10, (f, a) -> true).check(file, attributes(file)));
        assertFalse(new MCRSolrFileSizeStrategy(10, (f, a) -> false).check(file, attributes(file)));
    }

    @Test
    public void checkDoesNotCallDelegateIfFileExceedsMaxSize() throws IOException {
        Path file = createFile(11);
        MCRSolrFileSizeStrategy strategy = new MCRSolrFileSizeStrategy(10, (f, a) -> {
            throw new AssertionError("delegate must not be called");
        });
        assertFalse(strategy.check(file, attributes(file)));
    }

    private Path createFile(int size) throws IOException {
        Path file = folder.newFile().toPath();
        Files.write(file, new byte[size]);
        return file;
    }

    private static BasicFileAttributes attributes(Path file) throws IOException {
        return Files.readAttributes(file, BasicFileAttributes.class);
    }
}
