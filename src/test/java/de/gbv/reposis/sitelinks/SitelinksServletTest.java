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

package de.gbv.reposis.sitelinks;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRStringContent;

import de.gbv.reposis.sitelinks.dto.SitelinksObjectDto;
import de.gbv.reposis.sitelinks.dto.SitelinksRootPageDto;
import de.gbv.reposis.sitelinks.dto.SitelinksYearPageDto;
import jakarta.servlet.http.HttpServletRequest;

public class SitelinksServletTest extends MCRTestCase {

    private SitelinksServlet servlet;

    private SitelinksServlet failingServlet;

    @Before
    public void createServlets() {
        SitelinksService service = new SitelinksService(new TestMetadataService());
        servlet = new SitelinksServlet(service, new TestMapper(), 100);
        failingServlet = new SitelinksServlet(service, new FailingMapper(), 100);
    }

    @Test
    public void testExistingPages() throws IOException {
        Assert.assertNotNull(servlet.getContent(request("/"), null));
        Assert.assertNotNull(servlet.getContent(request("/2026"), null));
        Assert.assertNotNull(servlet.getContent(request("/2026/page/2"), null));
    }

    @Test
    public void testInvalidPagesAreNotFound() throws IOException {
        for (String path : List.of("/2026/page/0", "/2026/page/-1", "/2026/page/abc", "/2026/page/3",
            "/2026/page/2147483647", "/2000", "/abc", "///")) {
            Assert.assertNull(path, servlet.getContent(request(path), null));
        }
    }

    @Test
    public void testMappingFailureOfRootPageIsNotHandledAsNotFound() {
        Assert.assertThrows(SitelinksMappingException.class, () -> failingServlet.getContent(request("/"), null));
    }

    @Test
    public void testMappingFailureOfYearPageIsNotHandledAsNotFound() {
        Assert.assertThrows(SitelinksMappingException.class, () -> failingServlet.getContent(request("/2026"), null));
    }

    private static HttpServletRequest request(String pathInfo) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getPathInfo()).thenReturn(pathInfo);
        return request;
    }

    private static class TestMetadataService implements SitelinksMetadataService {

        @Override
        public List<Integer> getYearsWithObjects() {
            return List.of(2026);
        }

        @Override
        public ObjectsWithCount getObjectsByYear(int year, int offset, int limit) {
            return new ObjectsWithCount(List.of(new SitelinksObjectDto("a_mods_00000001", "Title")), 150);
        }
    }

    private static class TestMapper implements SitelinksPageMapper {

        @Override
        public MCRContent map(SitelinksRootPageDto rootPage) {
            return new MCRStringContent("root");
        }

        @Override
        public MCRContent map(SitelinksYearPageDto yearPage) {
            return new MCRStringContent("year");
        }
    }

    private static class FailingMapper implements SitelinksPageMapper {

        @Override
        public MCRContent map(SitelinksRootPageDto rootPage) {
            throw new SitelinksMappingException("mapping failed", new IOException());
        }

        @Override
        public MCRContent map(SitelinksYearPageDto yearPage) {
            throw new SitelinksMappingException("mapping failed", new IOException());
        }
    }
}
