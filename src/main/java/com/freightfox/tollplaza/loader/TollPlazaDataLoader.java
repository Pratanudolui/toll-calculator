package com.freightfox.tollplaza.loader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TollPlazaDataLoader implements CommandLineRunner {

    private final TollPlazaCsvParser csvParser;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        // Load directly from the classpath root where Maven packages resources
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("data/toll_plaza_india.csv");

        if (inputStream == null) {
            inputStream = getClass().getResourceAsStream("/data/toll_plaza_india.csv");
        }

        if (inputStream == null) {
            log.error("CRITICAL: data/toll_plaza_india.csv not found in classpath!");
            return;
        }

        List<TollPlazaRecord> records = csvParser.parse(inputStream);

        if (records.isEmpty()) {
            log.warn("No toll plaza records found in CSV!");
            return;
        }

        List<Object[]> batchArgs = records.stream()
                .map(r -> new Object[]{r.name(), r.latitude(), r.longitude(), r.geoState()})
                .toList();

        int[] results = jdbcTemplate.batchUpdate(
                "MERGE INTO toll_plaza (name, latitude, longitude, geo_state) "
                        + "KEY (name, latitude, longitude) VALUES (?, ?, ?, ?)",
                batchArgs);

        log.info("Successfully upserted {} toll plaza records.", results.length);
    }
}