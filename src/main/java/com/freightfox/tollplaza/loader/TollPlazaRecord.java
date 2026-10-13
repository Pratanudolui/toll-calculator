package com.freightfox.tollplaza.loader;

/**
 * One parsed, validated row from the toll plaza CSV, ready for upsert.
 */
public record TollPlazaRecord(String name, double latitude, double longitude, String geoState) {
}
