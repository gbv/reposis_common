package de.gbv.reposis.geo;

import org.w3c.dom.NodeList;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class GeoFunctions {

    public static double[] getGeoPoint(String modsCoords) {
        if (modsCoords == null) {
            return null;
        }
        String[] parts = modsCoords.split(" ");
        if (parts.length != 2) {
            return null;
        }

        double[] result = new double[2];
        result[0] = Double.parseDouble(parts[0]);
        result[1] = Double.parseDouble(parts[1]);
        return result;
    }

    public static double[][] getGeoPoints(String modsCoords){
        String[] points = modsCoords.split(",[ ]*");
        double[][] doubles = Arrays.stream(points)
                .map(GeoFunctions::getGeoPoint)
                .toArray(double[][]::new);
        return doubles;
    }

    public static boolean isPolygonInverse(double[][] vertices) {
        // Calculate the sum of the angles between the points
        int n = vertices.length;
        double angleSum = 0;
        for (int i = 0; i < n; i++) {
            double[] p1 = vertices[i];
            double[] p2 = vertices[(i + 1) % n];
            double[] p3 = vertices[(i + 2) % n];
            angleSum += angleBetween(p1, p2, p3);
        }

        // If the angle sum is positive, the polygon is counterclockwise
        if (angleSum > 0) {
            return false;
        }
        // If the angle sum is negative, the polygon is clockwise
        else if (angleSum < 0) {
            return true;
        }
        // If the angle sum is zero, the polygon is flat
        else {
            return false;
        }
    }

    public static double angleBetween(double[] p1, double[] p2, double[] p3) {
        // Calculate the angle between three points
        double a = p1[0] - p2[0];
        double b = p1[1] - p2[1];
        double c = p3[0] - p2[0];
        double d = p3[1] - p2[1];
        return Math.atan2(a * d - b * c, a * c + b * d);
    }

    /**
     * The MODS Metadata contains single points or polygons. The polygons are either clockwise or counterclockwise.
     * Solr GEO3D only accepts polygons in counterclockwise order, otherwise the polygon is interpreted as a hole.
     * The Polygon gets reversed if it is clockwise.
     * @param modsCoords The content of the mods:coordinates element.
     * @return a polygon or point WKT string.
     */
    public static String getNormalizedWKTString(NodeList modsCoords) {
        if (modsCoords == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("GeometryCollection (");
        for (int i = 0; i < modsCoords.getLength(); i++) {
            String coords = modsCoords.item(i).getTextContent();
            double[][] verticles = getGeoPoints(coords);
            if (verticles.length == 1) {
                sb.append("POINT (").append(verticles[0][0]).append(" ").append(verticles[0][1]).append(")");
            } else  {
                if(!isPolygonInverse(verticles)){
                    List<double[]> list = Arrays.asList(verticles);
                    Collections.reverse(list);
                    verticles = list.toArray(double[][]::new);
                }

                String points = Arrays.stream(verticles)
                        .map(point -> point[0] + " " + point[1])
                        .collect(Collectors.joining(", "));
                sb.append("POLYGON ((").append(points).append("))");
            }
            if(i < modsCoords.getLength() - 1){
                sb.append(", ");
            }
        }
        sb.append(")");
        return sb.toString();
    }
}
