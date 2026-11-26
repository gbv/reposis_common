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

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mycore.test.MyCoReTest;

import de.gbv.reposis.sitelinks.dto.SitelinksObjectDto;

@MyCoReTest
public class SitelinksServiceTest {

    private final CountingMetadataService metadataService = new CountingMetadataService();

    private SitelinksService service;

    @BeforeEach
    public void createService() {
        service = new SitelinksService(metadataService);
    }

    @Test
    public void testYearsAreCached() {
        Assertions.assertEquals(List.of(2026, 2025), service.getRootPage().years());
        Assertions.assertTrue(service.hasYearPage(2026));
        Assertions.assertNotNull(service.getYearPage(2026, 1, 100));
        Assertions.assertEquals(1, metadataService.yearRequests);
    }

    @Test
    public void testYearPageContainsObjects() {
        Assertions.assertEquals(List.of(new SitelinksObjectDto("a_mods_00000001", "Title")),
            service.getYearPage(2026, 1, 100).objects());
    }

    @Test
    public void testInvalidateCache() {
        service.getRootPage();
        service.invalidateCache();
        service.getRootPage();
        Assertions.assertEquals(2, metadataService.yearRequests);
    }

    @Test
    public void testPageOutOfIntegerRangeIsNotRequested() {
        Assertions.assertThrows(SitelinksNotFoundException.class,
            () -> service.getYearPage(2026, Integer.MAX_VALUE, 100));
        Assertions.assertEquals(0, metadataService.objectRequests);
    }

    @Test
    public void testLastRowOutOfIntegerRangeIsNotRequested() {
        Assertions.assertThrows(SitelinksNotFoundException.class, () -> service.getYearPage(2026, 21474837, 100));
        Assertions.assertEquals(0, metadataService.objectRequests);
    }

    private static class CountingMetadataService implements SitelinksMetadataService {

        private int yearRequests;

        private int objectRequests;

        @Override
        public List<Integer> getYearsWithObjects() {
            yearRequests++;
            return List.of(2025, 2026);
        }

        @Override
        public ObjectsWithCount getObjectsByYear(int year, int offset, int limit) {
            objectRequests++;
            return new ObjectsWithCount(List.of(new SitelinksObjectDto("a_mods_00000001", "Title")), 1);
        }
    }
}
