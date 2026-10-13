package com.freightfox.tollplaza.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GeoUtilsTest {

    private static final double DELTA_KM = 1.0;

    @Test
    void haversine_oneDegreeLongitudeAtEquator_isAbout111Km() {
        Coordinates a = new Coordinates(0.0, 0.0);
        Coordinates b = new Coordinates(0.0, 1.0);

        double distance = GeoUtils.haversineKm(a, b);

        assertThat(distance).isCloseTo(111.19, org.assertj.core.data.Offset.offset(DELTA_KM));
    }

    @Test
    void haversine_samePoint_isZero() {
        Coordinates a = new Coordinates(12.9716, 77.5946);

        assertThat(GeoUtils.haversineKm(a, a)).isCloseTo(0.0, org.assertj.core.data.Offset.offset(0.001));
    }

    @Test
    void crossTrackDistance_pointExactlyOnRoute_isZero() {
        Coordinates start = new Coordinates(0.0, 0.0);
        Coordinates end = new Coordinates(0.0, 4.0);
        Coordinates onRoute = new Coordinates(0.0, 2.0);

        double crossTrack = GeoUtils.crossTrackDistanceKm(onRoute, start, end);

        assertThat(crossTrack).isCloseTo(0.0, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    void crossTrackDistance_pointOffRoute_isApproximatelyExpected() {
        Coordinates start = new Coordinates(0.0, 0.0);
        Coordinates end = new Coordinates(0.0, 4.0);
        // 1 degree north of the equatorial route, roughly midway along it.
        Coordinates offRoute = new Coordinates(1.0, 2.0);

        double crossTrack = GeoUtils.crossTrackDistanceKm(offRoute, start, end);

        assertThat(Math.abs(crossTrack)).isCloseTo(111.19, org.assertj.core.data.Offset.offset(2.0));
    }

    @Test
    void alongTrackDistance_pointOnRoute_matchesDistanceFromStart() {
        Coordinates start = new Coordinates(0.0, 0.0);
        Coordinates end = new Coordinates(0.0, 4.0);
        Coordinates onRoute = new Coordinates(0.0, 2.0);

        double alongTrack = GeoUtils.alongTrackDistanceKm(onRoute, start, end);

        assertThat(alongTrack).isCloseTo(GeoUtils.haversineKm(start, onRoute), org.assertj.core.data.Offset.offset(0.5));
    }

    @Test
    void alongTrackDistance_pointAtStart_isZero() {
        Coordinates start = new Coordinates(0.0, 0.0);
        Coordinates end = new Coordinates(0.0, 4.0);

        assertThat(GeoUtils.alongTrackDistanceKm(start, start, end)).isCloseTo(0.0, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    void alongTrackDistance_pointBehindStart_isNegative() {
        // Route runs east (toward increasing longitude); a point to the west of
        // start is in the opposite direction, i.e. "behind" the route.
        Coordinates start = new Coordinates(0.0, 0.0);
        Coordinates end = new Coordinates(0.0, 4.0);
        Coordinates behindStart = new Coordinates(0.0, -2.0);

        double alongTrack = GeoUtils.alongTrackDistanceKm(behindStart, start, end);

        assertThat(alongTrack).isNegative();
        assertThat(alongTrack).isCloseTo(-GeoUtils.haversineKm(start, behindStart),
                org.assertj.core.data.Offset.offset(0.5));
    }
}
