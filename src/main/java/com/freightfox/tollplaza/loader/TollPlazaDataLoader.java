package com.freightfox.tollplaza.loader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Loads the bundled toll plaza CSV into the database on startup.
 *
 * <p>Uses H2's {@code MERGE ... KEY(...)} to upsert on (name, latitude, longitude),
 * so re-running the app (e.g. against a persistent file-based H2 database) never
 * creates duplicate rows, and the whole file loads as a single batch statement
 * rather than one round trip per row.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TollPlazaDataLoader implements CommandLineRunner {

    private static final String CSV_CLASSPATH_LOCATION = "data/toll_plaza_india.csv";

    private final TollPlazaCsvParser csvParser;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        List<TollPlazaRecord> records;
        try (var inputStream = new ClassPathResource(CSV_CLASSPATH_LOCATION).getInputStream()) {
            records = csvParser.parse(inputStream);
        }

        if (records.isEmpty()) {
            log.warn("No toll plaza records found in {}", CSV_CLASSPATH_LOCATION);
            return;
        }

        List<Object[]> batchArgs = records.stream()
                .map(r -> new Object[]{r.name(), r.latitude(), r.longitude(), r.geoState()})
                .toList();

        int[] results = jdbcTemplate.batchUpdate(
                "MERGE INTO toll_plaza (name, latitude, longitude, geo_state) "
                        + "KEY (name, latitude, longitude) VALUES (?, ?, ?, ?)",
                batchArgs);

        log.info("Upserted {} toll plaza records from {}", results.length, CSV_CLASSPATH_LOCATION);
    }
}
