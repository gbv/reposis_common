/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <https://www.gnu.org/licenses/>.
 */

package de.gbv.reposis.solr;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.solr.index.strategy.MCRSolrFileStrategy;

/**
 * A {@link MCRSolrFileStrategy} that only sends the content of files to Solr whose size does not exceed a
 * configured limit. All other decisions are delegated to another strategy. Files that are too large are still
 * indexed, but without their content.
 * <p>
 * Configuration:
 * <ul>
 *   <li>{@code MCR.Solr.FileSizeStrategy.MaxSize}: maximum file size in bytes, unlimited if not set</li>
 *   <li>{@code MCR.Solr.FileSizeStrategy.Delegate}: class name of the strategy to delegate to</li>
 * </ul>
 */
public class MCRSolrFileSizeStrategy implements MCRSolrFileStrategy {

    public static final String CONFIG_PREFIX = "MCR.Solr.FileSizeStrategy.";

    private static final Logger LOGGER = LogManager.getLogger();

    private final long maxSize;

    private final MCRSolrFileStrategy delegate;

    public MCRSolrFileSizeStrategy() {
        this(MCRConfiguration2.getLong(CONFIG_PREFIX + "MaxSize").orElse(Long.MAX_VALUE),
            MCRConfiguration2.instantiateClass(MCRConfiguration2.getStringOrThrow(CONFIG_PREFIX + "Delegate")));
    }

    /**
     * Creates a new {@code MCRSolrFileSizeStrategy}.
     *
     * @param maxSize the maximum file size in bytes whose content is sent to Solr
     * @param delegate the strategy that decides about all files within the size limit
     */
    public MCRSolrFileSizeStrategy(long maxSize, MCRSolrFileStrategy delegate) {
        if (maxSize < 0) {
            throw new IllegalArgumentException("maxSize must not be negative");
        }
        this.maxSize = maxSize;
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    }

    @Override
    public boolean check(Path file, BasicFileAttributes attrs) {
        if (attrs.size() > maxSize) {
            LOGGER.info("Skipping content of {} ({} bytes), it exceeds the limit of {} bytes", file, attrs.size(),
                maxSize);
            return false;
        }
        return delegate.check(file, attrs);
    }
}
