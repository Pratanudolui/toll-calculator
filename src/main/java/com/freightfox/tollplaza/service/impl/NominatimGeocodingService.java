package com.freightfox.tollplaza.service.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.freightfox.tollplaza.config.CacheConfig;
import com.freightfox.tollplaza.model.PincodeLocation;
import com.freightfox.tollplaza.repository.PincodeLocationRepository;
import com.freightfox.tollplaza.service.GeocodingService;
import com.freightfox.tollplaza.util.Coordinates;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

/**
 * Resolves Indian pincodes to coordinates using the free OpenStreetMap Nominatim
 * search API (no key required). Results are persisted to {@link PincodeLocationRepository}
 * so that the external call only happens once per pincode; {@link Cacheable} adds an
 * in-memory layer on top of that for the lifetime of the JVM.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NominatimGeocodingService implements GeocodingService {

    private static final String NOMINATIM_URL =
            "https://nominatim.openstreetmap.org/search?postalcode={pincode}&country=India&format=json&limit=1";

    private final RestTemplate restTemplate;
    private final PincodeLocationRepository pincodeLocationRepository;

    @Override
    @Cacheable(value = CacheConfig.GEOCODE_CACHE, key = "#pincode")
    public Optional<Coordinates> geocode(String pincode) {
        Optional<PincodeLocation> cached = pincodeLocationRepository.findById(pincode);
        if (cached.isPresent()) {
            PincodeLocation location = cached.get();
            return Optional.of(new Coordinates(location.getLatitude(), location.getLongitude()));
        }

        try {
            NominatimResult[] results = restTemplate.getForObject(NOMINATIM_URL, NominatimResult[].class, pincode);
            if (results == null || results.length == 0) {
                return Optional.empty();
            }

            double lat = Double.parseDouble(results[0].lat());
            double lon = Double.parseDouble(results[0].lon());

            pincodeLocationRepository.save(PincodeLocation.builder()
                    .pincode(pincode)
                    .latitude(lat)
                    .longitude(lon)
                    .build());

            return Optional.of(new Coordinates(lat, lon));
        } catch (RestClientException | NumberFormatException ex) {
            log.warn("Geocoding lookup failed for pincode {}: {}", pincode, ex.getMessage());
            return Optional.empty();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record NominatimResult(String lat, String lon) {
    }
}
