package de.gbv.reposis.geo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

public class GeoFunctionsTest {

    @Test
    public void shouldBuildPoint() {
        assertEquals("GeometryCollection (POINT (10.0 20.0))",
            GeoFunctions.getNormalizedWKTString(List.of("10 20")));
    }

    @Test
    public void shouldBuildLineString() {
        assertEquals("GeometryCollection (LINESTRING (0.0 0.0, 1.0 1.0))",
            GeoFunctions.getNormalizedWKTString(List.of("0 0, 1 1")));
    }

    @Test
    public void shouldKeepCounterclockwisePolygon() {
        assertEquals("GeometryCollection (POLYGON ((0.0 0.0, 1.0 0.0, 1.0 1.0, 0.0 1.0)))",
            GeoFunctions.getNormalizedWKTString(List.of("0 0, 1 0, 1 1, 0 1")));
    }

    @Test
    public void shouldReverseClockwisePolygon() {
        assertEquals("GeometryCollection (POLYGON ((1.0 0.0, 1.0 1.0, 0.0 1.0, 0.0 0.0)))",
            GeoFunctions.getNormalizedWKTString(List.of("0 0, 0 1, 1 1, 1 0")));
    }

    @Test
    public void shouldJoinMultipleCoordinates() {
        assertEquals("GeometryCollection (POINT (10.0 20.0), POINT (30.0 40.0))",
            GeoFunctions.getNormalizedWKTString(List.of("10 20", "30 40")));
    }

    @Test
    public void shouldReturnEmptyStringForInvalidCoordinates() {
        assertEquals("", GeoFunctions.getNormalizedWKTString(List.of("E 72°--E 148°/N 13°--N 18°")));
    }

    @Test
    public void shouldReturnEmptyStringWithoutCoordinates() {
        assertEquals("", GeoFunctions.getNormalizedWKTString(List.of()));
    }
}
