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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mycore.common.MCRException;
import org.mycore.common.content.MCRByteContent;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.transformer.MCRContentTransformer;
import org.mycore.test.MyCoReTest;

import de.gbv.reposis.sitelinks.dto.SitelinksObjectDto;
import de.gbv.reposis.sitelinks.dto.SitelinksRootPageDto;
import de.gbv.reposis.sitelinks.dto.SitelinksYearPageDto;

@MyCoReTest
public class SitelinksXslPageMapperTest {

    private final SitelinksXslPageMapper mapper = new SitelinksXslPageMapper(new TestTransformer());

    @Test
    public void testSameContentHasSameETag() throws IOException {
        Assertions.assertEquals(mapYearPage("a_mods_00000001").getETag(), mapYearPage("a_mods_00000001").getETag());
    }

    @Test
    public void testChangedContentHasOtherETag() throws IOException {
        Assertions.assertNotEquals(mapYearPage("a_mods_00000001").getETag(), mapYearPage("a_mods_00000002").getETag());
    }

    @Test
    public void testPageKeepsContentProperties() throws IOException {
        MCRContent page = mapYearPage("a_mods_00000001");
        Assertions.assertEquals("page.html", page.getName());
        Assertions.assertEquals("text/html", page.getMimeType());
        Assertions.assertEquals("UTF-8", page.getEncoding());
        Assertions.assertFalse(page.isUsingSession());
        Assertions.assertEquals(-1, page.lastModified());
    }

    @Test
    public void testRootPageHasContentBasedETag() throws IOException {
        MCRContent page = mapper.map(new SitelinksRootPageDto(List.of(2026)));
        Assertions.assertEquals(page.getETag(), mapper.map(new SitelinksRootPageDto(List.of(2026))).getETag());
        Assertions.assertNotEquals(page.getETag(), mapper.map(new SitelinksRootPageDto(List.of(2025))).getETag());
        Assertions.assertEquals("text/html", page.getMimeType());
    }

    @Test
    public void testRootPageTransformationFails() {
        Assertions.assertThrows(SitelinksMappingException.class,
            () -> new SitelinksXslPageMapper(new FailingTransformer()).map(new SitelinksRootPageDto(List.of(2026))));
    }

    @Test
    public void testYearPageTransformationFails() {
        Assertions.assertThrows(SitelinksMappingException.class, () -> new SitelinksXslPageMapper(
            new FailingTransformer()).map(yearPage(new SitelinksObjectDto("a_mods_00000001", "Title"))));
    }

    @Test
    public void testLazyTransformationFails() {
        Assertions.assertThrows(SitelinksMappingException.class,
            () -> new SitelinksXslPageMapper(new LazyFailingTransformer())
                .map(new SitelinksRootPageDto(List.of(2026))));
    }

    @Test
    public void testTitleIsPassedToStylesheet() throws IOException {
        assertPageContains("title=\"A &lt; B\"", new SitelinksObjectDto("a_mods_00000001", "A < B"));
    }

    @Test
    public void testMissingTitleIsOmitted() throws IOException {
        assertPageContains("<object-id>a_mods_00000001</object-id>", new SitelinksObjectDto("a_mods_00000001", null));
    }

    @Test
    public void testTitleWithInvalidXmlCharactersIsOmitted() throws IOException {
        assertPageContains("<object-id>a_mods_00000001</object-id>",
            new SitelinksObjectDto("a_mods_00000001", "Title\u0001"));
    }

    private void assertPageContains(String expected, SitelinksObjectDto object) throws IOException {
        String page = new String(mapper.map(yearPage(object)).asByteArray(), StandardCharsets.UTF_8);
        Assertions.assertTrue(page.contains(expected), page);
    }

    private MCRContent mapYearPage(String objectId) {
        return mapper.map(yearPage(new SitelinksObjectDto(objectId, "Title")));
    }

    private static SitelinksYearPageDto yearPage(SitelinksObjectDto object) {
        return new SitelinksYearPageDto(2026, 1, 1, List.of(object));
    }

    private static class TestTransformer extends MCRContentTransformer {

        @Override
        public MCRContent transform(MCRContent source) throws IOException {
            MCRContent content = new MCRByteContent(source.asByteArray(), -1);
            content.setName("page.html");
            content.setMimeType("text/html");
            content.setEncoding("UTF-8");
            content.setUsingSession(true);
            return content;
        }
    }

    private static class LazyFailingTransformer extends MCRContentTransformer {

        @Override
        public MCRContent transform(MCRContent source) {
            return new MCRByteContent(new byte[0], -1) {
                @Override
                public byte[] asByteArray() {
                    throw new MCRException("transformation failed");
                }
            };
        }
    }

    private static class FailingTransformer extends MCRContentTransformer {

        @Override
        public MCRContent transform(MCRContent source) throws IOException {
            throw new IOException("transformation failed");
        }
    }
}
