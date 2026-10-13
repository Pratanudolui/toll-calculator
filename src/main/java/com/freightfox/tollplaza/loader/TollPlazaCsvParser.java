package com.freightfox.tollplaza.loader;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses the toll plaza CSV (columns: longitude, latitude, toll_name, geo_state),
 * skipping rows with a blank or "nan" name and rows with unparsable coordinates.
 */
@Component
@Slf4j
public class TollPlazaCsvParser {

    public List<TollPlazaRecord> parse(InputStream inputStream) {
        List<TollPlazaRecord> records = new ArrayList<>();

        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setTrim(true)
                     .build()
                     .parse(reader)) {

            for (CSVRecord row : parser) {
                parseRow(row).ifPresent(records::add);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to parse toll plaza CSV", e);
        }

        return records;
    }

    private java.util.Optional<TollPlazaRecord> parseRow(CSVRecord row) {
        String name = row.isMapped("toll_name") ? row.get("toll_name") : null;
        if (name == null || name.isBlank() || "nan".equalsIgnoreCase(name.trim())) {
            return java.util.Optional.empty();
        }

        try {
            double latitude = Double.parseDouble(row.get("latitude"));
            double longitude = Double.parseDouble(row.get("longitude"));
            String geoState = row.isMapped("geo_state") ? row.get("geo_state") : null;
            return java.util.Optional.of(new TollPlazaRecord(name.trim(), latitude, longitude, geoState));
        } catch (IllegalArgumentException e) {
            log.warn("Skipping malformed toll plaza CSV row: {}", row);
            return java.util.Optional.empty();
        }
    }
}
