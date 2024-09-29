package csw.korea.festival.main.util;

import csw.korea.festival.main.config.converter.TimezoneMapper;

public class CoordinatesConverter {
    // Constants related to the WGS84 ellipsoid.
    private static final double A_WGS84 = 6378137.0; // Semi-major axis.
    private static final double FLATTENING_FACTOR = 0.0033528106647474805;

    // Constants for Korea TM projection.
    private static final double K0 = 1; // Scale factor.
    private static final double DX = 500000; // False Easting.
    private static final double DY = 200000; // False Northing.
    private static final double LAT0 = 38; // Latitude of origin.
    private static final double LON0 = 127; // Longitude of origin.
    private static final double RADIANS_PER_DEGREE = Math.PI / 180;
    private static final double SCALE_FACTOR = 2.5;

    public static double calculateDistanceApproximately(double lat1, double long1, double lat2, double long2) {
        double lat1Rad = lat1 * RADIANS_PER_DEGREE;
        double lat2Rad = lat2 * RADIANS_PER_DEGREE;
        double deltaLat = (lat2 - lat1) * RADIANS_PER_DEGREE;
        double deltaLong = (long2 - long1) * RADIANS_PER_DEGREE;
        double x = deltaLong * Math.cos((lat1Rad + lat2Rad) / 2);
        return Math.sqrt(x * x + deltaLat * deltaLat) * A_WGS84;
    }

    public static XYCoordinate convertWGS84ToWCONGNAMUL(double lat, double lon) {
        double[] tmCoords = transformWGS84ToKoreaTM(lat, lon);
        double x = Math.round(tmCoords[0] * SCALE_FACTOR);
        double y = Math.round(tmCoords[1] * SCALE_FACTOR);
        return new XYCoordinate(x, y);
    }

