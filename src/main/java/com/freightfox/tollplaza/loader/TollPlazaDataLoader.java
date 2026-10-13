package com.freightfox.tollplaza.loader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
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
        List<TollPlazaRecord> records = null;

        // Attempt 1: Load via Spring ClassPathResource
        try {
            ClassPathResource resource = new ClassPathResource("data/toll_plaza_india.csv");
            if (resource.exists()) {
                try (InputStream inputStream = resource.getInputStream()) {
                    records = csvParser.parse(inputStream);
                }
            }
        } catch (Exception e) {
            log.warn("ClassPathResource lookup failed: {}", e.getMessage());
        }

        // Attempt 2: Direct file system fallbacks for cloud containers / Docker
        if (records == null || records.isEmpty()) {
            File[] possibleFiles = {
                    new File("src/main/resources/data/toll_plaza_india.csv"),
                    new File("data/toll_plaza_india.csv"),
                    new File("toll_plaza_india.csv")
            };

            for (File file : possibleFiles) {
                if (file.exists()) {
                    log.info("Loading CSV from file path: {}", file.getAbsolutePath());
                    try (InputStream inputStream = new FileInputStream(file)) {
                        records = csvParser.parse(inputStream);
                        if (!records.isEmpty()) break;
                    } catch (Exception ignored) {}
                }
            }
        }

        if (records == null || records.isEmpty()) {
            log.error("CRITICAL: No toll plaza records found in any location!");
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