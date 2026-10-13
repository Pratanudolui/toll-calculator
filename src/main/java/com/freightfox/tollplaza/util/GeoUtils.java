package com.freightfox.tollplaza.util;

/**
 * Spherical-earth geometry helpers used to simulate a route between two points
 * and to decide which toll plazas fall "on" that route.
 *
 * <p>The route is approximated as the great-circle line between source and
 * destination (see README for why, and the corridor-width assumption used to
 * decide inclusion). All distances are in kilometres.
 */
public final class GeoUtils {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private GeoUtils() {
    }

    public static double haversineKm(Coordinates a, Coordinates b) {
        double lat1 = Math.toRadians(a.latitude());
        double lat2 = Math.toRadians(b.latitude());
        double dLat = Math.toRadians(b.latitude() - a.latitude());
        double dLon = Math.toRadians(b.longitude() - a.longitude());

        double sinDLat = Math.sin(dLat / 2);
        double sinDLon = Math.sin(dLon / 2);

        double h = sinDLat * sinDLat + Math.cos(lat1) * Math.cos(lat2) * sinDLon * sinDLon;
        double c = 2 * Math.atan2(Math.sqrt(h), Math.sqrt(1 - h));
        return EARTH_RADIUS_KM * c;
    }

    public static double bearingRad(Coordinates from, Coordinates to) {
        double lat1 = Math.toRadians(from.latitude());
        double lat2 = Math.toRadians(to.latitude());
        double dLon = Math.toRadians(to.longitude() - from.longitude());

        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);
        return Math.atan2(y, x);
    }

    /**
     * Perpendicular ("cross-track") distance in km of {@code point} from the
     * great-circle line through {@code routeStart} and {@code routeEnd}.
     * The sign indicates which side of the route the point falls on.
     */
    public static double crossTrackDistanceKm(Coordinates point, Coordinates routeStart, Coordinates routeEnd) {
        double d13 = haversineKm(routeStart, point) / EARTH_RADIUS_KM;
        double theta13 = bearingRad(routeStart, point);
        double theta12 = bearingRad(routeStart, routeEnd);

        return Math.asin(Math.sin(d13) * Math.sin(theta13 - theta12)) * EARTH_RADIUS_KM;
    }

    /**
     * Signed distance in km from {@code routeStart}, measured along the great-circle
     * route, to the point on that route closest to {@code point}. Negative means the
     * point's closest point on the route lies behind {@code routeStart} (i.e. the point
     * is roughly in the opposite direction from {@code routeEnd}) — {@code Math.acos} is
     * always non-negative, so without this correction a plaza behind the source would be
     * reported at some positive distance "ahead" instead of being recognisably off-route.
     */
    public static double alongTrackDistanceKm(Coordinates point, Coordinates routeStart, Coordinates routeEnd) {
        double d13 = haversineKm(routeStart, point) / EARTH_RADIUS_KM;
        double crossTrack = crossTrackDistanceKm(point, routeStart, routeEnd) / EARTH_RADIUS_KM;

        double cosRatio = Math.cos(d13) / Math.cos(crossTrack);
        cosRatio = Math.max(-1.0, Math.min(1.0, cosRatio));
        double magnitude = Math.acos(cosRatio) * EARTH_RADIUS_KM;

        double theta13 = bearingRad(routeStart, point);
        double theta12 = bearingRad(routeStart, routeEnd);
        double bearingDiff = Math.atan2(Math.sin(theta13 - theta12), Math.cos(theta13 - theta12));
        boolean isBehindStart = Math.abs(bearingDiff) > Math.PI / 2;

        return isBehindStart ? -magnitude : magnitude;
    }
}