    public static double[] transformWGS84ToKoreaTM(double lat, double lon) {
        // Convert degrees to radians
        final double latRad = lat * RADIANS_PER_DEGREE;
        final double lonRad = lon * RADIANS_PER_DEGREE;
        final double lRad = LAT0 * RADIANS_PER_DEGREE;
        final double mRad = LON0 * RADIANS_PER_DEGREE;

        // Compute trigonometric values
        final double sinLat = Math.sin(latRad);
        final double cosLat = Math.cos(latRad);
        final double tanLat = sinLat / cosLat;

        // Handle w calculation and ensure proper calculation for different e values
        double w = 1.0 / FLATTENING_FACTOR;

        // Compute z and G
        double z = A_WGS84 * (w - 1.0) / w;
        double zSquared = z * z;
        double dSquared = A_WGS84 * A_WGS84;
        double G = 1.0 - (zSquared / dSquared);
        double w0 = (dSquared - zSquared) / zSquared; // Preserve original w

        // Simplify z calculation
        z = (A_WGS84 - z) / (A_WGS84 + z);
        double z2 = z * z;
        double z3 = z2 * z;
        double z4 = z3 * z;
        double z5 = z4 * z;

        // Precompute coefficients to avoid redundant calculations
        double E = A_WGS84 * (1 - z + (5.0 / 4.0) * (z2 - z3) + (81.0 / 64.0) * (z4 - z5));
        double I = (3.0 / 2.0) * A_WGS84 * (z - z2 + (7.0 / 8.0) * (z3 - z4) + (55.0 / 64.0) * z5);
        double J = (15.0 / 16.0) * A_WGS84 * (z2 - z3 + (3.0 / 4.0) * (z4 - z5));
        double L = (35.0 / 48.0) * A_WGS84 * (z3 - z4 + (11.0 / 16.0) * z5);
        double M = (315.0 / 512.0) * A_WGS84 * (z4 - z5);
        double D = lonRad - mRad;

        // Compute u and z
        double u = E * lRad - I * Math.sin(2.0 * lRad) + J * Math.sin(4.0 * lRad) - L * Math.sin(6.0 * lRad) + M * Math.sin(8.0 * lRad);
        double z1 = u * K0; // Use z1 to avoid overwriting z

        // More optimizations on G
        G = A_WGS84 / Math.sqrt(1.0 - G * sinLat * sinLat);

        // Recompute u and compute o
        u = E * latRad - I * Math.sin(2.0 * latRad) + J * Math.sin(4.0 * latRad) - L * Math.sin(6.0 * latRad) + M * Math.sin(8.0 * latRad);
        double o = u * K0;

        // Precompute powers of cosLat
        double cosLat3 = cosLat * cosLat * cosLat;
        double cosLat5 = cosLat3 * cosLat * cosLat;
        double cosLat7 = cosLat5 * cosLat * cosLat;

        // Calculate E1, I1, J1, H using optimized expressions
        double E1 = G * sinLat * cosLat * K0 * 0.5;
        double I1 = G * sinLat * cosLat3 * K0 * (5.0 - tanLat * tanLat + 9.0 * w0 + 4.0 * w0 * w0) / 24.0;
        double J1 = G * sinLat * cosLat5 * K0 * (
                61.0 - 58.0 * tanLat * tanLat + Math.pow(tanLat, 4)
                        + 270.0 * w0 - 330.0 * tanLat * tanLat * w0
                        + 445.0 * w0 * w0 + 324.0 * Math.pow(w0, 3)
                        - 680.0 * tanLat * tanLat * w0 * w0
                        + 88.0 * Math.pow(w0, 4)
                        - 600.0 * tanLat * tanLat * Math.pow(w0, 3)
                        - 192.0 * tanLat * tanLat * Math.pow(w0, 4)
        ) / 720.0;
        double H = G * sinLat * cosLat7 * K0 * (
                1385.0 - 3111.0 * tanLat * tanLat
                        + 543.0 * Math.pow(tanLat, 4)
                        - Math.pow(tanLat, 6)
        ) / 40320.0;

        // Update o with polynomial terms
        o += D * D * E1
                + Math.pow(D, 3) * I1
                + Math.pow(D, 5) * J1
                + Math.pow(D, 7) * H;

        // Compute y (Northing)
        double y = o - z1 + DX;

        // Compute x using optimized terms without overwriting w
        double o1 = G * cosLat * K0;
        double zTerm = G * cosLat3 * K0 * (1.0 - tanLat * tanLat + w0) / 6.0;
        double wTerm = G * cosLat5 * K0 * (
                5.0 - 18.0 * tanLat * tanLat
                        + Math.pow(tanLat, 4)
                        + 14.0 * w0
                        - 58.0 * tanLat * tanLat * w0
                        + 13.0 * w0 * w0
                        + 4.0 * Math.pow(w0, 3)
                        - 64.0 * tanLat * tanLat * w0 * w0
                        - 25.0 * tanLat * tanLat * Math.pow(w0, 3)
        ) / 120.0;
        double uTerm = G * cosLat7 * K0 * (
                61.0 - 479.0 * tanLat * tanLat
                        + 179.0 * Math.pow(tanLat, 4)
                        - Math.pow(tanLat, 6)
        ) / 5040.0;

        // Compute x (Easting)
        double x = DY
                + D * o1
                + Math.pow(D, 3) * zTerm
                + Math.pow(D, 5) * wTerm
                + Math.pow(D, 7) * uTerm;

        // Return x (Easting) and y (Northing)
        return new double[]{x, y};
    }


    public static XYCoordinate convertWCONGNAMULToWGS84(double lat, double lon) {
        double[] tmCoords = transformKoreaTMToWGS84(lat / 2.5, lon / 2.5);
        return new XYCoordinate(tmCoords[0], tmCoords[1]);
    }

