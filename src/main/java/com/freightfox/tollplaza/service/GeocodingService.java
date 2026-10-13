package com.freightfox.tollplaza.service;

import com.freightfox.tollplaza.util.Coordinates;

import java.util.Optional;

public interface GeocodingService {

    /**
     * Resolves a 6-digit Indian pincode to coordinates.
     *
     * @return the coordinates, or empty if the pincode could not be resolved
     *         (treated by callers as an invalid pincode).
     */
    Optional<Coordinates> geocode(String pincode);
}
