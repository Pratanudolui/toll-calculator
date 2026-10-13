package com.freightfox.tollplaza.loader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
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
        List<TollPlazaRecord> records;

        // Use Spring's ClassPathResource which is robust inside executable JARs/Docker
        ClassPathResource resource = new ClassPathResource("data/toll_plaza_india.csv");
        if (!resource.exists()) {
            resource = new ClassPathResource("/data/toll_plaza_india.csv");
        }

        try (InputStream inputStream = resource.getInputStream()) {
            records = csvParser.parse(inputStream);
        } catch (Exception e) {
            log.error("CRITICAL: Failed to open toll plaza CSV from classpath: {}", e.getMessage());
            return;
        }

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