    private static double[] transformKoreaTMToWGS84(double x, double y) {
        double u = CoordinatesConverter.FLATTENING_FACTOR;

        double w = Math.atan(1) / 45; // Conversion factor from degrees to radians
        double o = CoordinatesConverter.LAT0 * w;
        double D = CoordinatesConverter.LON0 * w;
        u = 1 / u;

        double B = CoordinatesConverter.A_WGS84 * (u - 1) / u;
        double z = (CoordinatesConverter.A_WGS84 * CoordinatesConverter.A_WGS84 - B * B) / (CoordinatesConverter.A_WGS84 * CoordinatesConverter.A_WGS84);
        u = (CoordinatesConverter.A_WGS84 * CoordinatesConverter.A_WGS84 - B * B) / (B * B);
        B = (CoordinatesConverter.A_WGS84 - B) / (CoordinatesConverter.A_WGS84 + B);

        // Precompute powers of B
        double B2 = B * B;
        double B3 = B2 * B;
        double B4 = B3 * B;
        double B5 = B4 * B;

        double G = CoordinatesConverter.A_WGS84 * (1 - B + 5 * (B2 - B3) / 4 + 81 * (B4 - B5) / 64);
        double E = 3 * CoordinatesConverter.A_WGS84 * (B - B2 + 7 * (B3 - B4) / 8 + 55 * B5 / 64) / 2;
        double I = 15 * CoordinatesConverter.A_WGS84 * (B2 - B3 + 3 * (B4 - B5) / 4) / 16;
        double J = 35 * CoordinatesConverter.A_WGS84 * (B3 - B4 + 11 * B5 / 16) / 48;
        double L = 315 * CoordinatesConverter.A_WGS84 * (B4 - B5) / 512;

        o = G * o - E * Math.sin(2 * o) + I * Math.sin(4 * o) - J * Math.sin(6 * o) + L * Math.sin(8 * o);
        // o *= CoordinatesConverter.K0;
        o = y + o - CoordinatesConverter.DX;
        double M = o / CoordinatesConverter.K0;
        double H = CoordinatesConverter.A_WGS84 * (1 - z) / Math.pow(Math.sqrt(1 - z * Math.pow(Math.sin(0), 2)), 3);
        o = M / H;
        for (int i = 0; i < 5; i++) {
            B = G * o - E * Math.sin(2 * o) + I * Math.sin(4 * o) - J * Math.sin(6 * o) + L * Math.sin(8 * o);
            H = CoordinatesConverter.A_WGS84 * (1 - z) / Math.pow(Math.sqrt(1 - z * Math.pow(Math.sin(o), 2)), 3);
            o += (M - B) / H;
        }
        H = CoordinatesConverter.A_WGS84 * (1 - z) / Math.pow(Math.sqrt(1 - z * Math.pow(Math.sin(o), 2)), 3);
        G = CoordinatesConverter.A_WGS84 / Math.sqrt(1 - z * Math.pow(Math.sin(o), 2));
        B = Math.sin(o);
        z = Math.cos(o);
        E = B / z;
        u *= z * z;
        double A = x - CoordinatesConverter.DY;
        B = E / (2 * H * G * Math.pow(CoordinatesConverter.K0, 2));
        I = E * (5 + 3 * E * E + u - 4 * u * u - 9 * E * E * u) / (24 * H * G * G * G * Math.pow(CoordinatesConverter.K0, 4));
        J = E * (61 + 90 * E * E + 46 * u + 45 * E * E * E * E - 252 * E * E * u - 3 * u * u + 100 * u * u * u - 66 * E * E * u * u - 90 * E * E * E * E * u + 88 * u * u * u * u + 225 * E * E * E * E * u * u + 84 * E * E * u * u * u - 192 * E * E * u * u * u * u) / (720 * H * G * G * G * G * G * Math.pow(CoordinatesConverter.K0, 6));
        H = E * (1385 + 3633 * E * E + 4095 * E * E * E * E + 1575 * E * E * E * E * E * E) / (40320 * H * G * G * G * G * G * G * G * Math.pow(CoordinatesConverter.K0, 8));
        o = o - Math.pow(A, 2) * B + Math.pow(A, 4) * I - Math.pow(A, 6) * J + Math.pow(A, 8) * H;
        B = 1 / (G * z * CoordinatesConverter.K0);
        H = (1 + 2 * E * E + u) / (6 * G * G * G * z * z * z * Math.pow(CoordinatesConverter.K0, 3));
        u = (5 + 6 * u + 28 * E * E - 3 * u * u + 8 * E * E * u + 24 * E * E * E * E - 4 * u * u * u + 4 * E * E * u * u + 24 * E * E * u * u * u) / (120 * G * G * G * G * G * z * z * z * z * z * Math.pow(CoordinatesConverter.K0, 5));
        z = (61 + 662 * E * E + 1320 * E * E * E * E + 720 * E * E * E * E * E * E) / (5040 * G * G * G * G * G * G * G * z * z * z * z * z * z * z * Math.pow(CoordinatesConverter.K0, 7));
        A = A * B - Math.pow(A, 3) * H + Math.pow(A, 5) * u - Math.pow(A, 7) * z;
        D += A;

        return new double[]{o / w, D / w}; // LATITUDE, LONGITUDE
    }

    public static boolean IsInSouthKorea(double lat, double lon) {
        return TimezoneMapper.latLngToTimezoneString(lat, lon).equals("Asia/Seoul");
    }

    public static class XYCoordinate {
        double x;
        double y;

        public XYCoordinate(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public double latitude() {
            return x;
        }

        public double longitude() {
            return y;
        }

        @Override
        public String toString() {
            return "WCONGNAMULCoord{" + "x=" + x + ", y=" + y + '}';
        }
    }
}
